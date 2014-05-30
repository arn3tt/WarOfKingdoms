package br.edu.ufcg.ccc.projeto2.warofkingdoms.entities;

import br.edu.ufcg.ccc.projeto2.warofkingdoms.states.ConqueredTerritory;
import br.edu.ufcg.ccc.projeto2.warofkingdoms.states.TerritoryState;

public class Territory {

	private TerritoryState currentState = new ConqueredTerritory();
	private String name;
	private String id;
	private Player owner;

	private int centerX;
	private int centerY;

	private final int NEXUS_7_MAP_WIDTH = 886;
	private final int NEXUS_7_MAP_HEIGHT = 1774;

	public Territory(String name, int centerX, int centerY) {
		super();
		this.name = name;
		this.centerX = centerX;
		this.centerY = centerY;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Player getOwner() {
		return owner;
	}

	public void setOwner(Player owner) {
		this.owner = owner;
	}

	public int getCenterX(int minhaLargura) {
		return minhaLargura * centerX / NEXUS_7_MAP_WIDTH;
	}

	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}

	public int getCenterY(int minhaAltura) {
		return minhaAltura * centerY / NEXUS_7_MAP_HEIGHT;
	}

	public void setCenterY(int centerY) {
		this.centerY = centerY;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TerritoryState getCurrentState() {
		return currentState;
	}

	public void setCurrentState(TerritoryState currentState) {
		this.currentState = currentState;
	}

	@Override
	public String toString() {
		return name;
	}

}