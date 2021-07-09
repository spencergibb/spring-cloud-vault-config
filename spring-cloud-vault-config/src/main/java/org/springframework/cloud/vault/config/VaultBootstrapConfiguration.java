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

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.vault.client.RestTemplateCustomizer;
import org.springframework.vault.client.RestTemplateRequestCustomizer;
import org.springframework.vault.client.VaultEndpointProvider;

/**
 * {@link BootstrapConfiguration Boostrap configuration} for Spring Vault support.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @deprecated since 3.0, use {@link VaultReactiveAutoConfiguration} through
 * {@code @EnableAutoConfiguration}.
 */
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ VaultProperties.class, RetryProperties.class })
@Order(Ordered.LOWEST_PRECEDENCE - 5)
@Deprecated
public class VaultBootstrapConfiguration extends VaultAutoConfiguration {

	public VaultBootstrapConfiguration(ConfigurableApplicationContext applicationContext,
			VaultProperties vaultProperties, RetryProperties retryProperties,
			ObjectProvider<VaultEndpointProvider> endpointProvider,
			ObjectProvider<List<RestTemplateCustomizer>> customizers,
			ObjectProvider<List<RestTemplateRequestCustomizer<?>>> requestCustomizers) {
		super(applicationContext, vaultProperties, retryProperties, endpointProvider, customizers, requestCustomizers);
	}

}
