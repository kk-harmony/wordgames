package org.learning.games.api.dto;

import org.learning.games.domain.model.WordType;

public class MyWordResponse {
	public String word;
	public WordType type;

	public MyWordResponse() {
	}

	public MyWordResponse(String word, WordType type) {
		this.word = word;
		this.type = type;
	}
}
