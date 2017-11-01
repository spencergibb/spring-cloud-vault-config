/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.vault.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.LoginToken;
import org.springframework.vault.client.VaultClients;
import org.springframework.vault.client.VaultClients.PrefixAwareUriTemplateHandler;
import org.springframework.vault.support.VaultToken;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link KubernetesAuthentication}.
 *
 * @author Michal Budzyn
 */
public class KubernetesAuthenticationUnitTests {

	private RestTemplate restTemplate;
	private MockRestServiceServer mockRest;

	@Before
	public void before() throws Exception {

		RestTemplate restTemplate = VaultClients.createRestTemplate();
		restTemplate.setUriTemplateHandler(new PrefixAwareUriTemplateHandler());
		this.mockRest = MockRestServiceServer.createServer(restTemplate);
		this.restTemplate = restTemplate;
	}

	@Test
	public void loginShouldObtainTokenWithStaticJwtSupplier() throws Exception {

		KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
				.role("hello") //
				.jwtSupplier((new KubernetesJwtSupplier() {
					@Override
					public String get() {
						return "my-jwt-token";
					}
				})).build();

		mockRest.expect(requestTo("/auth/kubernetes/login"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.role").value("hello"))
				.andExpect(jsonPath("$.jwt").value("my-jwt-token"))
				.andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON)
						.body("{" + "\"auth\":{\"client_token\":\"my-token\"}" + "}"));

		KubernetesAuthentication authentication = new KubernetesAuthentication(options, restTemplate);

		VaultToken login = authentication.login();
		assertThat(login).isInstanceOf(LoginToken.class);
		assertThat(login.getToken()).isEqualTo("my-token");
	}

	@Test(expected = VaultException.class)
	public void loginShouldFail() throws Exception {

		KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
				.role("hello").jwtSupplier(new KubernetesJwtSupplier() {
					@Override
					public String get() {
						return "my-jwt-token";
					}
				}).build();

		mockRest.expect(requestTo("/auth/kubernetes/login")) //
				.andRespond(withServerError());

		new KubernetesAuthentication(options, restTemplate).login();
	}
}
