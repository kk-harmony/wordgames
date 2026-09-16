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
@Table(name = "sessionmember", uniqueConstraints = @UniqueConstraint(columnNames = { "session_id", "user_id" }))
public class SessionMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@ManyToOne
	@JoinColumn(name = "session_id")
	public GameSession session;

	@NotBlank
	public String userId;

	@Size(max = 30)
	public String displayName;

	@Enumerated(EnumType.STRING)
	public MemberRole role;
}
