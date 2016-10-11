package ai.vacuity.rudi.adaptors.bo;

import java.util.regex.Pattern;

import rice.pastry.NodeHandle;

public class Cache {

	private Pattern pattern;
	private double patternScore;
	private NodeHandle peer;

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public NodeHandle getPeer() {
		return peer;
	}

	public void setPeer(NodeHandle peer) {
		this.peer = peer;
	}

	public double getPatternScore() {
		return patternScore;
	}

	public void setPatternScore(double patternScore) {
		this.patternScore = patternScore;
	}

}
