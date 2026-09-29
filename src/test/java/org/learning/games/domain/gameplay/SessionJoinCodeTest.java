package org.learning.games.domain.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.learning.games.domain.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class SessionJoinCodeTest {

	@Test
	void normalizesToUpperCase() {
		assertEquals("AB2CD", SessionJoinCode.parse("ab2cd").value());
	}

	@Test
	void acceptsValidFiveCharCode() {
		assertEquals("K7M2Q", SessionJoinCode.parse("K7M2Q").value());
	}

	@Test
	void rejectsWrongLength() {
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("ABCD"));
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("ABCDEF"));
	}

	@Test
	void rejectsBlank() {
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse(" "));
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse(null));
	}

	@Test
	void rejectsAmbiguousOrInvalidCharacters() {
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("O1234")); // O ambiguous
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("I1234")); // I ambiguous
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("L1234")); // L ambiguous
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("A1BCD")); // 1 ambiguous
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("A0BCD")); // 0 ambiguous
		assertThrows(BadRequestException.class, () -> SessionJoinCode.parse("AB-CD"));
	}

	@Test
	void alphabetExcludesAmbiguousGlyphs() {
		String alphabet = SessionJoinCode.ALPHABET;
		assertEquals(SessionJoinCode.LENGTH, 5);
		assertFalse(alphabet.contains("0"));
		assertFalse(alphabet.contains("1"));
		assertFalse(alphabet.contains("I"));
		assertFalse(alphabet.contains("L"));
		assertFalse(alphabet.contains("O"));
		assertTrue(alphabet.contains("A"));
		assertTrue(alphabet.contains("2"));
		assertTrue(alphabet.contains("9"));
	}
}
