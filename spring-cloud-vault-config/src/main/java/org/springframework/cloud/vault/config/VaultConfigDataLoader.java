/*
 * Copyright 2019-2021 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.BootstrapContext;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.Bootstrapper;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.cloud.vault.config.VaultAutoConfiguration.TaskSchedulerWrapper;
import org.springframework.cloud.vault.config.VaultReactiveAutoConfiguration.ClientHttpConnectorWrapper;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.AuthenticationStepsFactory;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.ReactiveSessionManager;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.VaultTokenSupplier;
import org.springframework.vault.client.ClientHttpRequestFactoryFactory;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.RestTemplateFactory;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.client.WebClientBuilder;
import org.springframework.vault.client.WebClientFactory;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.env.LeaseAwareVaultPropertySource;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.LeaseErrorListener;
import org.springframework.web.client.RestTemplate;

import static org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper;

/**
 * {@link ConfigDataLoader} for Vault for {@link VaultConfigLocation}. This class
 * materializes {@link PropertySource property sources} by using Vault and
 * {@link VaultConfigLocation}. This class also ensures that all necessary infrastructure
 * beans are registered in the {@link BootstrapRegistry}. Registrations made by this
 * config data loader are typically propagated into the {@link BeanFactory} as this
 * configuration mirrors to some extent {@link VaultAutoConfiguration} and
 * {@link VaultReactiveAutoConfiguration}.
 * <p>
 * Infrastructure beans can be customized by registering instances through
 * {@link Bootstrapper}.
 *
 * @author Mark Paluch
 * @since 3.0
 * @see VaultConfigLocation
 * @see VaultAutoConfiguration
 * @see VaultReactiveAutoConfiguration
 */
public class VaultConfigDataLoader implements ConfigDataLoader<VaultConfigLocation> {

	private final static ConfigData SKIP_LOCATION = null;

	private final static boolean FLUX_AVAILABLE = ClassUtils.isPresent("reactor.core.publisher.Flux",
			VaultConfigDataLoader.class.getClassLoader());

	private final static boolean WEBCLIENT_AVAILABLE = ClassUtils.isPresent(
			"org.springframework.web.reactive.function.client.WebClient", VaultConfigDataLoader.class.getClassLoader());

	private final static boolean REGISTER_REACTIVE_INFRASTRUCTURE = FLUX_AVAILABLE && WEBCLIENT_AVAILABLE;

	private final static boolean RETRY_AVAILABLE = ClassUtils
			.isPresent("org.springframework.retry.support.RetryTemplate", VaultConfigDataLoader.class.getClassLoader());

	private final DeferredLogFactory logFactory;

	public VaultConfigDataLoader(DeferredLogFactory logFactory) {
		this.logFactory = logFactory;
		reconfigureLoggers(logFactory);
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, VaultConfigLocation location)
			throws ConfigDataLocationNotFoundException {

		ConfigurableBootstrapContext bootstrap = context.getBootstrapContext();
		VaultProperties vaultProperties = bootstrap.get(VaultProperties.class);
		RetryProperties retryProperties = bootstrap.get(RetryProperties.class);

		if (!vaultProperties.isEnabled()) {
			return SKIP_LOCATION;
		}

		if (vaultProperties.getSession().getLifecycle().isEnabled()
				|| vaultProperties.getConfig().getLifecycle().isEnabled()) {
			registerVaultTaskScheduler(bootstrap);
		}

		registerImperativeInfrastructure(bootstrap, vaultProperties, retryProperties);

		if (REGISTER_REACTIVE_INFRASTRUCTURE && vaultProperties.getReactive().isEnabled()) {
			registerReactiveInfrastructure(bootstrap, vaultProperties);
		}

		registerVaultConfigTemplate(bootstrap, vaultProperties);

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {
			registerSecretLeaseContainer(bootstrap, new VaultConfiguration(vaultProperties));
		}

		return loadConfigData(location, bootstrap, vaultProperties);
	}

	private ConfigData loadConfigData(VaultConfigLocation location, ConfigurableBootstrapContext bootstrap,
			VaultProperties vaultProperties) {

		if (location.getSecretBackendMetadata() instanceof ApplicationEventPublisherAware) {

			bootstrap.addCloseListener(event -> {
				((ApplicationEventPublisherAware) location.getSecretBackendMetadata())
						.setApplicationEventPublisher(event.getApplicationContext());
			});
		}

		if (vaultProperties.getConfig().getLifecycle().isEnabled()) {

			RequestedSecret secret = getRequestedSecret(location.getSecretBackendMetadata());

			if (vaultProperties.isFailFast()) {
				return new ConfigData(Collections.singleton(createLeasingPropertySourceFailFast(
						bootstrap.get(SecretLeaseContainer.class), secret, location.getSecretBackendMetadata())));
			}

			return createConfigData(() -> createLeasingPropertySource(bootstrap.get(SecretLeaseContainer.class), secret,
					location.getSecretBackendMetadata()));
		}

		return createConfigData(() -> {
			VaultConfigTemplate configTemplate = bootstrap.get(VaultConfigTemplate.class);

			return createVaultPropertySource(configTemplate, vaultProperties.isFailFast(),
					location.getSecretBackendMetadata());
		});
	}

	private void registerImperativeInfrastructure(ConfigurableBootstrapContext bootstrap,
			VaultProperties vaultProperties, RetryProperties retryProperties) {

		ImperativeInfrastructure infra = new ImperativeInfrastructure(bootstrap, vaultProperties, retryProperties,
				this.logFactory);

		infra.registerClientHttpRequestFactoryWrapper();
		infra.registerRestTemplateBuilder();
		infra.registerVaultRestTemplateFactory();

		VaultProperties.AuthenticationMethod authentication = vaultProperties.getAuthentication();

		if (authentication == VaultProperties.AuthenticationMethod.NONE) {
			registerIfAbsent(bootstrap, "vaultTemplate", VaultTemplate.class,
					ctx -> new VaultTemplate(ctx.get(RestTemplateBuilder.class)));
		}
		else {

			infra.registerClientAuthentication();

			if (!(REGISTER_REACTIVE_INFRASTRUCTURE && vaultProperties.getReactive().isEnabled())) {
				infra.registerVaultSessionManager();
			}

			registerIfAbsent(bootstrap, "vaultTemplate", VaultTemplate.class,
					ctx -> new VaultTemplate(bootstrap.get(RestTemplateBuilder.class),
							bootstrap.get(SessionManager.class)));
		}
	}

	private void registerReactiveInfrastructure(ConfigurableBootstrapContext bootstrap,
			VaultProperties vaultProperties) {

		ReactiveInfrastructure reactiveInfrastructure = new ReactiveInfrastructure(bootstrap, vaultProperties,
				this.logFactory);
		reactiveInfrastructure.registerClientHttpConnectorWrapper();
		reactiveInfrastructure.registerWebClientBuilder();
		reactiveInfrastructure.registerWebClientFactory();

		VaultProperties.AuthenticationMethod authentication = vaultProperties.getAuthentication();

		if (authentication == VaultProperties.AuthenticationMethod.NONE) {
			registerIfAbsent(bootstrap, "reactiveVaultTemplate", ReactiveVaultTemplate.class,
					ctx -> new ReactiveVaultTemplate(ctx.get(WebClientBuilder.class)));
		}
		else {

			reactiveInfrastructure.registerTokenSupplier();
			reactiveInfrastructure.registerReactiveSessionManager();
			reactiveInfrastructure.registerSessionManager();

			registerIfAbsent(bootstrap, "reactiveVaultTemplate", ReactiveVaultTemplate.class,
					ctx -> new ReactiveVaultTemplate(bootstrap.get(WebClientBuilder.class),
							bootstrap.get(ReactiveSessionManager.class)));
		}
	}

	static ConfigData createConfigData(Supplier<PropertySource<?>> propertySourceSupplier) {
		return new ConfigData(Collections.singleton(propertySourceSupplier.get()));
	}

	private void registerVaultConfigTemplate(ConfigurableBootstrapContext bootstrap, VaultProperties vaultProperties) {
		bootstrap.registerIfAbsent(VaultConfigTemplate.class,
				ctx -> new VaultConfigTemplate(ctx.get(VaultTemplate.class), vaultProperties));
	}

	private void registerVaultTaskScheduler(ConfigurableBootstrapContext bootstrap) {
		registerIfAbsent(bootstrap, "vaultTaskScheduler", TaskSchedulerWrapper.class, () -> {

			ThreadPoolTaskScheduler scheduler = VaultConfiguration.createScheduler();

			scheduler.afterPropertiesSet();

			// avoid double-initialization
			return new TaskSchedulerWrapper(scheduler, false);
		}, ConfigurableApplicationContext::registerShutdownHook);
	}

	private void registerSecretLeaseContainer(ConfigurableBootstrapContext bootstrap,
			VaultConfiguration vaultConfiguration) {
		registerIfAbsent(bootstrap, "secretLeaseContainer", SecretLeaseContainer.class, ctx -> {

			SecretLeaseContainer container = vaultConfiguration.createSecretLeaseContainer(ctx.get(VaultTemplate.class),
					() -> ctx.get(TaskSchedulerWrapper.class).getTaskScheduler());

			try {
				container.afterPropertiesSet();
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
			container.start();

			return container;
		}, ConfigurableApplicationContext::registerShutdownHook);
	}

	private PropertySource<?> createVaultPropertySource(VaultConfigOperations configOperations, boolean failFast,
			SecretBackendMetadata accessor) {

		VaultPropertySource vaultPropertySource = new VaultPropertySource(configOperations, failFast, accessor);
		vaultPropertySource.init();
		return vaultPropertySource;
	}

	private PropertySource<?> createLeasingPropertySource(SecretLeaseContainer secretLeaseContainer,
			RequestedSecret secret, SecretBackendMetadata accessor) {

		if (accessor instanceof LeasingSecretBackendMetadata) {
			((LeasingSecretBackendMetadata) accessor).beforeRegistration(secret, secretLeaseContainer);
		}

		LeaseAwareVaultPropertySource propertySource = new LeaseAwareVaultPropertySource(accessor.getName(),
				secretLeaseContainer, secret, accessor.getPropertyTransformer());

		if (accessor instanceof LeasingSecretBackendMetadata) {
			((LeasingSecretBackendMetadata) accessor).afterRegistration(secret, secretLeaseContainer);
		}

		return propertySource;
	}

	private PropertySource<?> createLeasingPropertySourceFailFast(SecretLeaseContainer secretLeaseContainer,
			RequestedSecret secret, SecretBackendMetadata accessor) {

		final AtomicReference<Exception> errorRef = new AtomicReference<>();

		LeaseErrorListener errorListener = (leaseEvent, exception) -> {

			if (leaseEvent.getSource() == secret) {
				errorRef.compareAndSet(null, exception);
			}
		};

		secretLeaseContainer.addErrorListener(errorListener);
		try {
			return createLeasingPropertySource(secretLeaseContainer, secret, accessor);
		}
		finally {
			secretLeaseContainer.removeLeaseErrorListener(errorListener);

			Exception exception = errorRef.get();
			if (exception != null) {
				if (exception instanceof VaultException) {
					throw (VaultException) exception;
				}
				throw new VaultException(
						String.format("Cannot initialize PropertySource for secret at %s", secret.getPath()),
						exception);
			}
		}
	}

	private RequestedSecret getRequestedSecret(SecretBackendMetadata accessor) {

		if (accessor instanceof LeasingSecretBackendMetadata) {

			LeasingSecretBackendMetadata leasingBackend = (LeasingSecretBackendMetadata) accessor;
			return RequestedSecret.from(leasingBackend.getLeaseMode(), accessor.getPath());
		}

		if (accessor instanceof KeyValueSecretBackendMetadata) {
			return RequestedSecret.rotating(accessor.getPath());
		}

		return RequestedSecret.renewable(accessor.getPath());
	}

	static <T> void registerIfAbsent(ConfigurableBootstrapContext bootstrap, String beanName, Class<T> instanceType,
			Supplier<T> instanceSupplier) {
		registerIfAbsent(bootstrap, beanName, instanceType, ctx -> instanceSupplier.get(), ctx -> {
		});
	}

	static <T> void registerIfAbsent(ConfigurableBootstrapContext bootstrap, String beanName, Class<T> instanceType,
			Supplier<T> instanceSupplier, Consumer<ConfigurableApplicationContext> contextCustomizer) {
		registerIfAbsent(bootstrap, beanName, instanceType, ctx -> instanceSupplier.get(), contextCustomizer);
	}

	static <T> void registerIfAbsent(ConfigurableBootstrapContext bootstrap, String beanName, Class<T> instanceType,
			Function<BootstrapContext, T> instanceSupplier) {
		registerIfAbsent(bootstrap, beanName, instanceType, instanceSupplier, ctx -> {
		});
	}

	static <T> void registerIfAbsent(ConfigurableBootstrapContext bootstrap, String beanName, Class<T> instanceType,
			Function<BootstrapContext, T> instanceSupplier,
			Consumer<ConfigurableApplicationContext> contextCustomizer) {

		bootstrap.registerIfAbsent(instanceType, instanceSupplier::apply);

		bootstrap.addCloseListener(event -> {

			GenericApplicationContext gac = (GenericApplicationContext) event.getApplicationContext();

			contextCustomizer.accept(gac);
			T instance = event.getBootstrapContext().get(instanceType);

			gac.registerBean(beanName, instanceType, () -> instance);
		});
	}

	static void reconfigureLoggers(DeferredLogFactory logFactory) {

		List<Class<?>> loggers = Arrays.asList(ClientHttpRequestFactoryFactory.class, VaultPropertySource.class,
				LeaseAwareVaultPropertySource.class,
				forName("org.springframework.vault.core.lease.SecretLeaseContainer$LeaseRenewalScheduler"),
				forName("org.springframework.vault.core.lease.SecretLeaseEventPublisher$LoggingErrorListener"));

		loggers.forEach(it -> reconfigureLogger(it, logFactory));
	}

	static void reconfigureLogger(Class<?> type, DeferredLogFactory logFactory) {

		ReflectionUtils.doWithFields(type, field -> {

			field.setAccessible(true);
			field.set(null, logFactory.getLog(type));

		}, VaultConfigDataLoader::isUpdateableLogField);
	}

	static void reconfigureLogger(Object object, DeferredLogFactory logFactory) {

		ReflectionUtils.doWithFields(object.getClass(), field -> {

			field.setAccessible(true);
			field.set(object, logFactory.getLog(object.getClass()));

		}, VaultConfigDataLoader::isUpdateableLogField);
	}

	static boolean isUpdateableLogField(Field field) {
		return !Modifier.isFinal(field.getModifiers()) && field.getType().isAssignableFrom(Log.class);
	}

	@Nullable
	static Class<?> forName(String name) {
		try {
			return ClassUtils.forName(name, VaultConfigDataLocationResolver.class.getClassLoader());
		}
		catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * Support class to register imperative infrastructure bootstrap instances and beans.
	 *
	 * Mirrors {@link VaultAutoConfiguration}.
	 */
	static class ImperativeInfrastructure {

		private final ConfigurableBootstrapContext bootstrap;

		private final VaultProperties vaultProperties;

		private final RetryProperties retryProperties;

		private final VaultConfiguration configuration;

		private final VaultEndpointProvider endpointProvider;

		private final DeferredLogFactory logFactory;

		ImperativeInfrastructure(ConfigurableBootstrapContext bootstrap, VaultProperties vaultProperties,
				RetryProperties retryProperties, DeferredLogFactory logFactory) {
			this.bootstrap = bootstrap;
			this.vaultProperties = vaultProperties;
			this.retryProperties = retryProperties;
			this.configuration = new VaultConfiguration(vaultProperties);
			this.endpointProvider = SimpleVaultEndpointProvider.of(this.configuration.createVaultEndpoint());
			this.logFactory = logFactory;
		}

		void registerClientHttpRequestFactoryWrapper() {
			registerIfAbsent(this.bootstrap, "clientHttpRequestFactoryWrapper", ClientFactoryWrapper.class, () -> {

				ClientHttpRequestFactory factory = this.configuration.createClientHttpRequestFactory();
				if (RETRY_AVAILABLE && this.vaultProperties.isFailFast()) {
					RetryTemplate retryTemplate = bootstrap.getOrElse(RetryTemplate.class,
							VaultRetryUtil.createRetryTemplate(retryProperties));
					factory = VaultRetryUtil.createRetryableClientHttpRequestFactory(retryTemplate, factory);
				}

				// early initialization
				try {
					new ClientFactoryWrapper(factory).afterPropertiesSet();
				}
				catch (Exception e) {
					ReflectionUtils.rethrowRuntimeException(e);
				}

				return new NonInitializingClientFactoryWrapper(factory);
			});
		}

		void registerRestTemplateBuilder() {
			// not a bean
			this.bootstrap.registerIfAbsent(RestTemplateBuilder.class,
					ctx -> this.configuration.createRestTemplateBuilder(
							ctx.get(ClientFactoryWrapper.class).getClientHttpRequestFactory(), this.endpointProvider,
							Collections.emptyList(), Collections.emptyList()));
		}

		void registerVaultRestTemplateFactory() {
			registerIfAbsent(this.bootstrap, "vaultRestTemplateFactory", RestTemplateFactory.class,
					ctx -> new DefaultRestTemplateFactory(
							ctx.get(ClientFactoryWrapper.class).getClientHttpRequestFactory(),
							requestFactory -> this.configuration.createRestTemplateBuilder(requestFactory,
									this.endpointProvider, Collections.emptyList(), Collections.emptyList())));
		}

		void registerClientAuthentication() {
			registerIfAbsent(this.bootstrap, "clientAuthentication", ClientAuthentication.class, ctx -> {

				ClientHttpRequestFactory factory = this.bootstrap.get(ClientFactoryWrapper.class)
						.getClientHttpRequestFactory();

				RestTemplate externalRestTemplate = new RestTemplate(factory);

				ClientAuthenticationFactory authenticationFactory = new ClientAuthenticationFactory(
						this.vaultProperties, this.bootstrap.get(RestTemplateFactory.class).create(),
						externalRestTemplate);
				return authenticationFactory.createClientAuthentication();
			});
		}

		void registerVaultSessionManager() {
			registerIfAbsent(this.bootstrap, "vaultSessionManager", SessionManager.class, ctx -> {
				SessionManager sessionManager = this.configuration.createSessionManager(
						ctx.get(ClientAuthentication.class),
						() -> ctx.get(TaskSchedulerWrapper.class).getTaskScheduler(),
						ctx.get(RestTemplateFactory.class));
				reconfigureLogger(sessionManager, this.logFactory);
				return sessionManager;
			});
		}

	}

	/**
	 * Support class to register reactive infrastructure bootstrap instances and beans.
	 * Mirrors {@link VaultReactiveAutoConfiguration}.
	 */
	static class ReactiveInfrastructure {

		private final ConfigurableBootstrapContext bootstrap;

		private final VaultReactiveConfiguration configuration;

		private final VaultEndpointProvider endpointProvider;

		private final DeferredLogFactory logFactory;

		ReactiveInfrastructure(ConfigurableBootstrapContext bootstrap, VaultProperties vaultProperties,
				DeferredLogFactory logFactory) {
			this.bootstrap = bootstrap;
			this.configuration = new VaultReactiveConfiguration(vaultProperties);
			this.endpointProvider = SimpleVaultEndpointProvider
					.of(new VaultConfiguration(vaultProperties).createVaultEndpoint());
			this.logFactory = logFactory;
		}

		void registerClientHttpConnectorWrapper() {
			registerIfAbsent(this.bootstrap, "clientHttpConnectorWrapper", ClientHttpConnectorWrapper.class,
					() -> new ClientHttpConnectorWrapper(this.configuration.createClientHttpConnector()));
		}

		public void registerWebClientBuilder() {
			// not a bean
			this.bootstrap.registerIfAbsent(WebClientBuilder.class,
					ctx -> this.configuration.createWebClientBuilder(
							ctx.get(ClientHttpConnectorWrapper.class).getConnector(), this.endpointProvider,
							Collections.emptyList()));
		}

		void registerWebClientFactory() {
			registerIfAbsent(this.bootstrap, "vaultWebClientFactory", WebClientFactory.class,
					ctx -> new DefaultWebClientFactory(ctx.get(ClientHttpConnectorWrapper.class).getConnector(),
							connector -> this.configuration.createWebClientBuilder(connector, this.endpointProvider,
									Collections.emptyList())));
		}

		void registerTokenSupplier() {

			registerIfAbsent(this.bootstrap, "vaultTokenSupplier", VaultTokenSupplier.class,
					ctx -> this.configuration.createVaultTokenSupplier(ctx.get(WebClientFactory.class), () -> {
						if (this.bootstrap.isRegistered(AuthenticationStepsFactory.class)) {
							return this.bootstrap.get(AuthenticationStepsFactory.class);
						}

						return null;
					}, () -> {
						if (this.bootstrap.isRegistered(ClientAuthentication.class)) {
							return this.bootstrap.get(ClientAuthentication.class);
						}

						return null;
					}));
		}

		void registerReactiveSessionManager() {

			registerIfAbsent(this.bootstrap, "reactiveVaultSessionManager", ReactiveSessionManager.class,
					ctx -> this.configuration.createReactiveSessionManager(ctx.get(VaultTokenSupplier.class),
							() -> ctx.get(TaskSchedulerWrapper.class).getTaskScheduler(),
							ctx.get(WebClientFactory.class)));
		}

		void registerSessionManager() {
			registerIfAbsent(this.bootstrap, "vaultSessionManager", SessionManager.class, ctx -> {
				SessionManager sessionManager = this.configuration
						.createSessionManager(ctx.get(ReactiveSessionManager.class));
				reconfigureLogger(sessionManager, this.logFactory);
				return sessionManager;
			});
		}

	}

	/**
	 * Wrapper for {@link ClientHttpRequestFactory} that suppresses
	 * {@link #afterPropertiesSet()} to avoid double-initialization.
	 */
	private static class NonInitializingClientFactoryWrapper extends ClientFactoryWrapper {

		NonInitializingClientFactoryWrapper(ClientHttpRequestFactory clientHttpRequestFactory) {
			super(clientHttpRequestFactory);
		}

		@Override
		public void afterPropertiesSet() {
		}

	}

}
