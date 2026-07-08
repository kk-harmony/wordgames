package org.learning.games.domain;

import java.util.concurrent.ThreadLocalRandom;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GameRandom {
	public int nextInt(int bound) {
		return ThreadLocalRandom.current().nextInt(bound);
	}
}
