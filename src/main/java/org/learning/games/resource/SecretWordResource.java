package org.learning.games.resource;

import java.net.URI;

import org.learning.games.api.dto.SecretWordResponse;
import org.learning.games.api.dto.request.CreateSecretWordRequest;
import org.learning.games.api.mapper.SecretWordMapper;
import org.learning.games.domain.SecretWordService;
import org.learning.games.infra.CustomErrorWrapper;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/secretwords")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Secret Words", description = "Manage secret word pairs used to start games")
public class SecretWordResource {

	@Inject
	SecretWordService secretWordService;

	@Inject
	SecurityIdentity identity;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Create secret word", description = "Creates a new authentic/imposed word pair")
	@APIResponses({
			@APIResponse(responseCode = "201", description = "Secret word created", content = @Content(schema = @Schema(implementation = SecretWordResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response create(@Valid CreateSecretWordRequest request) {
		var created = secretWordService.create(request.authentic, request.imposed);
		SecretWordResponse response = SecretWordMapper.toResponse(created);
		return Response.created(URI.create("/secretwords/" + response.id)).entity(response).build();
	}

	@GET
	@Path("random")
	@Operation(summary = "Random secret word", description = "Returns a random secret word from the admin's selection pool")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Random secret word", content = @Content(schema = @Schema(implementation = SecretWordResponse.class))),
			@APIResponse(responseCode = "404", description = "No secret words available", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public SecretWordResponse randomForAdmin() {
		String userId = identity.getPrincipal().getName();
		return SecretWordMapper.toResponse(secretWordService.randomForUser(userId));
	}

	@GET
	@Path("{id}")
	@Operation(summary = "Get secret word", description = "Returns a secret word the caller is authorized to view (selection-pool admin, admin who used it, or member of a finished game that used it)")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Secret word", content = @Content(schema = @Schema(implementation = SecretWordResponse.class))),
			@APIResponse(responseCode = "403", description = "Not authorized", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Secret word not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public SecretWordResponse getById(@PathParam("id") Long id) {
		String userId = identity.getPrincipal().getName();
		return SecretWordMapper.toResponse(secretWordService.getByIdForUser(id, userId));
	}
}
