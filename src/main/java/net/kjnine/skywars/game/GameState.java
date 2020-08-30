package net.kjnine.skywars.game;

public enum GameState {
	
	GENERATING(0),
	WAITING_FOR_PLAYERS(1),
	STARTING(2),
	IN_GAME(3),
	GAME_OVER(4),
	DELETING(5),
	;
	
	private int id;
	GameState(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isPregame() {
		return id < IN_GAME.id;
	}
	
	public boolean isPostgame() {
		return id > IN_GAME.id;
	}
	
	public boolean isIngame() {
		return id == IN_GAME.id;
	}
	
}
