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

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RetryOperations;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * {@link ClientHttpRequest} configured with retry support
 */
class RetryableClientHttpRequest implements ClientHttpRequest {

	private final ClientHttpRequest delegateRequest;

	private final RetryOperations retryOperations;

	RetryableClientHttpRequest(ClientHttpRequest request, RetryOperations retryOperations) {
		this.delegateRequest = request;
		this.retryOperations = retryOperations;
	}

	@Override
	public ClientHttpResponse execute() throws IOException {
		return retryOperations.execute(retryContext -> delegateRequest.execute());
	}

	@Override
	public OutputStream getBody() throws IOException {
		return delegateRequest.getBody();
	}

	@Override
	public String getMethodValue() {
		return delegateRequest.getMethodValue();
	}

	@Override
	public URI getURI() {
		return delegateRequest.getURI();
	}

	@Override
	public HttpHeaders getHeaders() {
		return delegateRequest.getHeaders();
	}

}
