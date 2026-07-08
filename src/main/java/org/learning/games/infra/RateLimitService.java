package org.learning.games.infra;

import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RateLimitService {

	@RateLimited(bucket = "api", identityResolver = IpResolver.class)
	public void checkIp() {
		// Rate limiting is enforced by the Bucket4j interceptor.
	}
}
