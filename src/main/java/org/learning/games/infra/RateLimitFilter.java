package org.learning.games.infra;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION - 50)
public class RateLimitFilter implements ContainerRequestFilter {

	@Inject
	RateLimitService rateLimitService;

	@Inject
	PrincipalRateLimitService principalRateLimitService;

	@ConfigProperty(name = "app.rate-limit.enabled", defaultValue = "true")
	boolean enabled;

	@ConfigProperty(name = "app.rate-limit.per-user.enabled", defaultValue = "false")
	boolean perUserEnabled;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		if (!enabled) {
			return;
		}

		String path = requestContext.getUriInfo().getPath();
		if (path.startsWith("q/health")) {
			return;
		}

		rateLimitService.checkIp();
		if (perUserEnabled) {
			principalRateLimitService.checkUser();
		}
	}
}
