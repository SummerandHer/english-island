package com.island.module.translation;

/**
 * 四六级翻译 100 分制 → 15 分制换算与官方分档文案。
 */
public final class TranslationCetScore {

	private TranslationCetScore() {
	}

	public record Band(String code, String label, String description) {
	}

	public static int toCetScore(int score100) {
		return Math.clamp(Math.round(score100 * 15f / 100f), 0, 15);
	}

	public static Band bandFromScore100(int score100) {
		return bandFromCetScore(toCetScore(score100));
	}

	public static Band bandFromCetScore(int cetScore) {
		if (cetScore >= 13) {
			return new Band("13-15", "优秀",
					"译文准确表达原文意思，用词贴切，行文流畅，基本上无语言错误。");
		}
		if (cetScore >= 10) {
			return new Band("10-12", "良好",
					"译文基本上表达了原文意思，文字通顺连贯，无重大语言错误。");
		}
		if (cetScore >= 7) {
			return new Band("7-9", "及格",
					"译文勉强表达原文意思，用词欠准确，语言错误较多。");
		}
		if (cetScore >= 4) {
			return new Band("4-6", "较差",
					"译文仅表达一小部分原文意思，严重语言错误较多。");
		}
		if (cetScore >= 1) {
			return new Band("1-3", "很差",
					"译文支离破碎，绝大部分内容未表达原文意思。");
		}
		return new Band("0", "未达要求",
				"未作答或译文与原文毫不相关。");
	}
}
