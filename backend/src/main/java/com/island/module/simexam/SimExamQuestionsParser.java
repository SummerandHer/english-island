package com.island.module.simexam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.island.common.BusinessException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将管理员粘贴的四六级题目纯文本解析为结构化 JSON。
 * 兼容已有 JSON 数组输入。
 */
public final class SimExamQuestionsParser {

	/** 46. / 46、 / 46) / Q46. */
	private static final Pattern STEM_START = Pattern.compile(
			"(?m)^\\s*(?:Q\\s*)?(\\d{1,3})[.、．)\\]]\\s*(.+)$");

	/** A) / A. / A、 / (A) */
	private static final Pattern OPTION_LINE = Pattern.compile(
			"^\\s*(?:\\()?([A-Ha-h])(?:\\)|[.、．:])\\s*(.+)$");

	/** 46.D / 46-D / 46 D / 46、D */
	private static final Pattern ANSWER_PAIR = Pattern.compile(
			"(\\d{1,3})\\s*[.、．\\-:)\\]]\\s*([A-Ha-h])");

	private SimExamQuestionsParser() {
	}

	/**
	 * @param questionsText 题目纯文本或 JSON
	 * @param answersText   可选答案，如 {@code 46.D 47.A} 或按题序 {@code D A D C B}
	 * @return 规范化 JSON 数组字符串
	 */
	public static String normalizeToJson(String questionsText, String answersText, ObjectMapper mapper) {
		if (!StringUtils.hasText(questionsText)) {
			throw new BusinessException(400, "请粘贴题目文本");
		}
		String trimmed = questionsText.trim();
		if (trimmed.startsWith("[")) {
			try {
				JsonNode node = mapper.readTree(trimmed);
				if (!node.isArray() || node.isEmpty()) {
					throw new BusinessException(400, "题目 JSON 须为非空数组");
				}
				return mapper.writeValueAsString(node);
			} catch (BusinessException e) {
				throw e;
			} catch (Exception e) {
				// 不是合法 JSON，继续按纯文本解析
			}
		}

		List<ParsedQuestion> questions = parsePlainText(trimmed);
		if (questions.isEmpty()) {
			throw new BusinessException(400, "未能识别题目：请按「46. 题干」+「A) 选项」格式粘贴");
		}

		Map<Integer, String> answerByNum = parseAnswers(answersText, questions);
		ArrayNode arr = mapper.createArrayNode();
		for (ParsedQuestion q : questions) {
			ObjectNode obj = mapper.createObjectNode();
			obj.put("number", q.number);
			obj.put("stem", q.stem);
			ArrayNode opts = obj.putArray("options");
			String correct = answerByNum.get(q.number);
			if (q.options.isEmpty()) {
				// 长篇匹配：仅题干，答案为段落字母
				if (StringUtils.hasText(correct)) {
					obj.put("answerKey", correct);
				}
			} else {
				for (ParsedOption o : q.options) {
					ObjectNode opt = opts.addObject();
					opt.put("label", o.label);
					opt.put("content", o.content);
					opt.put("correct", correct != null && correct.equalsIgnoreCase(o.label));
				}
			}
			arr.add(obj);
		}

		long marked = 0;
		for (JsonNode n : arr) {
			if (n.has("answerKey") && StringUtils.hasText(n.get("answerKey").asText())) {
				marked++;
				continue;
			}
			JsonNode opts = n.get("options");
			if (opts != null && opts.isArray()) {
				for (JsonNode o : opts) {
					if (o.path("correct").asBoolean(false)) {
						marked++;
						break;
					}
				}
			}
		}
		if (marked == 0 && StringUtils.hasText(answersText)) {
			throw new BusinessException(400, "答案未能匹配到题目编号，请用「46.D 47.A」或按题序填写「D A D C B」");
		}

		try {
			return mapper.writeValueAsString(arr);
		} catch (Exception e) {
			throw new BusinessException(500, "题目序列化失败");
		}
	}

	/** 将库中 JSON 还原为便于编辑的纯文本（编辑回显）。 */
	public static String toPlainText(String questionsJson, ObjectMapper mapper) {
		if (!StringUtils.hasText(questionsJson)) {
			return "";
		}
		try {
			JsonNode arr = mapper.readTree(questionsJson);
			if (!arr.isArray()) {
				return questionsJson;
			}
			StringBuilder sb = new StringBuilder();
			for (JsonNode q : arr) {
				int num = q.path("number").asInt(0);
				String stem = q.path("stem").asText("");
				if (num > 0) {
					sb.append(num).append(". ").append(stem).append('\n');
				} else {
					sb.append(stem).append('\n');
				}
				JsonNode opts = q.get("options");
				if (opts != null && opts.isArray()) {
					for (JsonNode o : opts) {
						sb.append(o.path("label").asText("")).append(") ")
								.append(o.path("content").asText("")).append('\n');
					}
				}
				sb.append('\n');
			}
			return sb.toString().trim();
		} catch (Exception e) {
			return questionsJson;
		}
	}

	/** 从 JSON 抽出答案纯文本，便于编辑回显。 */
	public static String toAnswersText(String questionsJson, ObjectMapper mapper) {
		if (!StringUtils.hasText(questionsJson)) {
			return "";
		}
		try {
			JsonNode arr = mapper.readTree(questionsJson);
			if (!arr.isArray()) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			for (JsonNode q : arr) {
				int num = q.path("number").asInt(0);
				String key = q.path("answerKey").asText("");
				if (!StringUtils.hasText(key)) {
					JsonNode opts = q.get("options");
					if (opts != null && opts.isArray()) {
						for (JsonNode o : opts) {
							if (o.path("correct").asBoolean(false)) {
								key = o.path("label").asText("");
								break;
							}
						}
					}
				}
				if (num > 0 && StringUtils.hasText(key)) {
					if (!sb.isEmpty()) {
						sb.append(' ');
					}
					sb.append(num).append('.').append(key.toUpperCase(Locale.ROOT));
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	static List<ParsedQuestion> parsePlainText(String text) {
		String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = normalized.split("\n");
		List<ParsedQuestion> out = new ArrayList<>();
		ParsedQuestion current = null;
		StringBuilder stemBuf = new StringBuilder();

		for (String raw : lines) {
			String line = raw.stripTrailing();
			if (line.isBlank()) {
				continue;
			}
			Matcher stemM = STEM_START.matcher(line);
			if (stemM.matches()) {
				if (current != null) {
					current.stem = stemBuf.toString().trim();
					out.add(current);
				}
				current = new ParsedQuestion();
				current.number = Integer.parseInt(stemM.group(1));
				stemBuf = new StringBuilder(stemM.group(2).trim());
				continue;
			}
			Matcher optM = OPTION_LINE.matcher(line.trim());
			if (optM.matches() && current != null) {
				if (stemBuf.length() > 0 && current.stem == null) {
					current.stem = stemBuf.toString().trim();
					stemBuf = new StringBuilder();
				} else if (current.stem == null) {
					current.stem = stemBuf.toString().trim();
					stemBuf = new StringBuilder();
				}
				ParsedOption opt = new ParsedOption();
				opt.label = optM.group(1).toUpperCase(Locale.ROOT);
				opt.content = optM.group(2).trim();
				current.options.add(opt);
				continue;
			}
			// 题干换行续写
			if (current != null && current.options.isEmpty()) {
				if (!stemBuf.isEmpty()) {
					stemBuf.append(' ');
				}
				stemBuf.append(line.trim());
			}
		}
		if (current != null) {
			if (current.stem == null) {
				current.stem = stemBuf.toString().trim();
			}
			out.add(current);
		}
		return out;
	}

	static Map<Integer, String> parseAnswers(String answersText, List<ParsedQuestion> questions) {
		Map<Integer, String> map = new LinkedHashMap<>();
		if (!StringUtils.hasText(answersText)) {
			return map;
		}
		String text = answersText.trim();
		Matcher m = ANSWER_PAIR.matcher(text);
		boolean anyPair = false;
		while (m.find()) {
			anyPair = true;
			map.put(Integer.parseInt(m.group(1)), m.group(2).toUpperCase(Locale.ROOT));
		}
		if (anyPair) {
			return map;
		}
		// 按题序：D A D C B 或 DACDB
		List<String> letters = new ArrayList<>();
		Matcher letterM = Pattern.compile("[A-Ha-h]").matcher(text.replaceAll("[^A-Ha-h\\s]", " "));
		while (letterM.find()) {
			letters.add(letterM.group().toUpperCase(Locale.ROOT));
		}
		if (letters.isEmpty()) {
			// 紧凑串 DACDB
			for (char c : text.toCharArray()) {
				if ((c >= 'A' && c <= 'H') || (c >= 'a' && c <= 'h')) {
					letters.add(String.valueOf(c).toUpperCase(Locale.ROOT));
				}
			}
		}
		for (int i = 0; i < questions.size() && i < letters.size(); i++) {
			map.put(questions.get(i).number, letters.get(i));
		}
		return map;
	}

	static final class ParsedQuestion {
		int number;
		String stem;
		List<ParsedOption> options = new ArrayList<>();
	}

	static final class ParsedOption {
		String label;
		String content;
	}
}
