/*
 *
 *  *
 *  *  * Copyright 2019-2020 the original author or authors.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package org.springdoc.webflux.ui;

import java.net.URI;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.ArrayUtils;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.core.SwaggerUiConfigParameters;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.ui.AbstractSwaggerWelcome;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springdoc.core.Constants.SPRINGDOC_SWAGGER_UI_ENABLED;
import static org.springdoc.core.Constants.SWAGGER_CONFIG_URL;
import static org.springdoc.core.Constants.SWAGGER_UI_PATH;
import static org.springdoc.core.Constants.SWAGGER_UI_URL;
import static org.springframework.util.AntPathMatcher.DEFAULT_PATH_SEPARATOR;

/**
 * The type Swagger welcome.
 * @author bnasslahsen
 */
@Controller
@ConditionalOnProperty(name = SPRINGDOC_SWAGGER_UI_ENABLED, matchIfMissing = true)
@ConditionalOnBean(SpringDocConfiguration.class)
public class SwaggerWelcome extends AbstractSwaggerWelcome {

	/**
	 * The Web jars prefix url.
	 */
	private String webJarsPrefixUrl;

	/**
	 * The Oauth prefix.
	 */
	private UriComponentsBuilder oauthPrefix;

	/**
	 * Instantiates a new Swagger welcome.
	 *
	 * @param swaggerUiConfig the swagger ui config
	 * @param springDocConfigProperties the spring doc config properties
	 * @param swaggerUiConfigParameters the swagger ui config parameters
	 */
	public SwaggerWelcome(SwaggerUiConfigProperties swaggerUiConfig, SpringDocConfigProperties springDocConfigProperties,SwaggerUiConfigParameters swaggerUiConfigParameters) {
		super(swaggerUiConfig, springDocConfigProperties,swaggerUiConfigParameters);
		this.webJarsPrefixUrl = springDocConfigProperties.getWebjars().getPrefix();
	}

	/**
	 * Redirect to ui mono.
	 *
	 * @param request the request
	 * @param response the response
	 * @return the mono
	 */
	@Operation(hidden = true)
	@GetMapping(SWAGGER_UI_PATH)
	public Mono<Void> redirectToUi(ServerHttpRequest request, ServerHttpResponse response) {
		String contextPath = this.fromCurrentContextPath(request);
		String sbUrl = this.buildUrl(contextPath, swaggerUiConfigParameters.getUiRootPath() + springDocConfigProperties.getWebjars().getPrefix() + SWAGGER_UI_URL);
		UriComponentsBuilder uriBuilder = getUriComponentsBuilder(sbUrl);
		response.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
		response.getHeaders().setLocation(URI.create(uriBuilder.build().encode().toString()));
		return response.setComplete();
	}


	/**
	 * Gets swagger ui config.
	 *
	 * @param request the request
	 * @return the swagger ui config
	 */
	@Operation(hidden = true)
	@GetMapping(value = SWAGGER_CONFIG_URL, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> getSwaggerUiConfig(ServerHttpRequest request) {
		this.fromCurrentContextPath(request);
		return swaggerUiConfigParameters.getConfigParameters();
	}

	@Override
	protected void calculateUiRootPath(StringBuilder... sbUrls) {
		StringBuilder sbUrl = new StringBuilder();
		if (ArrayUtils.isNotEmpty(sbUrls))
			sbUrl = sbUrls[0];
		String swaggerPath = swaggerUiConfigParameters.getPath();
		if (swaggerPath.contains(DEFAULT_PATH_SEPARATOR))
			sbUrl.append(swaggerPath, 0, swaggerPath.lastIndexOf(DEFAULT_PATH_SEPARATOR));
		swaggerUiConfigParameters.setUiRootPath(sbUrl.toString());
	}

	@Override
	protected void calculateOauth2RedirectUrl(UriComponentsBuilder uriComponentsBuilder) {
		if (oauthPrefix == null && !swaggerUiConfigParameters.isValidUrl(swaggerUiConfigParameters.getOauth2RedirectUrl())) {
			this.oauthPrefix = uriComponentsBuilder.path(swaggerUiConfigParameters.getUiRootPath()).path(webJarsPrefixUrl);
			swaggerUiConfigParameters.setOauth2RedirectUrl(this.oauthPrefix.path(swaggerUiConfigParameters.getOauth2RedirectUrl()).build().toString());
		}
	}

	/**
	 * From current context path string.
	 *
	 * @param request the request
	 * @return the string
	 */
	private String fromCurrentContextPath(ServerHttpRequest request) {
		String contextPath = request.getPath().contextPath().value();
		String url = UriComponentsBuilder.fromHttpRequest(request).toUriString();
		url = url.replace(request.getPath().toString(), "");
		buildConfigUrl(contextPath, UriComponentsBuilder.fromUriString(url));
		return contextPath;
	}
}
