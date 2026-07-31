package com.island.module.simexam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimExamQuestionsParserTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void parseCetStylePaste() throws Exception {
		String text = """
				46. What is the classic grocery store dilemma?

				A) Whether or not one should eat organic food.
				B) Whether or not one can stretch their wallet far.
				C) Organic food costs more but it certainly tastes better.
				D) One wants the best food but their budget is limited.
				47. What do we learn about organic food from science?

				A) Whether it is any better remains uncertain.
				B) Whether it is healthier is under consideration.
				C) Whether it is more nutritious than conventional food is arguable.
				D) Whether it is going to replace conventional food is still unclear.
				""";
		String json = SimExamQuestionsParser.normalizeToJson(text, "46.D 47.A", mapper);
		JsonNode arr = mapper.readTree(json);
		assertEquals(2, arr.size());
		assertEquals(46, arr.get(0).path("number").asInt());
		assertEquals(4, arr.get(0).path("options").size());
		assertTrue(arr.get(0).path("options").get(3).path("correct").asBoolean());
		assertTrue(arr.get(1).path("options").get(0).path("correct").asBoolean());
	}

	@Test
	void parseAnswersByOrder() throws Exception {
		String text = """
				1. Stem one?
				A) a
				B) b
				C) c
				D) d
				2. Stem two?
				A) a
				B) b
				C) c
				D) d
				""";
		String json = SimExamQuestionsParser.normalizeToJson(text, "C B", mapper);
		JsonNode arr = mapper.readTree(json);
		assertTrue(arr.get(0).path("options").get(2).path("correct").asBoolean());
		assertTrue(arr.get(1).path("options").get(1).path("correct").asBoolean());
	}
}
