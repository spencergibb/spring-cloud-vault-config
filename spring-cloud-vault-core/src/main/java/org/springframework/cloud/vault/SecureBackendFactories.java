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
package org.springframework.cloud.vault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Mark Paluch
 */
@CommonsLog
public class SecureBackendFactories {

	public static Collection<SecureBackendAccessor> createBackendAcessors(
			Collection<VaultSecretBackend> vaultSecretBackends,
			Collection<SecureBackendAccessorFactory<? super VaultSecretBackend>> factories) {

		List<SecureBackendAccessor> accessors = new ArrayList<>();

		for (VaultSecretBackend vaultSecretBackend : vaultSecretBackends) {

			if (!vaultSecretBackend.isEnabled()) {
				continue;
			}

			SecureBackendAccessor accessor = createSecureBackendAccessor(factories,
					vaultSecretBackend);
			if (accessor == null) {
				log.warn(String.format("Cannot create SecureBackendAccessor for %s",
						vaultSecretBackend));
				continue;
			}

			accessors.add(accessor);
		}

		return accessors;
	}

	private static SecureBackendAccessor createSecureBackendAccessor(
			Collection<SecureBackendAccessorFactory<? super VaultSecretBackend>> factories,
			VaultSecretBackend vaultSecretBackend) {
		SecureBackendAccessor accessor = null;
		for (SecureBackendAccessorFactory<? super VaultSecretBackend> factory : factories) {

			if (factory.supports(vaultSecretBackend)) {
				accessor = factory.createSecureBackendAccessor(vaultSecretBackend);
				break;
			}
		}
		return accessor;
	}
}
