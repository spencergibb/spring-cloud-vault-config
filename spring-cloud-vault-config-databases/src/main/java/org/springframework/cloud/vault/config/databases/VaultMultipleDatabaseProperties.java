package org.springframework.cloud.vault.config.databases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Configuration properties for multiple database secrets using the Database backend. This is configured
 * with the spring.cloud.vault.databases list.
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
