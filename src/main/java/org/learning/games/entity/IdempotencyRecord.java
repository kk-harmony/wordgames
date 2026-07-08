package org.learning.games.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "idempotency_record", uniqueConstraints = @UniqueConstraint(
		name = "uk_idempotency_user_key",
		columnNames = { "user_id", "idempotency_key" }))
public class IdempotencyRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Column(name = "idempotency_key", nullable = false, length = 128)
	public String idempotencyKey;

	@Column(name = "user_id", nullable = false)
	public String userId;

	@Column(nullable = false, length = 50)
	public String operation;

	@Column(name = "game_id", nullable = false)
	public Long gameId;

	@Column(name = "request_hash", nullable = false)
	public String requestHash;

	@Column(name = "response_status", nullable = false)
	public int responseStatus;

	@Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
	public String responseBody;

	@Column(name = "created_at", nullable = false)
	public Instant createdAt = Instant.now();
}
