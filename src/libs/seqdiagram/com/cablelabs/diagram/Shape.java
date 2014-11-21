package com.cablelabs.diagram;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

public abstract class Shape {

	protected Canvas canvas = null;
	protected Color color;
	protected boolean displayInfo = false;
	protected boolean displayToolTip = false;
	protected Event event = null;

	protected String name;

	protected boolean selected;
	protected boolean statusInfo = false;
	protected boolean visible = true;
	protected int xmax;

	protected int xmaxInfo;
	protected int xmin;
	protected int xminInfo;

	protected int ymax;
	protected int ymaxInfo;
	protected int ymin;
	protected int yminInfo;

	public Shape(boolean selected, String name, boolean visible,
			int xmin, int xmax, int ymin, int ymax, boolean info) {
			if (name == null || name.length() == 0) {
				throw new IllegalArgumentException("name can not be null or empty");
			}
			this.name = name;
			this.xmin = xmin;
			this.xmax = xmax;
			this.ymin = ymin;
			this.ymax = ymax;
			this.selected = selected;
			this.visible = visible;
			statusInfo = info;
		}
	public Shape(boolean selected, String name, boolean visible,
			int xmin, int xmax, int ymin, int ymax, boolean info, Event event) {
			this.name = name;
			this.xmin = xmin;
			this.xmax = xmax;
			this.ymin = ymin;
			this.ymax = ymax;
			this.selected = selected;
			this.event = event;
			this.visible = visible;
			statusInfo = info;

		}
	public Event getEvent() {
		return event;
	}
	public boolean isCursorInInfo(int x, int y) {
		// Return true if the cursor is inside the rectangle of the info
		if (x < xmaxInfo && x > xminInfo)
			if (y < ymaxInfo && y > yminInfo)
				return true;

		return false;
	}

	public boolean isCursorInShape(int x, int y) {
		// Return true if the cursor is inside the rectangle of
		// the shape:
		if (xmin <= xmax) {
			if (x < xmax && x > xmin)
				if (y < ymax && y > ymin)
					return true;
		}
		else if (x < xmin && x > xmax)
				if (y < ymax && y > ymin)
					return true;
		return false;
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	protected abstract void draw(Graphics g);

	protected abstract int xmax();

	protected abstract int xmin();

	protected abstract int ymax();

	protected abstract int ymin();
}
