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
package org.springframework.cloud.vault.config.databases;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.vault.AbstractIntegrationTests;
import org.springframework.cloud.vault.ClientAuthentication;
import org.springframework.cloud.vault.VaultClient;
import org.springframework.cloud.vault.VaultProperties;
import org.springframework.cloud.vault.VaultTemplate;
import org.springframework.cloud.vault.config.VaultConfigOperations;
import org.springframework.cloud.vault.config.VaultConfigTemplate;
import org.springframework.cloud.vault.util.CanConnect;
import org.springframework.cloud.vault.util.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.springframework.cloud.vault.config.databases.VaultConfigDatabaseBootstrapConfiguration.DatabaseSecureBackendAccessorFactory.forDatabase;

/**
 * Integration tests for {@link VaultClient} using the mysql secret backend. This test
 * requires a running MySQL instance, see {@link #ROOT_CREDENTIALS}.
 *
 * @author Mark Paluch
 */
public class MySqlSecretIntegrationTests extends AbstractIntegrationTests {

	private final static int MYSQL_PORT = 3306;
	private final static String MYSQL_HOST = "localhost";
	private final static String ROOT_CREDENTIALS = String.format(
			"spring:vault@tcp(%s:%d)/", MYSQL_HOST, MYSQL_PORT);
	private final static String CREATE_USER_AND_GRANT_SQL = "CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';"
			+ "GRANT SELECT ON *.* TO '{{name}}'@'%';";

	private VaultProperties vaultProperties = Settings.createVaultProperties();
	private VaultConfigOperations configOperations;
	private VaultMySqlProperties mySql = new VaultMySqlProperties();

	/**
	 * Initialize the mysql secret backend.
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		assumeTrue(CanConnect.to(new InetSocketAddress(MYSQL_HOST, MYSQL_PORT)));

		mySql.setEnabled(true);
		mySql.setRole("readonly");

		if (!prepare().hasSecret(mySql.getBackend())) {
			prepare().mountSecret(mySql.getBackend());
		}

		prepare().write(String.format("%s/config/connection", mySql.getBackend()),
				Collections.singletonMap("connection_url", ROOT_CREDENTIALS));

		prepare().write(
				String.format("%s/roles/%s", mySql.getBackend(), mySql.getRole()),
				Collections.singletonMap("sql", CREATE_USER_AND_GRANT_SQL));

		VaultTemplate vaultTemplate = new VaultTemplate(vaultProperties, prepare().newVaultClient(), ClientAuthentication.token(vaultProperties));
		configOperations = new VaultConfigTemplate(vaultTemplate, vaultProperties);
	}

	@Test
	public void shouldCreateCredentialsCorrectly() throws Exception {

		Map<String, String> secretProperties = configOperations.read(forDatabase(mySql));

		assertThat(secretProperties).containsKeys("spring.datasource.username",
				"spring.datasource.password");
	}
}
