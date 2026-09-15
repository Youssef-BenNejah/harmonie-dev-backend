package com.harmoniedev.api.pdf;

/** Converts a non-negative integer into French words (e.g. 22135 -> "vingt-deux mille cent trente-cinq"). */
final class NumberToWordsFr {
	private static final String[] UNITS = {
		"zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
		"dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"
	};
	private static final String[] TENS = {
		"", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre-vingt", "quatre-vingt"
	};

	private NumberToWordsFr() {}

	static String convert(long n) {
		if (n == 0) return "zéro";
		if (n < 0) return "moins " + convert(-n);

		StringBuilder sb = new StringBuilder();
		long milliards = n / 1_000_000_000L;
		n %= 1_000_000_000L;
		long millions = n / 1_000_000L;
		n %= 1_000_000L;
		long milliers = n / 1000L;
		long reste = n % 1000L;

		if (milliards > 0) {
			sb.append(convertUnder1000(milliards)).append(milliards > 1 ? " milliards " : " milliard ");
		}
		if (millions > 0) {
			sb.append(convertUnder1000(millions)).append(millions > 1 ? " millions " : " million ");
		}
		if (milliers > 0) {
			if (milliers == 1) sb.append("mille ");
			else sb.append(convertUnder1000(milliers)).append(" mille ");
		}
		if (reste > 0) {
			sb.append(convertUnder1000(reste));
		}
		return sb.toString().trim();
	}

	private static String convertUnder1000(long n) {
		StringBuilder sb = new StringBuilder();
		long centaines = n / 100;
		long reste = n % 100;
		if (centaines > 0) {
			if (centaines > 1) sb.append(UNITS[(int) centaines]).append(" ");
			sb.append("cent");
			if (centaines > 1 && reste == 0) sb.append("s");
			if (reste > 0) sb.append(" ");
		}
		if (reste > 0) {
			sb.append(convertUnder100(reste));
		}
		return sb.toString().trim();
	}

	private static String convertUnder100(long n) {
		if (n < 20) return UNITS[(int) n];
		int dizaine = (int) (n / 10);
		int unite = (int) (n % 10);
		if (dizaine == 7 || dizaine == 9) {
			int base = dizaine - 1;
			if (unite == 0) return TENS[base] + "-dix";
			if (unite == 1) return TENS[base] + "-et-" + UNITS[10 + unite];
			return TENS[base] + "-" + UNITS[10 + unite];
		}
		String tensWord = TENS[dizaine];
		if (unite == 0) {
			return dizaine == 8 ? tensWord + "s" : tensWord;
		}
		if (unite == 1 && dizaine != 8) {
			return tensWord + "-et-un";
		}
		return tensWord + "-" + UNITS[unite];
	}
}
