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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for multiple database secrets using the Database backend. This
 * is configured with the spring.cloud.vault.databases list.
 *
 * @author Quintin Beukes
 * @since 3.0.3
 */
@ConfigurationProperties("spring.cloud.vault")
public class VaultMultipleDatabaseProperties {

	private List<VaultDatabaseProperties> databases;

	public List<VaultDatabaseProperties> getDatabases() {
		return databases;
	}

	public void setDatabases(List<VaultDatabaseProperties> databases) {
		this.databases = databases;
	}

}
