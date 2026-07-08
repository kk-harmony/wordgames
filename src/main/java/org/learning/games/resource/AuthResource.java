package org.learning.games.resource;

import org.learning.games.api.dto.UserInfo;
import org.learning.games.infra.CustomErrorWrapper;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Auth", description = "Authenticated user information")
public class AuthResource {

	@Inject
	SecurityIdentity identity;

	@GET
	@Path("/me")
	@Authenticated
	@Operation(summary = "Current user", description = "Returns the authenticated user's id")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Current user", content = @Content(schema = @Schema(implementation = UserInfo.class))),
			@APIResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public UserInfo me() {
		return new UserInfo(identity.getPrincipal().getName());
	}
}
