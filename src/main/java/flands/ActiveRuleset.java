package flands;

import java.util.HashSet;
import java.util.Set;

/**
 * Set of the rules that may be active within a game.
 * Each rule extension is defined by a String key.
 * A rule may be chosen at the start of the game, in which case it is 'fixed';
 * a rule may also be required within a book, in which case it is added as
 * temporary while the character is in that book.
 * 
 * @author Jonathan Mann
 */
class ActiveRuleset {
	static String SARVEN = "Sarven";

	private Set<String> fixedRules;
	private Set<String> tempRules;

	ActiveRuleset() {
		fixedRules = new HashSet<>();
		tempRules = new HashSet<>();
	}

	boolean hasRule(String rule) {
		rule = rule.trim().toLowerCase();
		return fixedRules.contains(rule) || tempRules.contains(rule);
	}

	private void addFixedRule(String rule) {
		fixedRules.add(rule.trim().toLowerCase());
	}

	void addFixedRules(String rules) {
		if (rules == null) return;
		String[] rs = rules.split(",");
		for (String r : rs)
			addFixedRule(r);
	}

	/**
	 * Get a comma-separated list of the fixed rules (for output into a saved game).
	 */
	String getFixedRules() {
		StringBuilder sb = new StringBuilder();
		for (String fixedRule : fixedRules) {
			if (sb.length() > 0) sb.append(',');
			sb.append(fixedRule);
		}
		return sb.toString();
	}

	private void addTempRule(String rule) {
		tempRules.add(rule.trim().toLowerCase());
	}

	void addTempRules(String rules) {
		if (rules == null) return;
		String[] rs = rules.split(",");
		for (String r : rs)
			addTempRule(r);
	}

	void clearTempRules() {
		tempRules.clear();
	}
}
