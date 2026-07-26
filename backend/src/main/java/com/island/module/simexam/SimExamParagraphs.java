package com.island.module.simexam;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 长篇信息匹配：正文以 [A] [B] … 分段。 */
public final class SimExamParagraphs {

	private static final Pattern PARA = Pattern.compile(
			"\\[([A-Z])\\]\\s*([\\s\\S]*?)(?=\\[[A-Z]\\]|\\z)");

	private SimExamParagraphs() {
	}

	public record Paragraph(String label, String text) {
	}

	public static List<Paragraph> parse(String contentEn) {
		List<Paragraph> out = new ArrayList<>();
		if (!StringUtils.hasText(contentEn)) {
			return out;
		}
		Matcher m = PARA.matcher(contentEn.trim());
		while (m.find()) {
			String label = m.group(1);
			String text = m.group(2).trim();
			if (!text.isEmpty()) {
				out.add(new Paragraph(label, text));
			}
		}
		return out;
	}

	public static List<String> labels(String contentEn) {
		return parse(contentEn).stream().map(Paragraph::label).toList();
	}
}
