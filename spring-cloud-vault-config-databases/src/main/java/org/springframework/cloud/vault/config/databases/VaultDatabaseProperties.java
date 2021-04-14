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

package org.springframework.cloud.vault.config.databases;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for Vault using the Database integration.
 *
 * @author Per Abich
 * @author Sebastien Nahelou
 * @since 2.0
 */
@ConfigurationProperties("spring.cloud.vault.database")
public class VaultDatabaseProperties implements DatabaseSecretProperties {

	/**
	 * Enable database backend usage.
	 */
	private boolean enabled = false;

	/**
	 * Role name for credentials.
	 */
	@Nullable
	private String role;

	/**
	 * Enable static role usage.
	 */
	private boolean staticRole = false;

	/**
	 * Database backend path.
	 */
	private String backend = "database";

	/**
	 * Target property for the obtained username.
	 */
	private String usernameProperty = "spring.datasource.username";

	/**
	 * Target property for the obtained password.
	 */
	private String passwordProperty = "spring.datasource.password";

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	@Nullable
	public String getRole() {
		return this.role;
	}

	public void setRole(@Nullable String role) {
		this.role = role;
	}

	@Override
	public boolean isStaticRole() {
		return this.staticRole;
	}

	public void setStaticRole(boolean staticRole) {
		this.staticRole = staticRole;
	}

	@Override
	public String getBackend() {
		return this.backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	@Override
	public String getUsernameProperty() {
		return this.usernameProperty;
	}

	public void setUsernameProperty(String usernameProperty) {
		this.usernameProperty = usernameProperty;
	}

	@Override
	public String getPasswordProperty() {
		return this.passwordProperty;
	}

	public void setPasswordProperty(String passwordProperty) {
		this.passwordProperty = passwordProperty;
	}

}
