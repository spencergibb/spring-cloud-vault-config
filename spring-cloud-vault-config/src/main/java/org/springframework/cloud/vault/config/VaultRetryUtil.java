/*
 * Copyright 2018-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Util class for building objects that rely on spring-retry
 */
final class VaultRetryUtil {

	private static final Log log = LogFactory.getLog(VaultRetryUtil.class);

	private VaultRetryUtil() {

	}

	static RetryTemplate createRetryTemplate(RetryProperties retryProperties) {
		RetryTemplate retryTemplate = new RetryTemplate();

		ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
		policy.setInitialInterval(retryProperties.getInitialInterval());
		policy.setMultiplier(retryProperties.getMultiplier());
		policy.setMaxInterval(retryProperties.getMaxInterval());

		retryTemplate.setBackOffPolicy(policy);
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(retryProperties.getMaxAttempts()));

		return retryTemplate;
	}

	static ClientHttpRequestFactory createRetryableClientHttpRequestFactory(RetryTemplate retryTemplate,
			ClientHttpRequestFactory delegate) {
		return (uri, httpMethod) -> retryTemplate.execute(retryContext -> {
			ClientHttpRequest request = delegate.createRequest(uri, httpMethod);
			return new RetryableClientHttpRequest(request, retryTemplate);
		});
	}

	static ClientHttpRequestFactory createRetryableClientHttpRequestFactory(RetryProperties retryProperties,
			ClientHttpRequestFactory delegate) {
		RetryTemplate retryTemplate = createRetryTemplate(retryProperties);

		return createRetryableClientHttpRequestFactory(retryTemplate, delegate);
	}

}
