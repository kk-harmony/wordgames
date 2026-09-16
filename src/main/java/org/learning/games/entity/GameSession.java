package org.learning.games.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.learning.games.domain.model.SessionStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "gamesession")
public class GameSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@NotBlank
	@Size(min = 5, max = 5)
	@Column(name = "join_code", nullable = false, length = 5, unique = true)
	public String joinCode;

	@NotBlank
	@Size(min = 1, max = 100)
	public String name;

	@NotBlank
	public String adminUserId;

	@Enumerated(EnumType.STRING)
	public SessionStatus status = SessionStatus.OPEN;

	public int gamesStartedCount = 0;

	@Column(name = "current_game_id")
	public Long currentGameId;

	@Version
	public Long version = 0L;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP NOT NULL DEFAULT NOW()")
	public Instant createdAt;

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public List<SessionMember> members = new ArrayList<>();
}
