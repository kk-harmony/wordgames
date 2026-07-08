package org.learning.games.domain.model;

public record IdempotentResult(int status, String responseBody) {
}
