
package com.cablelabs.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public class Arrow extends Shape {

    public Dimension dimensionInfo;

    public Arrow(boolean selected, String name, int xmin, int xmax, int ymin,
            int ymax, boolean visible, boolean info) {
        super(selected, name, visible, xmin, xmax, ymin, ymax, info);
    }

    @Override
    public void draw(Graphics g) {
        // Set the color of this arrow:
        if (selected) {
            g.setColor(Color.red);
        }
        else {
            g.setColor(color);
        }

        // Setup the font
        Font font = g.getFont();
        Font newFont = new Font(font.getName(), Font.BOLD, 12);
        g.setFont(newFont);

        // calc y and leftToRight
        int y = (ymin + ymax) / 2;
        boolean leftToRight = (xmin < xmax);

        //// Draw an arrow head and a line
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BasicStroke stroke = new BasicStroke( 2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
        g2.setStroke(stroke);

        float arrowRatio = 0.5f;
        float arrowLength = Math.max(5f, 4f *stroke.getLineWidth());

        // Calculate the location of the arrow head with the current strokes join setting(JOIN_MITTER)
        float headX;
        if (leftToRight) {
            headX = xmax - stroke.getLineWidth() * 0.5f / arrowRatio - stroke.getLineWidth() * 0.25f;
        }
        else {
            headX = xmax + stroke.getLineWidth() * 0.5f / arrowRatio + stroke.getLineWidth() * 0.25f;
            arrowLength = -arrowLength;
        }

        // arrow head
        Path2D.Float path = new Path2D.Float();

        path.moveTo(headX - arrowLength, -arrowRatio*arrowLength);
        path.lineTo(headX, 0.0f );
        path.lineTo(headX - arrowLength, arrowRatio*arrowLength);

        g2.translate(0, y);

        //g2.setColor (Color.BLUE);
        g2.draw(path);

        //g2.setColor (Color.RED);
        g2.draw(new Line2D.Float(xmin, 0.0f, headX - stroke.getLineWidth() * 0.25f, 0.0f));

        // Reset the translate for any future drawing to g2
        g2.translate(0,-y);
        //// Done with arrow

        //// Draw the label near the arrow origination point (xmin,y)
        String eventStr = event.getFirstLine();
        String timeStr = "Time : " + event.getTimeStampStr();

        FontMetrics metrics = g.getFontMetrics();
        int eventStrWidth = metrics.stringWidth(eventStr);
        int timeStrWidth = metrics.stringWidth(timeStr);

        if (leftToRight) {
            g.drawString(eventStr, xmin() + 5, y - 5);
            g.drawString(timeStr, xmin() + 5, y + metrics.getAscent() + 2);
        }
        else {
            g.drawString(eventStr, xmin - eventStrWidth - 5, y - 5);
            g.drawString(timeStr, xmin - timeStrWidth - 5, y + metrics.getAscent() + 2);
        }
        
//        if (this.displayToolTip) {
//            
//        }
    }

    @Override
    public int xmax() {
        return Math.max(xmin, xmax);
    }

    @Override
    public int xmin() {
        return Math.min(xmin, xmax);
    }

    @Override
    public int ymax() {
        return Math.max(ymin, ymax);
    }

    @Override
    public int ymin() {
        return Math.min(ymin, ymax);
    }

}
