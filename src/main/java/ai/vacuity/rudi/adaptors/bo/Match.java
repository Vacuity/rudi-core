package ai.vacuity.rudi.adaptors.bo;

public class Match implements Comparable<Match> {
	private String pattern = null;
	private double score = 0;

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	@Override
	public int compareTo(Match m) {
		return Double.compare(this.score, m.score);
	}
}
