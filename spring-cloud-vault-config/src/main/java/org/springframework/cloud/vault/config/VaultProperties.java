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

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.AzureMsiAuthenticationOptions;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.core.lease.LeaseEndpoints;

/**
 * Properties to configure Vault support.
 *
 * @author Spencer Gibb
 * @author Mark Paluch
 * @author Kevin Holditch
 * @author Michal Budzyn
 * @author Grenville Wilson
 * @author Mårten Svantesson
 */
@ConfigurationProperties(VaultProperties.PREFIX)
public class VaultProperties implements EnvironmentAware {

	/**
	 * Configuration prefix for config properties.
	 */
	public static final String PREFIX = "spring.cloud.vault";

	/**
	 * Enable Vault config server.
	 */
	private boolean enabled = true;

	/**
	 * Vault server host.
	 */
	private String host = "localhost";

	/**
	 * Vault server port.
	 */
	private int port = 8200;

	/**
	 * Protocol scheme. Can be either "http" or "https".
	 */
	private String scheme = "https";

	/**
	 * Vault URI. Can be set with scheme, host and port.
	 */
	@Nullable
	private String uri;

	/**
	 * Vault namespace (requires Vault Enterprise).
	 */
	@Nullable
	private String namespace;

	/**
	 * Discovery properties.
	 */
	private Discovery discovery = new Discovery();

	/**
	 * Connection timeout.
	 */
	private int connectionTimeout = 5000;

	/**
	 * Read timeout.
	 */
	private int readTimeout = 15000;

	/**
	 * Fail fast if data cannot be obtained from Vault.
	 */
	private boolean failFast = false;

	/**
	 * Static vault token. Required if {@link #authentication} is {@code TOKEN}.
	 */
	@Nullable
	private String token;

	private AppIdProperties appId = new AppIdProperties();

	private AppRoleProperties appRole = new AppRoleProperties();

	private AwsEc2Properties awsEc2 = new AwsEc2Properties();

	private AwsIamProperties awsIam = new AwsIamProperties();

	private AzureMsiProperties azureMsi = new AzureMsiProperties();

	private GcpGceProperties gcpGce = new GcpGceProperties();

	private GcpIamProperties gcpIam = new GcpIamProperties();

	private KubernetesProperties kubernetes = new KubernetesProperties();

	private PcfProperties pcf = new PcfProperties();

	private Ssl ssl = new Ssl();

	private Config config = new Config();

	private Session session = new Session();

	private Reactive reactive = new Reactive();

	/**
	 * Application name for AppId authentication.
	 */
	private String applicationName = "application";

	private AuthenticationMethod authentication = AuthenticationMethod.TOKEN;

	@Override
	public void setEnvironment(Environment environment) {

		String springAppName = environment.getProperty("spring.application.name");

		if (StringUtils.hasText(springAppName)) {
			this.applicationName = springAppName;
		}
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getScheme() {
		return this.scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Nullable
	public String getUri() {
		return this.uri;
	}

	public void setUri(@Nullable String uri) {
		this.uri = uri;
	}

	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
	}

	public Discovery getDiscovery() {
		return this.discovery;
	}

	public void setDiscovery(Discovery discovery) {
		this.discovery = discovery;
	}

	public int getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	@Nullable
	public String getToken() {
		return this.token;
	}

	public void setToken(@Nullable String token) {
		this.token = token;
	}

	public AppIdProperties getAppId() {
		return this.appId;
	}

	public void setAppId(AppIdProperties appId) {
		this.appId = appId;
	}

	public AppRoleProperties getAppRole() {
		return this.appRole;
	}

	public void setAppRole(AppRoleProperties appRole) {
		this.appRole = appRole;
	}

	public AwsEc2Properties getAwsEc2() {
		return this.awsEc2;
	}

	public void setAwsEc2(AwsEc2Properties awsEc2) {
		this.awsEc2 = awsEc2;
	}

	public AwsIamProperties getAwsIam() {
		return this.awsIam;
	}

	public void setAwsIam(AwsIamProperties awsIam) {
		this.awsIam = awsIam;
	}

	public AzureMsiProperties getAzureMsi() {
		return this.azureMsi;
	}

	public void setAzureMsi(AzureMsiProperties azureMsi) {
		this.azureMsi = azureMsi;
	}

	public GcpGceProperties getGcpGce() {
		return this.gcpGce;
	}

	public void setGcpGce(GcpGceProperties gcpGce) {
		this.gcpGce = gcpGce;
	}

	public GcpIamProperties getGcpIam() {
		return this.gcpIam;
	}

	public void setGcpIam(GcpIamProperties gcpIam) {
		this.gcpIam = gcpIam;
	}

	public KubernetesProperties getKubernetes() {
		return this.kubernetes;
	}

	public void setKubernetes(KubernetesProperties kubernetes) {
		this.kubernetes = kubernetes;
	}

	public PcfProperties getPcf() {
		return this.pcf;
	}

	public void setPcf(PcfProperties pcf) {
		this.pcf = pcf;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public Config getConfig() {
		return this.config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public Session getSession() {
		return this.session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public String getApplicationName() {
		return this.applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public AuthenticationMethod getAuthentication() {
		return this.authentication;
	}

	public void setAuthentication(AuthenticationMethod authentication) {
		this.authentication = authentication;
	}

	public Reactive getReactive() {
		return reactive;
	}

	public void setReactive(Reactive reactive) {
		this.reactive = reactive;
	}

	/**
	 * Enumeration of authentication methods.
	 */
	public enum AuthenticationMethod {

		APPID, APPROLE, AWS_EC2, AWS_IAM, AZURE_MSI, CERT, CUBBYHOLE, GCP_GCE, GCP_IAM, KUBERNETES, NONE, PCF, TOKEN;

	}

	/**
	 * Discovery properties.
	 */
	public static class Discovery {

		/**
		 * Default service Id.
		 */
		public static final String DEFAULT_VAULT = "vault";

		/**
		 * Flag to indicate that Vault server discovery is enabled (vault server URL will
		 * be looked up via discovery).
		 */
		private boolean enabled;

		/**
		 * Service id to locate Vault.
		 */
		private String serviceId = DEFAULT_VAULT;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getServiceId() {
			return this.serviceId;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

	}

	/**
	 * AppId properties.
	 */
	public static class AppIdProperties {

		/**
		 * Property value for UserId generation using a Mac-Address.
		 *
		 * @see org.springframework.vault.authentication.MacAddressUserId
		 */
		public static final String MAC_ADDRESS = "MAC_ADDRESS";

		/**
		 * Property value for UserId generation using an IP-Address.
		 *
		 * @see org.springframework.vault.authentication.IpAddressUserId
		 */
		public static final String IP_ADDRESS = "IP_ADDRESS";

		/**
		 * Mount path of the AppId authentication backend.
		 */
		private String appIdPath = "app-id";

		/**
		 * Network interface hint for the "MAC_ADDRESS" UserId mechanism.
		 */
		@Nullable
		private String networkInterface;

		/**
		 * UserId mechanism. Can be either "MAC_ADDRESS", "IP_ADDRESS", a string or a
		 * class name.
		 */
		private String userId = MAC_ADDRESS;

		public String getAppIdPath() {
			return this.appIdPath;
		}

		public void setAppIdPath(String appIdPath) {
			this.appIdPath = appIdPath;
		}

		@Nullable
		public String getNetworkInterface() {
			return this.networkInterface;
		}

		public void setNetworkInterface(@Nullable String networkInterface) {
			this.networkInterface = networkInterface;
		}

		public String getUserId() {
			return this.userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

	}

	/**
	 * AppRole properties.
	 */
	public static class AppRoleProperties {

		/**
		 * Mount path of the AppRole authentication backend.
		 */
		private String appRolePath = "approle";

		/**
		 * Name of the role, optional, used for pull-mode.
		 */
		private String role = "";

		/**
		 * The RoleId.
		 */
		@Nullable
		private String roleId;

		/**
		 * The SecretId.
		 */
		@Nullable
		private String secretId;

		public String getAppRolePath() {
			return this.appRolePath;
		}

		public void setAppRolePath(String appRolePath) {
			this.appRolePath = appRolePath;
		}

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		@Nullable
		public String getRoleId() {
			return this.roleId;
		}

		public void setRoleId(@Nullable String roleId) {
			this.roleId = roleId;
		}

		@Nullable
		public String getSecretId() {
			return this.secretId;
		}

		public void setSecretId(@Nullable String secretId) {
			this.secretId = secretId;
		}

	}

	/**
	 * AWS-EC2 properties.
	 */
	public static class AwsEc2Properties {

		/**
		 * URL of the AWS-EC2 PKCS7 identity document.
		 */
		private URI identityDocument = URI.create("http://169.254.169.254/latest/dynamic/instance-identity/pkcs7");

		/**
		 * Mount path of the AWS-EC2 authentication backend.
		 */
		private String awsEc2Path = "aws-ec2";

		/**
		 * Name of the role, optional.
		 */
		private String role = "";

		/**
		 * Nonce used for AWS-EC2 authentication. An empty nonce defaults to nonce
		 * generation.
		 */
		@Nullable
		private String nonce;

		public URI getIdentityDocument() {
			return this.identityDocument;
		}

		public void setIdentityDocument(URI identityDocument) {
			this.identityDocument = identityDocument;
		}

		public String getAwsEc2Path() {
			return this.awsEc2Path;
		}

		public void setAwsEc2Path(String awsEc2Path) {
			this.awsEc2Path = awsEc2Path;
		}

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		@Nullable
		public String getNonce() {
			return this.nonce;
		}

		public void setNonce(@Nullable String nonce) {
			this.nonce = nonce;
		}

	}

	/**
	 * AWS-IAM properties.
	 */
	public static class AwsIamProperties {

		/**
		 * Mount path of the AWS authentication backend.
		 */
		private String awsPath = "aws";

		/**
		 * Name of the role, optional. Defaults to the friendly IAM name if not set.
		 */
		private String role = "";

		/**
		 * Name of the server used to set {@code X-Vault-AWS-IAM-Server-ID} header in the
		 * headers of login requests.
		 */
		@Nullable
		private String serverName;

		/**
		 * STS server URI.
		 *
		 * @since 2.2
		 */
		@Nullable
		private URI endpointUri;

		public String getAwsPath() {
			return this.awsPath;
		}

		public String getRole() {
			return this.role;
		}

		@Nullable
		public String getServerName() {
			return this.serverName;
		}

		public void setAwsPath(String awsPath) {
			this.awsPath = awsPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setServerName(String serverName) {
			this.serverName = serverName;
		}

		@Nullable
		public URI getEndpointUri() {
			return this.endpointUri;
		}

		public void setEndpointUri(URI endpointUri) {
			this.endpointUri = endpointUri;
		}

	}

	/**
	 * Azure MSI properties.
	 */
	public static class AzureMsiProperties {

		/**
		 * Mount path of the Azure MSI authentication backend.
		 */
		private String azurePath = "azure";

		/**
		 * Name of the role.
		 */
		private String role = "";

		/**
		 * Instance metadata service URI.
		 *
		 * @since 3.0
		 */
		private URI metadataService = AzureMsiAuthenticationOptions.DEFAULT_INSTANCE_METADATA_SERVICE_URI;

		/**
		 * Identity token service URI.
		 *
		 * @since 3.0
		 */
		private URI identityTokenService = AzureMsiAuthenticationOptions.DEFAULT_IDENTITY_TOKEN_SERVICE_URI;

		public String getAzurePath() {
			return this.azurePath;
		}

		public void setAzurePath(String azurePath) {
			this.azurePath = azurePath;
		}

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public URI getMetadataService() {
			return this.metadataService;
		}

		public void setMetadataService(URI metadataService) {
			this.metadataService = metadataService;
		}

		public URI getIdentityTokenService() {
			return this.identityTokenService;
		}

		public void setIdentityTokenService(URI identityTokenService) {
			this.identityTokenService = identityTokenService;
		}

	}

	/**
	 * GCP-GCE properties.
	 */
	public static class GcpGceProperties {

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		private String gcpPath = "gcp";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Optional service account id. Using the default id if left unconfigured.
		 */
		private String serviceAccount = "";

		public String getGcpPath() {
			return this.gcpPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getServiceAccount() {
			return this.serviceAccount;
		}

		public void setGcpPath(String gcpPath) {
			this.gcpPath = gcpPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setServiceAccount(String serviceAccount) {
			this.serviceAccount = serviceAccount;
		}

	}

	/**
	 * GCP-IAM properties.
	 */
	public static class GcpIamProperties {

		/**
		 * Credentials configuration.
		 */
		private final GcpCredentials credentials = new GcpCredentials();

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		private String gcpPath = "gcp";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Overrides the GCP project Id.
		 */
		private String projectId = "";

		/**
		 * Overrides the GCP service account Id.
		 */
		private String serviceAccountId = "";

		/**
		 * Validity of the JWT token.
		 */
		private Duration jwtValidity = Duration.ofMinutes(15);

		public GcpCredentials getCredentials() {
			return this.credentials;
		}

		public String getGcpPath() {
			return this.gcpPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getProjectId() {
			return this.projectId;
		}

		public String getServiceAccountId() {
			return this.serviceAccountId;
		}

		public Duration getJwtValidity() {
			return this.jwtValidity;
		}

		public void setGcpPath(String gcpPath) {
			this.gcpPath = gcpPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setProjectId(String projectId) {
			this.projectId = projectId;
		}

		public void setServiceAccountId(String serviceAccountId) {
			this.serviceAccountId = serviceAccountId;
		}

		public void setJwtValidity(Duration jwtValidity) {
			this.jwtValidity = jwtValidity;
		}

	}

	/**
	 * GCP credential properties.
	 */
	public static class GcpCredentials {

		/**
		 * Location of the OAuth2 credentials private key.
		 *
		 * <p>
		 * Since this is a Resource, the private key can be in a multitude of locations,
		 * such as a local file system, classpath, URL, etc.
		 */
		private Resource location;

		/**
		 * The base64 encoded contents of an OAuth2 account private key in JSON format.
		 */
		private String encodedKey;

		public Resource getLocation() {
			return this.location;
		}

		public String getEncodedKey() {
			return this.encodedKey;
		}

		public void setLocation(Resource location) {
			this.location = location;
		}

		public void setEncodedKey(String encodedKey) {
			this.encodedKey = encodedKey;
		}

	}

	/**
	 * Kubernetes properties.
	 */
	public static class KubernetesProperties {

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		private String kubernetesPath = "kubernetes";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Path to the service account token file.
		 */
		private String serviceAccountTokenFile = "/var/run/secrets/kubernetes.io/serviceaccount/token";

		public String getKubernetesPath() {
			return this.kubernetesPath;
		}

		public String getRole() {
			return this.role;
		}

		public String getServiceAccountTokenFile() {
			return this.serviceAccountTokenFile;
		}

		public void setKubernetesPath(String kubernetesPath) {
			this.kubernetesPath = kubernetesPath;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public void setServiceAccountTokenFile(String serviceAccountTokenFile) {
			this.serviceAccountTokenFile = serviceAccountTokenFile;
		}

	}

	/**
	 * PCF properties.
	 */
	public static class PcfProperties {

		/**
		 * Mount path of the Kubernetes authentication backend.
		 */
		private String pcfPath = "pcf";

		/**
		 * Name of the role against which the login is being attempted.
		 */
		private String role = "";

		/**
		 * Path to the instance certificate (PEM). Defaults to {@code CF_INSTANCE_CERT}
		 * env variable.
		 */
		@Nullable
		private Resource instanceCertificate;

		/**
		 * Path to the instance key (PEM). Defaults to {@code CF_INSTANCE_KEY} env
		 * variable.
		 */
		@Nullable
		private Resource instanceKey;

		public String getPcfPath() {
			return this.pcfPath;
		}

		public void setPcfPath(String pcfPath) {
			this.pcfPath = pcfPath;
		}

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		@Nullable
		public Resource getInstanceCertificate() {
			return this.instanceCertificate;
		}

		public void setInstanceCertificate(@Nullable Resource instanceCertificate) {
			this.instanceCertificate = instanceCertificate;
		}

		@Nullable
		public Resource getInstanceKey() {
			return this.instanceKey;
		}

		public void setInstanceKey(@Nullable Resource instanceKey) {
			this.instanceKey = instanceKey;
		}

	}

	/**
	 * SSL properties.
	 */
	public static class Ssl {

		/**
		 * Trust store that holds certificates and private keys.
		 */
		@Nullable
		private Resource keyStore;

		/**
		 * Password used to access the key store.
		 */
		@Nullable
		private String keyStorePassword;

		/**
		 * Type of the key store.
		 *
		 * @since 3.0
		 */
		@Nullable
		private String keyStoreType;

		/**
		 * Trust store that holds SSL certificates.
		 */
		@Nullable
		private Resource trustStore;

		/**
		 * Password used to access the trust store.
		 */
		@Nullable
		private String trustStorePassword;

		/**
		 * Type of the trust store.
		 *
		 * @since 3.0
		 */
		@Nullable
		private String trustStoreType;

		/**
		 * Mount path of the TLS cert authentication backend.
		 */
		private String certAuthPath = "cert";

		/**
		 * List of enabled SSL/TLS protocol.
		 * @since 3.0.2
		 */
		private List<String> enabledProtocols = new ArrayList<>();

		/**
		 * List of enabled SSL/TLS cipher suites.
		 * @since 3.0.2
		 */
		private List<String> enabledCipherSuites = new ArrayList<>();

		@Nullable
		public Resource getKeyStore() {
			return this.keyStore;
		}

		public void setKeyStore(@Nullable Resource keyStore) {
			this.keyStore = keyStore;
		}

		@Nullable
		public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		public void setKeyStorePassword(@Nullable String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		@Nullable
		public String getKeyStoreType() {
			return this.keyStoreType;
		}

		public void setKeyStoreType(@Nullable String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		@Nullable
		public Resource getTrustStore() {
			return this.trustStore;
		}

		public void setTrustStore(@Nullable Resource trustStore) {
			this.trustStore = trustStore;
		}

		@Nullable
		public String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		public void setTrustStorePassword(@Nullable String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		@Nullable
		public String getTrustStoreType() {
			return this.trustStoreType;
		}

		public void setTrustStoreType(@Nullable String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}

		public String getCertAuthPath() {
			return this.certAuthPath;
		}

		public void setCertAuthPath(String certAuthPath) {
			this.certAuthPath = certAuthPath;
		}

		public List<String> getEnabledProtocols() {
			return this.enabledProtocols;
		}

		public void setEnabledProtocols(List<String> enabledProtocols) {
			this.enabledProtocols = enabledProtocols;
		}

		public List<String> getEnabledCipherSuites() {
			return this.enabledCipherSuites;
		}

		public void setEnabledCipherSuites(List<String> enabledCipherSuites) {
			this.enabledCipherSuites = enabledCipherSuites;
		}

	}

	/**
	 * Property source properties.
	 */
	public static class Config {

		/**
		 * Used to set a {@link org.springframework.core.env.PropertySource} priority.
		 * This is useful to use Vault as an override on other property sources.
		 *
		 * @see org.springframework.core.PriorityOrdered
		 */
		private int order = 0;

		private ConfigLifecycle lifecycle = new ConfigLifecycle();

		@DeprecatedConfigurationProperty(reason = "Only required for deprecated Bootstrap Context usage")
		public int getOrder() {
			return this.order;
		}

		public ConfigLifecycle getLifecycle() {
			return this.lifecycle;
		}

		public void setOrder(int order) {
			this.order = order;
		}

		public void setLifecycle(ConfigLifecycle lifecycle) {
			this.lifecycle = lifecycle;
		}

	}

	/**
	 * Configuration to Vault lifecycle management (renewal, revocation of tokens and
	 * secrets).
	 */
	public static class ConfigLifecycle {

		/**
		 * Enable lifecycle management.
		 */
		private boolean enabled = true;

		/**
		 * The time period that is at least required before renewing a lease.
		 *
		 * @since 2.2
		 */
		@Nullable
		private Duration minRenewal;

		/**
		 * The expiry threshold. {@link Lease} is renewed the given {@link Duration}
		 * before it expires.
		 *
		 * @since 2.2
		 */
		@Nullable
		private Duration expiryThreshold;

		/**
		 * Set the {@link LeaseEndpoints} to delegate renewal/revocation calls to.
		 * {@link LeaseEndpoints} encapsulates differences between Vault versions that
		 * affect the location of renewal/revocation endpoints.
		 *
		 * Can be {@link LeaseEndpoints#SysLeases} for version 0.8 or above of Vault or
		 * {@link LeaseEndpoints#Legacy} for older versions (the default).
		 *
		 * @since 2.2
		 */
		@Nullable
		private LeaseEndpoints leaseEndpoints;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Nullable
		public Duration getMinRenewal() {
			return this.minRenewal;
		}

		public void setMinRenewal(@Nullable Duration minRenewal) {
			this.minRenewal = minRenewal;
		}

		@Nullable
		public Duration getExpiryThreshold() {
			return this.expiryThreshold;
		}

		public void setExpiryThreshold(@Nullable Duration expiryThreshold) {
			this.expiryThreshold = expiryThreshold;
		}

		@Nullable
		public LeaseEndpoints getLeaseEndpoints() {
			return this.leaseEndpoints;
		}

		public void setLeaseEndpoints(@Nullable LeaseEndpoints leaseEndpoints) {
			this.leaseEndpoints = leaseEndpoints;
		}

	}

	/**
	 * Session management configuration properties.
	 *
	 * @since 3.0
	 */
	public static class Session {

		private SessionLifecycle lifecycle = new SessionLifecycle();

		public SessionLifecycle getLifecycle() {
			return this.lifecycle;
		}

		public void setLifecycle(SessionLifecycle lifecycle) {
			this.lifecycle = lifecycle;
		}

	}

	/**
	 * Configuration to Vault Session lifecycle management.
	 *
	 * @since 3.0
	 */
	public static class SessionLifecycle {

		/**
		 * Enable session lifecycle management.
		 */
		private boolean enabled = true;

		/**
		 * The time period that is at least required before renewing the
		 * {@link LoginToken}.
		 */
		private Duration refreshBeforeExpiry = Duration.ofSeconds(5);

		/**
		 * The expiry threshold for a {@link LoginToken}. The threshold represents a
		 * minimum TTL duration to consider a login token as valid. Tokens with a shorter
		 * TTL are considered expired and are not used anymore. Should be greater than
		 * {@code refreshBeforeExpiry} to prevent token expiry.
		 */
		private Duration expiryThreshold = Duration.ofSeconds(7);

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Duration getRefreshBeforeExpiry() {
			return this.refreshBeforeExpiry;
		}

		public void setRefreshBeforeExpiry(Duration refreshBeforeExpiry) {
			this.refreshBeforeExpiry = refreshBeforeExpiry;
		}

		public Duration getExpiryThreshold() {
			return this.expiryThreshold;
		}

		public void setExpiryThreshold(Duration expiryThreshold) {
			this.expiryThreshold = expiryThreshold;
		}

	}

	public static class Reactive {

		/**
		 * Enable reactive beans if dependencies are available
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
