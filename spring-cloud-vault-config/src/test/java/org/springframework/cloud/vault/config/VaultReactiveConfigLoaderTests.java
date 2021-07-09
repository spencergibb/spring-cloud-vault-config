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
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.vault.core.ReactiveVaultTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

public class VaultReactiveConfigLoaderTests {

	private final Log log = LogFactory.getLog(VaultReactiveConfigLoaderTests.class);

	/**
	 * Config loader equivalent of
	 * {@link VaultReactiveBootstrapConfigurationTests#shouldNotConfigureReactiveSupport()}
	 */
	@Test
	public void shouldNotConfigureReactiveSupport() {
		SpringApplication app = new SpringApplication(VaultConfigLoaderRetryTests.TestApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

		Map<String, Object> properties = TestPropertySourceUtils.convertInlinedPropertiesToMap(
				"spring.profiles.active=test", "spring.cloud.vault.reactive.enabled=false",
				"spring.cloud.vault.uri=http://serverhostdoesnotexist:1234", "spring.config.import=vault://");
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("testPropertiesSource", properties));
		DeferredLogs logs = new DeferredLogs();
		ConfigDataEnvironmentPostProcessor configDataEnvironmentPostProcessor = new ConfigDataEnvironmentPostProcessor(
				new DeferredLogs(), bootstrapContext);
		configDataEnvironmentPostProcessor.postProcessEnvironment(environment, app);
		((DeferredLog) logs.getLog(ConfigDataEnvironmentPostProcessor.class)).replayTo(log);

		assertThatIllegalStateException().isThrownBy(() -> bootstrapContext.get(ReactiveVaultTemplate.class))
				.withMessageContaining("has not been registered");
	}

}
