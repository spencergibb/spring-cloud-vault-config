/*
 * Copyright 2017-2021 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * Abstract {@link PropertySourceLocator} to create {@link PropertySource}s based on
 * {@link VaultKeyValueBackendProperties} and {@link SecretBackendMetadata}.
 *
 * @author Mark Paluch
 */
public abstract class VaultPropertySourceLocatorSupport implements PropertySourceLocator {

	private final String propertySourceName;

	private final PropertySourceLocatorConfiguration propertySourceLocatorConfiguration;

	/**
	 * Creates a new {@link VaultPropertySourceLocatorSupport} given a
	 * {@link PropertySourceLocatorConfiguration}.
	 * @param propertySourceName must not be {@literal null} or empty.
	 * @param propertySourceLocatorConfiguration must not be {@literal null}.
	 * @since 1.1
	 */
	public VaultPropertySourceLocatorSupport(String propertySourceName,
			PropertySourceLocatorConfiguration propertySourceLocatorConfiguration) {

		Assert.hasText(propertySourceName, "PropertySource name must not be empty");
		Assert.notNull(propertySourceLocatorConfiguration, "PropertySourceLocatorConfiguration must not be null");

		this.propertySourceName = propertySourceName;
		this.propertySourceLocatorConfiguration = propertySourceLocatorConfiguration;
	}

	static PropertySourceLocatorConfiguration createConfiguration(VaultKeyValueBackendProperties kvBackendProperties) {

		Assert.notNull(kvBackendProperties, "VaultKeyValueBackendProperties must not be null");

		return new KeyValuePropertySourceLocatorConfiguration(kvBackendProperties);
	}

	@Override
	public PropertySource<?> locate(Environment environment) {

		if (this.propertySourceLocatorConfiguration instanceof EnvironmentAware) {
			((EnvironmentAware) this.propertySourceLocatorConfiguration).setEnvironment(environment);
		}

		CompositePropertySource propertySource = createCompositePropertySource(environment);
		initialize(propertySource);

		return propertySource;
	}

	// -------------------------------------------------------------------------
	// Implementation hooks and helper methods
	// -------------------------------------------------------------------------

	/**
	 * Allows initialization the {@link PropertySource} before use. Implementations may
	 * override this method to preload properties in the {@link PropertySource}.
	 * @param propertySource must not be {@literal null}.
	 */
	protected void initialize(CompositePropertySource propertySource) {
	}

	/**
	 * Creates a {@link CompositePropertySource}.
	 * @param environment must not be {@literal null}.
	 * @return the composite {@link PropertySource}.
	 */
	protected CompositePropertySource createCompositePropertySource(Environment environment) {

		List<PropertySource<?>> propertySources = doCreatePropertySources(environment);

		return doCreateCompositePropertySource(this.propertySourceName, propertySources);
	}

	/**
	 * Create {@link PropertySource}s given {@link Environment} from the property
	 * configuration.
	 * @param environment must not be {@literal null}.
	 * @return a {@link List} of ordered {@link PropertySource}s.
	 */
	protected List<PropertySource<?>> doCreatePropertySources(Environment environment) {

		Collection<SecretBackendMetadata> secretBackends = this.propertySourceLocatorConfiguration.getSecretBackends();
		List<SecretBackendMetadata> sorted = new ArrayList<>(secretBackends);

		AnnotationAwareOrderComparator.sort(sorted);

		List<PropertySource<?>> propertySources = new ArrayList<>(doCreateKeyValuePropertySources(environment));

		for (SecretBackendMetadata backendAccessor : sorted) {
			PropertySource<?> vaultPropertySource = createVaultPropertySource(backendAccessor);
			propertySources.add(vaultPropertySource);
		}

		return propertySources;
	}

	/**
	 * Create {@link PropertySource}s using the kv {@literal secret} backend. Property
	 * sources for the kv secret backend derive from the application name and active
	 * profiles to generate context paths.
	 * @param environment must not be {@literal null}.
	 * @return {@link List} of {@link PropertySource}s.
	 */
	protected List<PropertySource<?>> doCreateKeyValuePropertySources(Environment environment) {
		return new ArrayList<>();
	}

	/**
	 * Create a {@link CompositePropertySource} given a {@link List} of
	 * {@link PropertySource}s.
	 * @param propertySourceName the property source name.
	 * @param propertySources the property sources.
	 * @return the {@link CompositePropertySource} to use.
	 */
	protected CompositePropertySource doCreateCompositePropertySource(String propertySourceName,
			List<PropertySource<?>> propertySources) {

		CompositePropertySource compositePropertySource = new CompositePropertySource(propertySourceName);

		for (PropertySource<?> propertySource : propertySources) {
			compositePropertySource.addPropertySource(propertySource);
		}

		return compositePropertySource;
	}

	/**
	 * Create {@link VaultPropertySource} initialized with a {@link SecretBackendMetadata}
	 * .
	 * @param accessor the {@link SecretBackendMetadata}.
	 * @return the {@link VaultPropertySource} to use.
	 */
	protected abstract PropertySource<?> createVaultPropertySource(SecretBackendMetadata accessor);

	private static class KeyValuePropertySourceLocatorConfiguration implements PropertySourceLocatorConfiguration {

		private final VaultKeyValueBackendPropertiesSupport keyValueBackendProperties;

		KeyValuePropertySourceLocatorConfiguration(VaultKeyValueBackendPropertiesSupport keyValueBackendProperties) {
			this.keyValueBackendProperties = keyValueBackendProperties;
		}

		@Override
		public Collection<SecretBackendMetadata> getSecretBackends() {

			if (this.keyValueBackendProperties.isEnabled()) {

				List<String> contexts = KeyValueSecretBackendMetadata.buildContexts(this.keyValueBackendProperties,
						this.keyValueBackendProperties.getProfiles());

				List<SecretBackendMetadata> result = new ArrayList<>(contexts.size());

				for (String context : contexts) {
					result.add(
							KeyValueSecretBackendMetadata.create(this.keyValueBackendProperties.getBackend(), context));
				}

				return result;
			}

			return Collections.emptyList();
		}

	}

	private static class WrappedPropertySourceLocatorConfiguration implements PropertySourceLocatorConfiguration {

		private final List<SecretBackendMetadata> metadata;

		WrappedPropertySourceLocatorConfiguration(List<SecretBackendMetadata> metadata) {
			this.metadata = metadata;
		}

		@Override
		public Collection<SecretBackendMetadata> getSecretBackends() {
			return this.metadata;
		}

	}

	private static class CompositePropertySourceConfiguration
			implements PropertySourceLocatorConfiguration, EnvironmentAware {

		private final List<PropertySourceLocatorConfiguration> configurations;

		CompositePropertySourceConfiguration(PropertySourceLocatorConfiguration... configurations) {

			List<PropertySourceLocatorConfiguration> copy = new ArrayList<>(Arrays.asList(configurations));

			AnnotationAwareOrderComparator.sortIfNecessary(copy);

			this.configurations = copy;
		}

		@Override
		public Collection<SecretBackendMetadata> getSecretBackends() {

			List<SecretBackendMetadata> result = new ArrayList<>();

			for (PropertySourceLocatorConfiguration configuration : this.configurations) {
				result.addAll(configuration.getSecretBackends());
			}

			return result;
		}

		@Override
		public void setEnvironment(Environment environment) {

			for (PropertySourceLocatorConfiguration configuration : this.configurations) {
				if (configuration instanceof EnvironmentAware) {
					((EnvironmentAware) configuration).setEnvironment(environment);
				}
			}
		}

	}

}
