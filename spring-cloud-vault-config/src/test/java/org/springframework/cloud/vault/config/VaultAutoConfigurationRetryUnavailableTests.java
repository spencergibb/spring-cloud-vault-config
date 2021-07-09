/*
 * Copyright 2016-2020 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests without spring-retry on the classpath to be sure there is no hard dependency
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "spring-retry-*.jar" })
public class VaultAutoConfigurationRetryUnavailableTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(VaultAutoConfiguration.class));

	@Test
	public void shouldNotBeConfigured() {

		this.contextRunner.withPropertyValues("spring.cloud.vault.kv.enabled=false",
				"spring.cloud.vault.fail-fast=true", "spring.cloud.vault.token=foo").run(context -> {
					assertThat(context.containsBean("vaultRetryOperations")).isFalse();
					AbstractVaultConfiguration.ClientFactoryWrapper clientFactoryWrapper = context
							.getBean(AbstractVaultConfiguration.ClientFactoryWrapper.class);
					ClientHttpRequestFactory requestFactory = clientFactoryWrapper.getClientHttpRequestFactory();
					ClientHttpRequest request = requestFactory.createRequest(new URI("https://spring.io/"),
							HttpMethod.GET);
					assertThat(request instanceof RetryableClientHttpRequest).isFalse();
				});
	}

}
