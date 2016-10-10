package ai.vacuity.rudi.adaptors.regex;

import org.apache.commons.lang.StringUtils;

/**
 * Future home of the set graph scoring service.
 * 
 * @author In Lak'ech.
 *
 */
public class GraphMaster {

	public static double score(String pattern) {
		double score = 0;
		pattern = pattern.replaceAll("\\[.*\\]", "");
		score = pattern.length();
		if (StringUtils.startsWith(pattern, "^")) score = score + 5;
		if (StringUtils.endsWith(pattern, "$")) score = score + 5;
		return score;
	}

}
