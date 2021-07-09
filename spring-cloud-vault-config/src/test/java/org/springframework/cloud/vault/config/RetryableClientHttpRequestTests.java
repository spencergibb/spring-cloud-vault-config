/*
 * Copyright 2016-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryableClientHttpRequestTests {

	@Test
	public void shouldRetry() throws URISyntaxException, IOException {
		ClientHttpRequestFactory delegate = mock(ClientHttpRequestFactory.class);
		ClientHttpRequest delegateRequest = mock(ClientHttpRequest.class);
		when(delegateRequest.execute()).thenThrow(new SocketTimeoutException());
		when(delegate.createRequest(any(), any())).thenReturn(delegateRequest);
		ClientHttpRequestFactory retryableFactory = VaultRetryUtil
				.createRetryableClientHttpRequestFactory(new RetryProperties(), delegate);
		ClientHttpRequest request = retryableFactory.createRequest(new URI("https://spring.io/"), HttpMethod.GET);
		try {
			request.execute();
		}
		catch (SocketTimeoutException e) {
			// expected
		}
		verify(delegateRequest, times(6)).execute();

	}

}
