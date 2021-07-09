/*
 * Copyright 2016-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.vault.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.LifecycleAwareSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.RestTemplateCustomizer;
import org.springframework.vault.client.RestTemplateFactory;
import org.springframework.vault.client.RestTemplateRequestCustomizer;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.web.client.RestTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Vault support.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ VaultProperties.class, RetryProperties.class })
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class VaultAutoConfiguration {

	private final Log log = LogFactory.getLog(getClass());

	private static final String RETRY_TEMPLATE = "org.springframework.retry.support.RetryTemplate";

	private final ConfigurableApplicationContext applicationContext;

	private final VaultProperties vaultProperties;

	private final RetryProperties retryProperties;

	private final VaultConfiguration configuration;

	private final VaultEndpointProvider endpointProvider;

	private final List<RestTemplateCustomizer> customizers;

	private final List<RestTemplateRequestCustomizer<?>> requestCustomizers;

	public VaultAutoConfiguration(ConfigurableApplicationContext applicationContext, VaultProperties vaultProperties,
			RetryProperties retryProperties, ObjectProvider<VaultEndpointProvider> endpointProvider,
			ObjectProvider<List<RestTemplateCustomizer>> customizers,
			ObjectProvider<List<RestTemplateRequestCustomizer<?>>> requestCustomizers) {

		this.applicationContext = applicationContext;
		this.vaultProperties = vaultProperties;
		this.retryProperties = retryProperties;
		this.configuration = new VaultConfiguration(vaultProperties);

		VaultEndpointProvider provider = endpointProvider.getIfAvailable();

		if (provider == null) {
			provider = SimpleVaultEndpointProvider.of(this.configuration.createVaultEndpoint());
		}

		this.endpointProvider = provider;
		this.customizers = new ArrayList<>(customizers.getIfAvailable(Collections::emptyList));
		AnnotationAwareOrderComparator.sort(this.customizers);

		this.requestCustomizers = new ArrayList<>(requestCustomizers.getIfAvailable(Collections::emptyList));
		AnnotationAwareOrderComparator.sort(this.requestCustomizers);
	}

	/**
	 * Create a {@link RestTemplateBuilder} initialized with {@link VaultEndpointProvider}
	 * and {@link ClientHttpRequestFactory}. May be overridden by subclasses.
	 * @param requestFactory the {@link ClientHttpRequestFactory}.
	 * @return the {@link RestTemplateBuilder}.
	 * @since 2.3
	 * @see #clientHttpRequestFactoryWrapper()
	 */
	protected RestTemplateBuilder restTemplateBuilder(ClientHttpRequestFactory requestFactory) {

		return this.configuration.createRestTemplateBuilder(requestFactory, this.endpointProvider, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Creates a {@link ClientFactoryWrapper} containing a
	 * {@link ClientHttpRequestFactory}. {@link ClientHttpRequestFactory} is not exposed
	 * as root bean because {@link ClientHttpRequestFactory} is configured with
	 * {@link ClientOptions} and {@link SslConfiguration} which are not necessarily
	 * applicable for the whole application.
	 * @return the {@link ClientFactoryWrapper} to wrap a {@link ClientHttpRequestFactory}
	 * instance.
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClientFactoryWrapper clientHttpRequestFactoryWrapper() {
		ClientHttpRequestFactory clientHttpRequestFactory = this.configuration.createClientHttpRequestFactory();
		if (ClassUtils.isPresent(RETRY_TEMPLATE, getClass().getClassLoader()) && this.vaultProperties.isFailFast()) {
			Map<String, RetryTemplate> beans = applicationContext.getBeansOfType(RetryTemplate.class);
			if (!beans.isEmpty()) {
				Map.Entry<String, RetryTemplate> existingBean = beans.entrySet().stream().findFirst().get();
				log.info("Using existing RestTemplate '" + existingBean.getKey() + "' for vault retries");
				clientHttpRequestFactory = VaultRetryUtil
						.createRetryableClientHttpRequestFactory(existingBean.getValue(), clientHttpRequestFactory);
			}
			else {
				clientHttpRequestFactory = VaultRetryUtil.createRetryableClientHttpRequestFactory(retryProperties,
						clientHttpRequestFactory);
			}
		}
		return new ClientFactoryWrapper(clientHttpRequestFactory);
	}

	/**
	 * Create a {@link RestTemplateFactory} bean that is used to produce
	 * {@link RestTemplate}.
	 * @param clientFactoryWrapper the {@link ClientFactoryWrapper}.
	 * @return the {@link RestTemplateFactory}.
	 * @see #clientHttpRequestFactoryWrapper()
	 * @since 3.0
	 */
	@Bean
	@ConditionalOnMissingBean
	public RestTemplateFactory vaultRestTemplateFactory(ClientFactoryWrapper clientFactoryWrapper) {
		return new DefaultRestTemplateFactory(clientFactoryWrapper.getClientHttpRequestFactory(),
				this::restTemplateBuilder);
	}

	/**
	 * Creates a {@link VaultTemplate}.
	 * @param clientFactoryWrapper the {@link ClientFactoryWrapper}.
	 * @return the {@link VaultTemplate} bean.
	 * @see VaultAutoConfiguration#clientHttpRequestFactoryWrapper()
	 */
	@Bean
	@ConditionalOnMissingBean(VaultOperations.class)
	public VaultTemplate vaultTemplate(ClientFactoryWrapper clientFactoryWrapper) {

		VaultProperties.AuthenticationMethod authentication = this.vaultProperties.getAuthentication();
		RestTemplateBuilder restTemplateBuilder = restTemplateBuilder(
				clientFactoryWrapper.getClientHttpRequestFactory());

		if (authentication == VaultProperties.AuthenticationMethod.NONE) {
			return new VaultTemplate(restTemplateBuilder);
		}

		return new VaultTemplate(restTemplateBuilder, this.applicationContext.getBean(SessionManager.class));
	}

	/**
	 * Creates a new {@link TaskSchedulerWrapper} that encapsulates a bean implementing
	 * {@link TaskScheduler} and {@link AsyncTaskExecutor}.
	 * @return the {@link TaskSchedulerWrapper} bean.
	 * @see ThreadPoolTaskScheduler
	 */
	@Bean
	@Lazy
	@ConditionalOnMissingBean(TaskSchedulerWrapper.class)
	public TaskSchedulerWrapper vaultTaskScheduler() {

		ThreadPoolTaskScheduler threadPoolTaskScheduler = VaultConfiguration.createScheduler();

		// This is to destroy bootstrap resources
		// otherwise, the bootstrap context is not shut down cleanly
		this.applicationContext.registerShutdownHook();

		return new TaskSchedulerWrapper(threadPoolTaskScheduler);
	}

	/**
	 * @return the {@link SessionManager} for Vault session management.
	 * @param clientAuthentication the {@link ClientAuthentication}.
	 * @param asyncTaskExecutorFactory the {@link ObjectFactory} for
	 * {@link TaskSchedulerWrapper}.
	 * @param restTemplateFactory the {@link RestTemplateFactory}.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAuthentication
	public SessionManager vaultSessionManager(ClientAuthentication clientAuthentication,
			ObjectFactory<TaskSchedulerWrapper> asyncTaskExecutorFactory, RestTemplateFactory restTemplateFactory) {

		return this.configuration.createSessionManager(clientAuthentication,
				() -> asyncTaskExecutorFactory.getObject().getTaskScheduler(), restTemplateFactory);
	}

	/**
	 * @return the {@link ClientAuthentication} to obtain a
	 * {@link org.springframework.vault.support.VaultToken}.
	 * @param clientFactoryWrapper the {@link ClientFactoryWrapper}.
	 * @param restTemplateFactory the {@link RestTemplateFactory}.
	 * @see SessionManager
	 * @see LifecycleAwareSessionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnAuthentication
	public ClientAuthentication clientAuthentication(ClientFactoryWrapper clientFactoryWrapper,
			RestTemplateFactory restTemplateFactory) {

		RestTemplate externalRestOperations = new RestTemplate(clientFactoryWrapper.getClientHttpRequestFactory());

		this.customizers.forEach(customizer -> customizer.customize(externalRestOperations));

		RestTemplate restTemplate = restTemplateFactory.create();
		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(this.vaultProperties, restTemplate,
				externalRestOperations);

		return factory.createClientAuthentication();
	}

	/**
	 * Wrapper to keep {@link TaskScheduler} local to Spring Cloud Vault.
	 */
	public static class TaskSchedulerWrapper implements InitializingBean, DisposableBean {

		private final ThreadPoolTaskScheduler taskScheduler;

		private final boolean acceptAfterPropertiesSet;

		public TaskSchedulerWrapper(ThreadPoolTaskScheduler taskScheduler) {
			this(taskScheduler, true);
		}

		TaskSchedulerWrapper(ThreadPoolTaskScheduler taskScheduler, boolean acceptAfterPropertiesSet) {
			this.taskScheduler = taskScheduler;
			this.acceptAfterPropertiesSet = acceptAfterPropertiesSet;
		}

		ThreadPoolTaskScheduler getTaskScheduler() {
			return this.taskScheduler;
		}

		@Override
		public void destroy() {
			this.taskScheduler.destroy();
		}

		@Override
		public void afterPropertiesSet() {

			if (this.acceptAfterPropertiesSet) {
				this.taskScheduler.afterPropertiesSet();
			}
		}

	}

}
