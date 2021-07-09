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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.vault.VaultException;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests without spring-retry on the classpath to be sure there is no hard dependency
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "spring-retry-*.jar" })
public class VaultConfigLoaderRetryUnavailableTests {

	private final Log log = LogFactory.getLog(getClass());

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void shouldNotBeConfiguredToRetry() throws URISyntaxException, IOException {
		SpringApplication app = new SpringApplication(TestApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

		Map<String, Object> properties = TestPropertySourceUtils.convertInlinedPropertiesToMap(
				"spring.profiles.active=test", "spring.cloud.vault.fail-fast=true",
				"spring.cloud.vault.retry.max-attempts=2", "spring.cloud.vault.lifecycle.enabled=false",
				"spring.cloud.vault.uri=http://serverhostdoesnotexist:1234", "spring.config.import=vault://");
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("testPropertiesSource", properties));
		DeferredLogs logs = new DeferredLogs();
		ConfigDataEnvironmentPostProcessor configDataEnvironmentPostProcessor = new ConfigDataEnvironmentPostProcessor(
				new DeferredLogs(), bootstrapContext);
		try {
			configDataEnvironmentPostProcessor.postProcessEnvironment(environment, app);
		}
		catch (VaultException ve) {
			// expected since fail-fast is true
		}
		((DeferredLog) logs.getLog(ConfigDataEnvironmentPostProcessor.class)).replayTo(log);

		AbstractVaultConfiguration.ClientFactoryWrapper clientFactoryWrapper = bootstrapContext
				.get(AbstractVaultConfiguration.ClientFactoryWrapper.class);
		ClientHttpRequestFactory requestFactory = clientFactoryWrapper.getClientHttpRequestFactory();
		ClientHttpRequest request = requestFactory.createRequest(new URI("https://spring.io/"), HttpMethod.GET);
		assertThat(request instanceof RetryableClientHttpRequest).isFalse();
	}

	@EnableAutoConfiguration(exclude = RefreshAutoConfiguration.class)
	public static class TestApplication {

	}

}
