package Server.Database;
public class User {
	private String username;
	private double score;
	private Account account;

	public User(String username, String password) {
		this.username = username;
		this.score = 14;
		this.account = new Account(username, password);
	}

	public Account getAccount() {
		return account;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", score=" + score + ", account=" + account + "]";
	}

	public String toString2() {
		return "[username=" + username + ", score=" + score + "]";
	}
	public String toString3() { return "[username=" + username + ", score=" + score + "]" +"!";
	}


	public double getScore() {
	return this.score;
}
	public void setScore(double newScore) { this.score = newScore; }
	public String getUsername() {
		return username;
	}
}
