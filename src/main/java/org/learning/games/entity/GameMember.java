package org.learning.games.entity;

import org.learning.games.domain.model.MemberRole;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "gamemember", uniqueConstraints = @UniqueConstraint(columnNames = { "game_id", "user_id" }))
public class GameMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@ManyToOne
	@JoinColumn(name = "game_id")
	public Game game;

	@NotBlank
	public String userId;

	@Size(max = 30)
	public String displayName;

	@Enumerated(EnumType.STRING)
	public MemberRole role;

	public String assignedWord;

	public String votedForUserId;

	public boolean turnCompleted = false;

	public boolean eliminated = false;
}
