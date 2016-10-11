package ai.vacuity.rudi.adaptors.regex;

import org.apache.commons.lang.StringUtils;

import ai.vacuity.rudi.adaptors.bo.Cache;
import ai.vacuity.rudi.adaptors.bo.InputProtocol;

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

	public static final InputProtocol[] regexPatterns = new InputProtocol[GraphMaster.MAX_INPUTS];
	public static final InputProtocol[] typedPatterns = new InputProtocol[GraphMaster.MAX_INPUTS];
	public static final InputProtocol[] queryPatterns = new InputProtocol[GraphMaster.MAX_INPUTS];
	public static final Cache[] peerPatterns = new Cache[GraphMaster.MAX_INPUTS];

	public static final int MAX_INPUTS = 100000;
	public static int regexInputsCursor = 0;
	public static int typedInputsCursor = 0;
	public static int queryInputsCursor = 0;
	public static int peerInputsCursor = 0;

	public static int getRegexInputsCursor() {
		return GraphMaster.regexInputsCursor;
	}

	public static void setRegexInputsCursor(int inputsCursor) {
		GraphMaster.regexInputsCursor = inputsCursor;
	}

	public static void incrementRegexInputsCursor() {
		GraphMaster.regexInputsCursor++;
	}

	public static void incrementTypedInputsCursor() {
		GraphMaster.typedInputsCursor++;
	}

	public static void incrementQueryInputsCursor() {
		GraphMaster.queryInputsCursor++;
	}

	public static void incrementPeerInputsCursor() {
		GraphMaster.peerInputsCursor++;
	}

	public static int getTypedInputsCursor() {
		return GraphMaster.typedInputsCursor;
	}

	public static void setTypedInputsCursor(int typedInputsCursor) {
		GraphMaster.typedInputsCursor = typedInputsCursor;
	}

	public static int getQueryInputsCursor() {
		return GraphMaster.queryInputsCursor;
	}

	public static void setQueryInputsCursor(int queryInputsCursor) {
		GraphMaster.queryInputsCursor = queryInputsCursor;
	}

	public static InputProtocol[] getTypedPatterns() {
		return GraphMaster.typedPatterns;
	}

	public static InputProtocol[] getQueryPatterns() {
		return GraphMaster.queryPatterns;
	}

	public static InputProtocol[] getRegexPatterns() {
		return GraphMaster.regexPatterns;
	}

	public static Cache[] getPeerPatterns() {
		return GraphMaster.peerPatterns;
	}

}
