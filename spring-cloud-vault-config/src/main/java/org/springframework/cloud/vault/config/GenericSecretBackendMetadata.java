/*
 * Copyright 2016-2017 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.util.PropertyTransformer;
import org.springframework.vault.core.util.PropertyTransformers;

/**
 * {@link SecretBackendMetadata} for the {@code generic} secret backend.
 *
 * @author Mark Paluch
 */
class GenericSecretBackendMetadata implements SecretBackendMetadata {

	private final String secretBackendPath;

	private final String key;

	private GenericSecretBackendMetadata(String secretBackendPath, String key) {

		Assert.hasText(secretBackendPath, "Secret backend path must not be empty");
		Assert.hasText(key, "Key must not be empty");

		this.key = key;
		this.secretBackendPath = secretBackendPath;
	}

	/**
	 * Create a {@link SecretBackendMetadata} for the {@code generic} secret backend given
	 * a {@code secretBackendPath} and {@code key}.
	 *
	 * @param secretBackendPath the secret backend mount path without leading/trailing
	 * slashes, must not be empty or {@literal null}.
	 * @param key the key within the secret backend. May contain slashes but not
	 * leading/trailing slashes, must not be empty or {@literal null}.
	 * @return the {@link SecretBackendMetadata}
	 */
	public static SecretBackendMetadata create(final String secretBackendPath,
			final String key) {
		return new GenericSecretBackendMetadata(secretBackendPath, key);
	}

	@Override
	public String getName() {
		return String.format("%s/%s", secretBackendPath, key);
	}

	@Override
	public PropertyTransformer getPropertyTransformer() {
		return PropertyTransformers.noop();
	}

	@Override
	public Map<String, String> getVariables() {

		Map<String, String> variables = new HashMap<>();

		variables.put("backend", secretBackendPath);
		variables.put("key", key);

		return variables;
	}

	/**
	 * Build a list of context paths from application name and the active profile names.
	 * Application name and profiles support multiple (comma-separated) values.
	 *
	 * @param genericBackendProperties
	 * @param environment
	 * @return
	 */
	public static List<String> buildContexts(
			VaultGenericBackendProperties genericBackendProperties,
			Environment environment) {

		String appName = genericBackendProperties.getApplicationName();
		List<String> profiles = Arrays.asList(environment.getActiveProfiles());
		List<String> contexts = new ArrayList<>();

		String defaultContext = genericBackendProperties.getDefaultContext();
		addContext(contexts, defaultContext, profiles, genericBackendProperties);

		for (String applicationName : StringUtils.commaDelimitedListToSet(appName)) {
			addContext(contexts, applicationName, profiles, genericBackendProperties);
		}

		Collections.reverse(contexts);
		return contexts;
	}

	private static void addContext(List<String> contexts, String applicationName,
			List<String> profiles,
			VaultGenericBackendProperties genericBackendProperties) {

		if (!StringUtils.hasText(applicationName)) {
			return;
		}

		if (!contexts.contains(applicationName)) {
			contexts.add(applicationName);
		}

		for (String profile : profiles) {

			if (!StringUtils.hasText(profile)) {
				continue;
			}

			String contextName = applicationName
					+ genericBackendProperties.getProfileSeparator() + profile.trim();

			if (!contexts.contains(contextName)) {
				contexts.add(contextName);
			}
		}
	}
}
