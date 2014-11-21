package com.cablelabs.diagram;

import java.awt.Color;
import java.awt.Graphics;

public class Circle extends Shape {

		protected int diameter = 0;
		protected int x = 0;
		protected int y = 0;

		public Circle(boolean selected, String name, int x, int ymin, int ymax,
			int diameter, boolean visible,	boolean info) {
			super(selected, name, visible, x, x + diameter, ymin, ymax, info);
			this.x = xmin;
			this.y = (ymin + ymax) / 2;
			this.diameter = diameter;
		}

		@Override
		public void draw(Graphics g) {
			// Set the color of this arrow:
			if (selected)
				g.setColor(Color.red);
			else
				g.setColor(color);
			/* Draw the circle */

			g.drawOval(x, y - (diameter / 2), diameter, diameter);
			g.drawOval(x - 1, y - (diameter / 2) - 1, diameter + 2, diameter + 2);

			/* Display the first line of the message */
			String timeString = "Time : " + event.getTimeStampStr();

			int timeStringWidth =
				g.getFontMetrics(g.getFont()).stringWidth(timeString);
			int fistLineStringWidth =
				g.getFontMetrics(g.getFont()).stringWidth(
					event.getFirstLine());

			g.drawString(
				event.getFirstLine(),
				x
					+ diameter
					+ 5
					+ SDTCanvas.HORIZONTAL_COMPUTER_OFFSET / 2
					- fistLineStringWidth / 2,
				y - 5);

			g.drawString(
				timeString,
				x
					+ diameter
					+ 5
					+ SDTCanvas.HORIZONTAL_COMPUTER_OFFSET / 2
					- timeStringWidth / 2,
				y + g.getFontMetrics(g.getFont()).getHeight());

			/* Draw the head of the arrow */

			g.drawLine(x, y, x - 3, y + 10);
			g.drawLine(x, y, x + 7, y + 7);

			g.drawLine(x - 1, y, x - 4, y + 10);
			g.drawLine(x + 1, y, x + 8, y + 7);

			g.drawLine(x - 2, y, x - 5, y + 10);
			g.drawLine(x + 2, y, x + 9, y + 7);
		}

		@Override
		public int xmax() {
			return x + diameter;
		}

		@Override
		public int xmin() {
			return x;
		}

		@Override
		public int ymax() {
			return ymax;
		}

		@Override
		public int ymin() {
			return ymin;
		}
}
