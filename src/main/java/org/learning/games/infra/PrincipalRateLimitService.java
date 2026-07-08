package org.learning.games.infra;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkiverse.bucket4j.runtime.RateLimited;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PrincipalRateLimitService {

	@Inject
	SecurityIdentity identity;

	@RateLimited(bucket = "api-user", identityResolver = PrincipalIdentityResolver.class)
	public void checkUser() {
		if (identity.isAnonymous()) {
			return;
		}
		// Rate limiting is enforced by the Bucket4j interceptor.
	}
}
