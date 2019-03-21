/*
 * Copyright 2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Unit tests for {@link VaultPropertySourceLocator}.
 *
 * @author Ryan Hoegg
 */
@RunWith(MockitoJUnitRunner.class)
public class VaultPropertySourceLocatorUnitTests {

	private VaultPropertySourceLocator propertySourceLocator;

	@Mock
	private VaultConfigTemplate operations;

	@Mock
	private ConfigurableEnvironment configurableEnvironment;

	@Mock
	private VaultPropertySource vaultPropertySource;

	@Before
	public void before() {
		propertySourceLocator = new VaultPropertySourceLocator(operations,
			new VaultProperties(), new VaultGenericBackendProperties(),
			Collections.<SecretBackendMetadata>emptyList());
	}

	@Test
	public void getOrderShouldReturnConfiguredOrder() {

		VaultProperties vaultProperties = new VaultProperties();
		vaultProperties.getConfig().setOrder(42);

		propertySourceLocator = new VaultPropertySourceLocator(operations,
			vaultProperties, new VaultGenericBackendProperties(),
			Collections.<SecretBackendMetadata>emptyList());

		assertThat(propertySourceLocator.getOrder()).isEqualTo(42);
	}

	@Test
	public void shouldLocateOnePropertySourceWithEmptyProfiles() {

		when(configurableEnvironment.getActiveProfiles()).thenReturn(new String[0]);

		PropertySource<?> propertySource = propertySourceLocator
			.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).hasSize(1);
	}

	@Test
	public void shouldLocatePropertySourcesForActiveProfilesInDefaultContext() {

		when(configurableEnvironment.getActiveProfiles())
			.thenReturn(new String[] { "vermillion", "periwinkle" });

		PropertySource<?> propertySource = propertySourceLocator
			.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources())
			.extracting("name")
			.containsAll(Arrays.asList(new String[] {
				"secret/application/vermillion",
				"secret/application/periwinkle" }));
	}

	@Test
	public void shouldLocatePropertySourcesInVaultApplicationContext() {
		final VaultGenericBackendProperties backendProperties = new VaultGenericBackendProperties();
		backendProperties.setApplicationName("wintermute");
		propertySourceLocator = new VaultPropertySourceLocator(operations,
			new VaultProperties(), backendProperties, Collections.<SecretBackendMetadata>emptyList());

		when(configurableEnvironment.getActiveProfiles())
			.thenReturn(new String[] { "vermillion", "periwinkle" });

		PropertySource<?> propertySource = propertySourceLocator
			.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources()).extracting("name")
			.containsAll(Arrays.asList(new String[] {
				"secret/wintermute",
				"secret/wintermute/vermillion",
				"secret/wintermute/periwinkle" }));
	}

	@Test
	public void shouldLocatePropertySourcesInEachPathSpecifiedWhenApplicationNameContainsSeveral() {
		final VaultGenericBackendProperties backendProperties = new VaultGenericBackendProperties();
		backendProperties.setApplicationName("wintermute,straylight,icebreaker/armitage");
		propertySourceLocator = new VaultPropertySourceLocator(
			operations,	new VaultProperties(), backendProperties, Collections.<SecretBackendMetadata>emptyList());

		when(configurableEnvironment.getActiveProfiles())
			.thenReturn(new String[] { "vermillion", "periwinkle" });

		PropertySource<?> propertySource =
			propertySourceLocator.locate(configurableEnvironment);

		assertThat(propertySource).isInstanceOf(CompositePropertySource.class);

		CompositePropertySource composite = (CompositePropertySource) propertySource;
		assertThat(composite.getPropertySources())
			.extracting("name")
			.containsAll(Arrays.asList(
				new String[] { "secret/wintermute",
					"secret/straylight",
					"secret/icebreaker/armitage",
					"secret/wintermute/vermillion",
					"secret/wintermute/periwinkle",
					"secret/straylight/vermillion",
					"secret/straylight/periwinkle",
					"secret/icebreaker/armitage/vermillion",
					"secret/icebreaker/armitage/periwinkle" }));
	}
}