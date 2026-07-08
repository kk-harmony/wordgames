package org.learning.games.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.learning.games.domain.model.GameOutcome;
import org.learning.games.domain.model.GameStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "game")
public class Game {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@NotBlank
	@Size(min = 1, max = 100)
	public String name;

	@NotBlank
	public String adminUserId;

	@Enumerated(EnumType.STRING)
	public GameStatus status = GameStatus.WAITING;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "secret_word_id")
	public SecretWord secretWord;

	public String impostorUserId;

	@Enumerated(EnumType.STRING)
	public GameOutcome outcome;

	public int currentRound = 0;

	public int voteResetCount = 0;

	public String currentTurnUserId;

	@Version
	public Long version = 0L;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP NOT NULL DEFAULT NOW()")
	public Instant createdAt;

	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public List<GameMember> members = new ArrayList<>();
}
