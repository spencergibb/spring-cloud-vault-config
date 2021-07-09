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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.util.PropertyTransformer;
import org.springframework.vault.core.util.PropertyTransformers;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link ConfigDataLocationResolver} for Vault resolving {@link VaultConfigLocation}
 * using the {@code vault:} prefix.
 * <p>
 * Resolution considers contextual locations as we as default locations. Contextual
 * locations such as {@code vault:secret/my-application} are considered to be context
 * paths for the Key-Value secrets backend. Using a default location {@code vault:}
 * imports all enabled {@link VaultSecretBackendDescriptor secret backends } by creating
 * {@link SecretBackendMetadata} from {@link SecretBackendMetadataFactory}. Note that both
 * types,{@link VaultSecretBackendDescriptor} and {@link SecretBackendMetadataFactory} are
 * resolved through {@link SpringFactoriesLoader spring.factories} to allow optional
 * presence/absence on the class path.
 * <p>
 * Mixing paths
 * ({@code spring.config.import=vault:,vault:secret/my-application,vault:secret/other-location})
 * is possible as each config location creates an individual {@link VaultConfigLocation}.
 * By enabling/disabling {@link VaultSecretBackendDescriptor#isEnabled() a
 * VaultSecretBackendDescriptor}, you can control the amount of secret backends that are
 * imported through the default location.
 * <p>
 * You can customize the default location capabilities by registering
 * {@link VaultConfigurer} in the {@link BootstrapRegistry}. For example:
 *
 * <pre class="code">
 * VaultConfigurer configurer = …;
 * SpringApplication application = …;
 *
 * application.addBootstrapper(registy -&gt; register(VaultConfigurer.class, context -&gt; configurer));
 * </pre>
 * <p>
 * Registers also {@link VaultProperties} in the {@link BootstrapRegistry} that is
 * required later on by {@link VaultConfigDataLoader}.
 *
 * @author Mark Paluch
 * @author Jeffrey van der Laan
 * @since 3.0
 * @see VaultConfigurer
 * @see BootstrapRegistry
 * @see VaultConfigDataLoader
 */
public class VaultConfigDataLocationResolver implements ConfigDataLocationResolver<VaultConfigLocation> {

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.getValue().startsWith(VaultConfigLocation.VAULT_PREFIX);
	}

	@Override
	public List<VaultConfigLocation> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		return Collections.emptyList();
	}

	@Override
	public List<VaultConfigLocation> resolveProfileSpecific(ConfigDataLocationResolverContext context,
			ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {

		if (!location.getValue().startsWith(VaultConfigLocation.VAULT_PREFIX)) {
			return Collections.emptyList();
		}

		registerVaultProperties(context);

		if (location.getValue().equals(VaultConfigLocation.VAULT_PREFIX)
				|| location.getValue().equals(VaultConfigLocation.VAULT_PREFIX + "//")) {
			List<SecretBackendMetadata> sorted = getSecretBackends(context, profiles);
			return sorted.stream().map(it -> new VaultConfigLocation(it, location.isOptional()))
					.collect(Collectors.toList());
		}

		String contextPath = location.getValue().substring(VaultConfigLocation.VAULT_PREFIX.length());

		while (contextPath.startsWith("/")) {
			contextPath = contextPath.substring(1);
		}

		return Collections.singletonList(
				new VaultConfigLocation(contextPath, getPropertyTransformer(contextPath), location.isOptional()));
	}

	private static PropertyTransformer getPropertyTransformer(String contextPath) {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(contextPath).build();
		String prefix = uriComponents.getQueryParams().getFirst("prefix");

		if (StringUtils.hasText(prefix) && StringUtils.hasText(uriComponents.getPath())) {
			return PropertyTransformers.propertyNamePrefix(prefix);
		}

		return PropertyTransformers.noop();
	}

	private static void registerVaultProperties(ConfigDataLocationResolverContext context) {

		context.getBootstrapContext().registerIfAbsent(VaultProperties.class, ignore -> {

			VaultProperties vaultProperties = context.getBinder().bindOrCreate(VaultProperties.PREFIX,
					VaultProperties.class);

			vaultProperties.setApplicationName(context.getBinder().bind("spring.application.name", String.class)
					.orElse(vaultProperties.getApplicationName()));

			return vaultProperties;
		});

		context.getBootstrapContext().registerIfAbsent(RetryProperties.class,
				ignore -> context.getBinder().bindOrCreate(RetryProperties.PREFIX, RetryProperties.class));
	}

	private List<SecretBackendMetadata> getSecretBackends(ConfigDataLocationResolverContext context,
			Profiles profiles) {

		List<VaultSecretBackendDescriptor> descriptors = findDescriptors(context.getBinder());
		List<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> factories = getSecretBackendMetadataFactories();

		Collection<VaultConfigurer> vaultConfigurers = getVaultConfigurers(context.getBootstrapContext());
		PropertySourceLocatorConfigurationFactory factory = new PropertySourceLocatorConfigurationFactory(
				vaultConfigurers, descriptors, factories);

		VaultKeyValueBackendProperties kvProperties = getKeyValueProperties(context, profiles);

		PropertySourceLocatorConfiguration configuration = factory.getPropertySourceConfiguration(kvProperties);

		Collection<SecretBackendMetadata> secretBackends = configuration.getSecretBackends();

		List<SecretBackendMetadata> sorted = new ArrayList<>(secretBackends);
		AnnotationAwareOrderComparator.sort(sorted);
		Collections.reverse(sorted);

		return sorted;
	}

	private static Collection<VaultConfigurer> getVaultConfigurers(ConfigurableBootstrapContext bootstrapContext) {

		Collection<VaultConfigurer> vaultConfigurers = new ArrayList<>(1);

		if (bootstrapContext.isRegistered(VaultConfigurer.class)) {
			vaultConfigurers.add(bootstrapContext.get(VaultConfigurer.class));
		}

		return vaultConfigurers;
	}

	private static VaultKeyValueBackendProperties getKeyValueProperties(ConfigDataLocationResolverContext context,
			Profiles profiles) {

		VaultKeyValueBackendProperties kvProperties = context.getBinder()
				.bindOrCreate(VaultKeyValueBackendProperties.PREFIX, VaultKeyValueBackendProperties.class);

		Binder binder = context.getBinder();

		kvProperties.setApplicationName(binder.bind("spring.cloud.vault.application-name", String.class)
				.orElseGet(() -> binder.bind("spring.application.name", String.class).orElse("")));
		kvProperties.setProfiles(profiles.getActive());

		return kvProperties;
	}

	private static List<VaultSecretBackendDescriptor> findDescriptors(Binder binder) {

		List<String> descriptorClasses = SpringFactoriesLoader.loadFactoryNames(VaultSecretBackendDescriptor.class,
				VaultConfigDataLocationResolver.class.getClassLoader());

		List<VaultSecretBackendDescriptor> descriptors = new ArrayList<>(descriptorClasses.size());

		for (String className : descriptorClasses) {

			Class<VaultSecretBackendDescriptor> descriptorClass = loadClass(className);

			MergedAnnotations annotations = MergedAnnotations.from(descriptorClass);
			if (annotations.isPresent(ConfigurationProperties.class)) {

				String prefix = annotations.get(ConfigurationProperties.class).getString("prefix");
				VaultSecretBackendDescriptor hydratedDescriptor = binder.bindOrCreate(prefix, descriptorClass);
				descriptors.add(hydratedDescriptor);
			}
			else {
				throw new IllegalStateException(String.format(
						"VaultSecretBackendDescriptor %s is not annotated with @ConfigurationProperties", className));
			}
		}

		return descriptors;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<SecretBackendMetadataFactory<? super VaultSecretBackendDescriptor>> getSecretBackendMetadataFactories() {
		return (List) SpringFactoriesLoader.loadFactories(SecretBackendMetadataFactory.class,
				VaultConfigDataLocationResolver.class.getClassLoader());
	}

	@SuppressWarnings("unchecked")
	private static Class<VaultSecretBackendDescriptor> loadClass(String className) {
		try {
			return (Class<VaultSecretBackendDescriptor>) ClassUtils.forName(className,
					VaultConfigDataLocationResolver.class.getClassLoader());
		}
		catch (ReflectiveOperationException e) {
			ReflectionUtils.rethrowRuntimeException(e);

			// should never happen.
			return null;
		}
	}

}
