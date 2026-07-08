package org.learning.games.infra;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkiverse.bucket4j.runtime.resolver.IdentityResolver;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PrincipalIdentityResolver implements IdentityResolver {

	@Inject
	SecurityIdentity identity;

	@Override
	public String getIdentityKey() {
		if (identity.isAnonymous()) {
			return "anonymous";
		}
		return identity.getPrincipal().getName();
	}
}
