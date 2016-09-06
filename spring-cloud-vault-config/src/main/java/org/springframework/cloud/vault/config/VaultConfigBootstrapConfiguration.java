/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.vault.config;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.AppIdUserIdMechanism;
import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.SecureBackendAccessor;
import org.springframework.cloud.vault.SecureBackendAccessorFactory;
import org.springframework.cloud.vault.SecureBackendFactories;
import org.springframework.cloud.vault.VaultBootstrapConfiguration;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultOperations;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultSecretBackend;
import org.springframework.cloud.vault.VaultTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author Mark Paluch
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@ConditionalOnBean(VaultBootstrapConfiguration.class)
public class VaultConfigBootstrapConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private Collection<VaultSecretBackend> vaultSecretBackends;
	private Collection<SecureBackendAccessorFactory<? super VaultSecretBackend>> factories;

	@Bean
	public VaultGenericBackendProperties vaultGenericBackendProperties() {
		return new VaultGenericBackendProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public VaultOperations vaultOperations(VaultProperties properties, VaultClient client) {
		ClientAuthentication clientAuthentication = clientAuthentication(
				applicationContext, properties, client);
		return new VaultTemplate(properties, client, clientAuthentication);
	}

	@Bean
	@ConditionalOnMissingBean
	public VaultConfigOperations vaultConfigOperations(VaultOperations operations, VaultProperties properties) {
		return new VaultConfigTemplate(operations, properties);
	}

	@Bean
	public VaultPropertySourceLocator vaultPropertySourceLocator(
			VaultConfigOperations operations, VaultProperties vaultProperties,
			VaultGenericBackendProperties vaultGenericBackendProperties) {

		Collection<SecureBackendAccessor> backendAccessors = SecureBackendFactories
				.createBackendAcessors(vaultSecretBackends, factories);

		return new VaultPropertySourceLocator(operations, vaultProperties,
				vaultGenericBackendProperties, backendAccessors);
	}

	private ClientAuthentication clientAuthentication(
			ApplicationContext applicationContext, VaultProperties vaultProperties,
			VaultClient client) {

		ClientAuthentication clientAuthentication;

		if (vaultProperties.getAuthentication() == VaultProperties.AuthenticationMethod.TOKEN) {
			clientAuthentication = ClientAuthentication.token(vaultProperties);
		}
		else if (vaultProperties.getAuthentication() == VaultProperties.AuthenticationMethod.APPID) {

			Map<String, AppIdUserIdMechanism> appIdUserIdMechanisms = applicationContext
					.getBeansOfType(AppIdUserIdMechanism.class);
			if (!appIdUserIdMechanisms.isEmpty()) {
				clientAuthentication = ClientAuthentication.appId(vaultProperties,
						client, appIdUserIdMechanisms.values().iterator().next());
			}
			else {
				clientAuthentication = ClientAuthentication
						.appId(vaultProperties, client);
			}
		}
		else {
			clientAuthentication = ClientAuthentication.create(vaultProperties, client);
		}

		return clientAuthentication;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	private void postConstruct() {

		this.vaultSecretBackends = applicationContext.getBeansOfType(
				VaultSecretBackend.class).values();
		this.factories = (Collection) applicationContext.getBeansOfType(
				SecureBackendAccessorFactory.class).values();
	}
}
