package org.learning.games.resource;

import java.net.URI;

import org.learning.games.api.dto.GameResponse;
import org.learning.games.api.dto.MyWordResponse;
import org.learning.games.api.dto.request.CreateGameRequest;
import org.learning.games.api.dto.request.StartGameRequest;
import org.learning.games.api.dto.request.VoteRequest;
import org.learning.games.api.idempotency.IdempotentResponseExecutor;
import org.learning.games.api.mapper.GameMapper;
import org.learning.games.domain.GameLifecycleService;
import org.learning.games.domain.GameService;
import org.learning.games.domain.IdempotencyOperation;
import org.learning.games.entity.Game;
import org.learning.games.infra.CustomErrorWrapper;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Games", description = "Create, join, and play impostor word games")
public class GameResource {

	@Inject
	SecurityIdentity identity;

	@Inject
	GameService gameService;

	@Inject
	GameLifecycleService gameLifecycleService;

	@Inject
	IdempotentResponseExecutor idempotentResponseExecutor;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Create game", description = "Creates a new game and adds the caller as admin")
	@APIResponses({
			@APIResponse(responseCode = "201", description = "Game created", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response create(@Valid CreateGameRequest request) {
		String userId = identity.getPrincipal().getName();
		Game game = gameLifecycleService.createGame(request.name, request.displayName, userId);
		GameResponse response = GameMapper.toResponse(game);
		return Response.created(URI.create("/games/" + response.id)).entity(response).build();
	}

	@GET
	@Path("{id}")
	@Operation(summary = "Get game", description = "Returns game state for a member of the game")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Game state", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "403", description = "Not a member", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Game not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public GameResponse getById(@PathParam("id") Long id) {
		String userId = identity.getPrincipal().getName();
		return GameMapper.toResponse(gameService.getGameForMember(id, userId));
	}

	@POST
	@Path("{id}/members")
	@Operation(summary = "Join game", description = "Adds the caller as a member of a waiting game")
	@APIResponses({
			@APIResponse(responseCode = "201", description = "Joined game", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Game not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response join(@PathParam("id") Long id, @QueryParam("displayName") String displayName) {
		String userId = identity.getPrincipal().getName();
		Game game = gameLifecycleService.joinGame(id, userId, displayName);
		GameResponse response = GameMapper.toResponse(game);
		return Response.created(URI.create("/games/" + id + "/members/" + userId)).entity(response).build();
	}

	@DELETE
	@Path("{id}/members/{userId}")
	@Operation(summary = "Leave or kick member", description = "Members may leave a waiting game; admins may kick members during play")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Member kicked", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "204", description = "Member left the game"),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Game or member not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response removeMember(@PathParam("id") Long id, @PathParam("userId") String targetUserId) {
		String requesterUserId = identity.getPrincipal().getName();

		if (requesterUserId.equals(targetUserId)) {
			gameLifecycleService.leaveGame(id, requesterUserId);
			return Response.noContent().build();
		}

		Game game = gameService.kickMember(id, requesterUserId, targetUserId);
		return Response.ok(GameMapper.toResponse(game)).build();
	}

	@POST
	@Path("{id}/start")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Start game", description = "Starts a waiting game with the selected secret word")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Game started", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Only admin may start", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Game or secret word not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public GameResponse start(@PathParam("id") Long id, @Valid StartGameRequest request) {
		String userId = identity.getPrincipal().getName();
		return GameMapper.toResponse(gameService.startGame(id, userId, request.secretWordId));
	}

	@POST
	@Path("{id}/turn/complete")
	@Operation(summary = "Complete turn", description = "Marks the current player's turn as complete")
	@Parameter(
			name = "Idempotency-Key",
			in = ParameterIn.HEADER,
			description = "Optional idempotency key for safe retries")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Turn completed", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "409", description = "Conflict (optimistic lock or idempotency key reuse)", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response completeTurn(
			@PathParam("id") Long id,
			@HeaderParam("Idempotency-Key") String idempotencyKey) {
		String userId = identity.getPrincipal().getName();
		return idempotentResponseExecutor.execute(
				id,
				userId,
				idempotencyKey,
				IdempotencyOperation.COMPLETE_TURN,
				"turn",
				() -> GameMapper.toResponse(gameService.completeTurn(id, userId)));
	}

	@GET
	@Path("{id}/my-word")
	@Operation(summary = "Get assigned word", description = "Returns the caller's assigned word after the game has started")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Assigned word", content = @Content(schema = @Schema(implementation = MyWordResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Not a member", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Game not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public MyWordResponse myWord(@PathParam("id") Long id) {
		String userId = identity.getPrincipal().getName();
		return GameMapper.toMyWordResponse(gameService.getMyWord(id, userId));
	}

	@POST
	@Path("{id}/vote")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Cast vote", description = "Casts a vote during the voting phase")
	@Parameter(
			name = "Idempotency-Key",
			in = ParameterIn.HEADER,
			description = "Optional idempotency key for safe retries")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Vote cast", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "409", description = "Conflict (optimistic lock or idempotency key reuse)", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response vote(
			@PathParam("id") Long id,
			@Valid VoteRequest request,
			@HeaderParam("Idempotency-Key") String idempotencyKey) {
		String userId = identity.getPrincipal().getName();
		return idempotentResponseExecutor.execute(
				id,
				userId,
				idempotencyKey,
				IdempotencyOperation.VOTE,
				request.votedUserId,
				() -> GameMapper.toResponse(gameService.castVote(id, userId, request.votedUserId)));
	}
}
