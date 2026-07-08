package org.learning.games.infra;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(
		info = @Info(
				title = "Word Games API",
				version = "1.0.0",
				description = "REST API for creating, joining, and playing impostor word games"))
@SecurityScheme(
		securitySchemeName = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "JWT access token from https://customauth.fly.dev/")
public class OpenApiConfig extends Application {
}
