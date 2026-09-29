package org.learning.games.resource;

import java.net.URI;

import org.learning.games.api.dto.GameResponse;
import org.learning.games.api.dto.SessionResponse;
import org.learning.games.api.dto.request.CreateSessionRequest;
import org.learning.games.api.dto.request.StartGameRequest;
import org.learning.games.api.mapper.GameMapper;
import org.learning.games.api.mapper.SessionMapper;
import org.learning.games.domain.SessionLifecycleService;
import org.learning.games.domain.model.GameStatus;
import org.learning.games.entity.Game;
import org.learning.games.entity.GameSession;
import org.learning.games.infra.CustomErrorWrapper;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sessions", description = "Create a multi-game session lobby and start games under it")
public class SessionResource {

	@Inject
	SecurityIdentity identity;

	@Inject
	SessionLifecycleService sessionLifecycleService;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Create session", description = "Creates a new session and adds the caller as admin")
	@APIResponses({
			@APIResponse(responseCode = "201", description = "Session created", content = @Content(schema = @Schema(implementation = SessionResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response create(@Valid CreateSessionRequest request) {
		String userId = identity.getPrincipal().getName();
		GameSession session = sessionLifecycleService.createSession(request.name, request.displayName, userId);
		SessionResponse response = toResponse(session);
		return Response.created(URI.create("/sessions/" + response.id)).entity(response).build();
	}

	@GET
	@Path("{code}")
	@Operation(summary = "Get session", description = "Returns session state for a member by join code")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Session state", content = @Content(schema = @Schema(implementation = SessionResponse.class))),
			@APIResponse(responseCode = "403", description = "Not a member", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Session not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public SessionResponse getByCode(@PathParam("code") String code) {
		String userId = identity.getPrincipal().getName();
		return toResponse(sessionLifecycleService.getSessionForMember(code, userId));
	}

	@POST
	@Path("{code}/members")
	@Operation(summary = "Join session", description = "Adds the caller as a member between games")
	@APIResponses({
			@APIResponse(responseCode = "201", description = "Joined session", content = @Content(schema = @Schema(implementation = SessionResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Session not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response join(@PathParam("code") String code, @QueryParam("displayName") String displayName) {
		String userId = identity.getPrincipal().getName();
		GameSession session = sessionLifecycleService.joinSession(code, userId, displayName);
		SessionResponse response = toResponse(session);
		return Response.created(URI.create("/sessions/" + response.id + "/members/" + userId)).entity(response).build();
	}

	@DELETE
	@Path("{code}/members/{userId}")
	@Operation(summary = "Leave or kick member", description = "Members may leave between games; admins may kick between games")
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Member kicked", content = @Content(schema = @Schema(implementation = SessionResponse.class))),
			@APIResponse(responseCode = "204", description = "Member left the session"),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Session or member not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response removeMember(@PathParam("code") String code, @PathParam("userId") String targetUserId) {
		String requesterUserId = identity.getPrincipal().getName();

		if (requesterUserId.equals(targetUserId)) {
			sessionLifecycleService.leaveSession(code, requesterUserId);
			return Response.noContent().build();
		}

		GameSession session = sessionLifecycleService.kickMember(code, requesterUserId, targetUserId);
		return Response.ok(toResponse(session)).build();
	}

	@POST
	@Path("{code}/games")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Start next game", description = "Creates and starts a game from the current session roster")
	@APIResponses({
			@APIResponse(responseCode = "201", description = "Game started", content = @Content(schema = @Schema(implementation = GameResponse.class))),
			@APIResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "403", description = "Only admin may start", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class))),
			@APIResponse(responseCode = "404", description = "Session or secret word not found", content = @Content(schema = @Schema(implementation = CustomErrorWrapper.class)))
	})
	public Response startGame(@PathParam("code") String code, @Valid StartGameRequest request) {
		String userId = identity.getPrincipal().getName();
		Game game = sessionLifecycleService.startGame(code, userId, request.secretWordId);
		GameResponse response = GameMapper.toResponse(game);
		return Response.created(URI.create("/games/" + response.id)).entity(response).build();
	}

	private SessionResponse toResponse(GameSession session) {
		GameStatus currentGameStatus = sessionLifecycleService.currentGameStatus(session);
		return SessionMapper.toResponse(session, currentGameStatus);
	}
}
