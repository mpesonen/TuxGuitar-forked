package org.herac.tuxguitar.io.gpx.score;

public class GPXAutomation {
	
	private String type;
	private int barId;
	private float position;
	private boolean linear;
	private boolean visible;
	private float[] value;
	
	public GPXAutomation(){
		super();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getBarId() {
		return barId;
	}

	public void setBarId(int barId) {
		this.barId = barId;
	}

	public float getPosition() {
		return position;
	}

	public void setPosition(float position) {
		this.position = position;
	}

	public boolean isLinear() {
		return linear;
	}

	public void setLinear(boolean linear) {
		this.linear = linear;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public float[] getValue() {
		return value;
	}

	public void setValue(float[] value) {
		this.value = value;
	}
}
