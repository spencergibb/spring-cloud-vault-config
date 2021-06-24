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

package org.springframework.cloud.vault.config.databases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.vault.config.PropertyNameTransformer;
import org.springframework.cloud.vault.config.SecretBackendMetadata;
import org.springframework.cloud.vault.config.SecretBackendMetadataFactory;
import org.springframework.cloud.vault.config.VaultSecretBackendDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.vault.core.util.PropertyTransformer;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bootstrap configuration providing support for the Database secret backends such as
 * Database, Apache Cassandra, Couchbase and MongoDB.
 *
 * @author Mark Paluch
 * @author Per Abich
 * @author Sebastien Nahelou
 * @author Francis Hitchens
 * @author Quintin Beukes
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ VaultMySqlProperties.class, VaultPostgreSqlProperties.class,
		VaultCassandraProperties.class, VaultCouchbaseProperties.class, VaultMongoProperties.class,
		VaultElasticsearchProperties.class, VaultMultipleDatabaseProperties.class, VaultDatabaseProperties.class })
@Order(Ordered.LOWEST_PRECEDENCE - 15)
public class VaultConfigDatabaseBootstrapConfiguration {

	@Autowired
	private ConfigurableBeanFactory beanFactory;

	@Autowired
	private VaultMultipleDatabaseProperties multipleDatabaseProperties;

	@PostConstruct
	public void registerBeans() {
		multipleDatabaseProperties.getDatabases().forEach(d -> {
			if (!beanFactory.containsBean(d.getRole())) {
				beanFactory.registerSingleton(d.getRole(), d);
			}
		});
	}

	@Bean
	@ConditionalOnMissingBean
	public DatabaseSecretBackendMetadataFactory databaseSecretBackendMetadataFactory() {
		return new DatabaseSecretBackendMetadataFactory();
	}

	/**
	 * {@link SecretBackendMetadataFactory} for Database integration using
	 * {@link DatabaseSecretProperties}.
	 */
	public static class DatabaseSecretBackendMetadataFactory
			implements SecretBackendMetadataFactory<DatabaseSecretProperties> {

		/**
		 * Creates a {@link SecretBackendMetadata} for a secret backend using
		 * {@link DatabaseSecretProperties}. This accessor transforms Vault's
		 * username/password property names to names provided with
		 * {@link DatabaseSecretProperties#getUsernameProperty()} and
		 * {@link DatabaseSecretProperties#getPasswordProperty()}.
		 * @param properties must not be {@literal null}.
		 * @return the {@link SecretBackendMetadata}
		 */
		static SecretBackendMetadata forDatabase(final DatabaseSecretProperties properties) {

			Assert.notNull(properties, "DatabaseSecretProperties must not be null");

			PropertyNameTransformer transformer = new PropertyNameTransformer();
			transformer.addKeyTransformation("username", properties.getUsernameProperty());
			transformer.addKeyTransformation("password", properties.getPasswordProperty());

			return new SecretBackendMetadata() {

				private final String credPath = properties.isStaticRole() ? "static-creds" : "creds";

				@Override
				public String getName() {
					return String.format("%s with Role %s", properties.getBackend(), properties.getRole());
				}

				@Override
				public String getPath() {
					return String.format("%s/%s/%s", properties.getBackend(), this.credPath, properties.getRole());
				}

				@Override
				public PropertyTransformer getPropertyTransformer() {
					return transformer;
				}

				@Override
				public Map<String, String> getVariables() {

					Map<String, String> variables = new HashMap<>();
					variables.put("backend", properties.getBackend());
					variables.put("key", String.format("%s/%s", this.credPath, properties.getRole()));
					return variables;
				}
			};
		}

		@Override
		public SecretBackendMetadata createMetadata(DatabaseSecretProperties backendDescriptor) {
			return forDatabase(backendDescriptor);
		}

		@Override
		public boolean supports(VaultSecretBackendDescriptor backendDescriptor) {
			return backendDescriptor instanceof DatabaseSecretProperties;
		}

	}

}
