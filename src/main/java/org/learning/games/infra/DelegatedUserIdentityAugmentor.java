package org.learning.games.infra;

import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.Principal;
import java.util.Map;

@ApplicationScoped
public class DelegatedUserIdentityAugmentor implements SecurityIdentityAugmentor {

	@ConfigProperty(name = "app.bff.client-id")
	String bffClientId;

	@ConfigProperty(name = "app.bff.delegated-user-header", defaultValue = "X-Delegated-User-Id")
	String delegatedUserHeader;

	@Override
	public Uni<SecurityIdentity> augment(
			SecurityIdentity identity,
			AuthenticationRequestContext context,
			Map<String, Object> attributes) {
		RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
		if (routingContext == null) {
			return augment(identity, context);
		}
		return context.runBlocking(() -> augmentBlocking(identity, routingContext));
	}

	@Override
	public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
		return Uni.createFrom().item(identity);
	}

	private SecurityIdentity augmentBlocking(SecurityIdentity identity, RoutingContext routingContext) {
		if (identity.isAnonymous()) {
			return identity;
		}

		String delegatedUserId = routingContext.request().getHeader(delegatedUserHeader);
		if (delegatedUserId == null || delegatedUserId.isBlank()) {
			return identity;
		}

		delegatedUserId = delegatedUserId.trim();
		if (!isBffClient(identity)) {
			throw new AuthenticationFailedException("Delegation header is not allowed for this caller.");
		}

		final String effectiveUserId = delegatedUserId;
		QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
		builder.setPrincipal(new Principal() {
			@Override
			public String getName() {
				return effectiveUserId;
			}
		});
		return builder.build();
	}

	private boolean isBffClient(SecurityIdentity identity) {
		if (bffClientId.equals(identity.getPrincipal().getName())) {
			return true;
		}

		if (identity.getPrincipal() instanceof OidcJwtCallerPrincipal jwtPrincipal) {
			String azp = jwtPrincipal.getClaim("azp");
			if (bffClientId.equals(azp)) {
				return true;
			}
			String clientId = jwtPrincipal.getClaim("client_id");
			return bffClientId.equals(clientId);
		}

		return false;
	}
}
