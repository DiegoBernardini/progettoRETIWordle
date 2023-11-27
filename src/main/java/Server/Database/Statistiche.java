package Server.Database;

import java.util.Arrays;

public class Statistiche {
	private int totPlayed;
	private int totWon;
	private int lastStreak;
	private int bestStreak;
	private String lastWordPlayed;
	private double percVittorie;
	private int[] guessDistribution;
	
	public Statistiche() {
		this.totPlayed = 0;
		this.totWon = 0;
		this.lastStreak = 0;
		this.bestStreak = 0;
		this.lastWordPlayed = "null";
		this.percVittorie = 0;
		this.guessDistribution = new int[12];
		for(int i=0; i< this.guessDistribution.length; i++)
			this.guessDistribution[i] = 0;
	}

	public String toString2() {
		return "totPlayed=" + totPlayed + ",totWon=" + totWon + ",lastStreak=" + lastStreak
				+ ",bestStreak=" + bestStreak + ",lastWordPlayed=" + lastWordPlayed + ",percVittorie=" + percVittorie
				+ ",guessDistribution=," + Arrays.toString(guessDistribution);
	}

		/**
         * @return the totPlayed
         */
	public int getTotPlayed() {
		return totPlayed;
	}

	/**
	 * @param totPlayed the totPlayed to set
	 */
	public void setTotPlayed(int totPlayed) {
		this.totPlayed = totPlayed;
	}

	/**
	 * @return the totWon
	 */
	public int getTotWon() {
		return totWon;
	}

	/**
	 * @param totWon the totWon to set
	 */
	public void setTotWon(int totWon) {
		this.totWon = totWon;
	}

	/**
	 * @return the lastStreak
	 */
	public int getLastStreak() {
		return lastStreak;
	}

	/**
	 * @param lastStreak the lastStreak to set
	 */
	public void setLastStreak(int lastStreak) {
		this.lastStreak = lastStreak;
	}

	/**
	 * @return the bestStreak
	 */
	public int getBestStreak() {
		return bestStreak;
	}

	/**
	 * @param bestStreak the bestStreak to set
	 */
	public void setBestStreak(int bestStreak) {
		this.bestStreak = bestStreak;
	}

	/**
	 * @return the lastWordPlayed
	 */
	public String getLastWordPlayed() {
		return lastWordPlayed;
	}

	/**
	 * @param lastWordPlayed the lastWordPlayed to set
	 */
	public void setLastWordPlayed(String lastWordPlayed) {
		this.lastWordPlayed = lastWordPlayed;
	}

	/**
	 * @return the percVittorie
	 */
	public double getPercVittorie() {
		return percVittorie;
	}

	/**
	 *
	 */
	public void calcolaPercVittorie(){
		this.percVittorie = (this.totWon*100) /this.totPlayed;
	}

	/**
	 * @return the guessDistribution
	 */
	public int[] getGuessDistribution() {
		return guessDistribution;
	}

	/**
	 * @param guessDistribution the guessDistribution to set
	 */
	public void setGuessDistribution(int[] guessDistribution) {
		this.guessDistribution = guessDistribution;
	}

	public void incrementPlayed () { this.totPlayed++; }
	public void incrementWon () { this.totWon++; }
	public void incrementLastStreak () { this.lastStreak++; }
	public void checkBestStreak () { this.bestStreak = Math.max(this.bestStreak, this.lastStreak);}
	public void resetLastStreak () { this.lastStreak = 0; }
	public void guessDistributionAdd (int r) {this.guessDistribution[r]++; }


}
