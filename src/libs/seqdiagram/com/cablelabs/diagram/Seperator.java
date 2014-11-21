package com.cablelabs.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;


public class Seperator extends Shape {

    public Seperator(String label, int xmin, int xmax, int ymin,
            int ymax) {
        super(false, label, true, xmin, xmax, ymin, ymax, false);
    }

    @Override
    protected void draw(Graphics g) {
       
        //TODO draw the label, increase stroke of line
        
        g.setColor(Color.blue);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BasicStroke stroke = new BasicStroke( 2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER );
        g2.setStroke(stroke);
        
        int y = ymin() + (ymax() - ymin())/2;
        g.drawLine(xmin, y, xmax, y);
        
        Font font = g.getFont();
        Font newFont = new Font(font.getName(), Font.BOLD, 14);
        g.setFont(newFont);
        
        g.drawString(name, xmin + 5, y+g.getFontMetrics().getHeight());
    }

    @Override
    protected int xmax() {
        return Math.max(xmin, xmax);
    }

    @Override
    protected int xmin() {
        return Math.min(xmin, xmax);
    }

    @Override
    protected int ymax() {
        return Math.max(ymin, ymax);
    }

    @Override
    protected int ymin() {
        return Math.min(ymin, ymax);
    }
    
    public void setXMax(int xmax) {
        this.xmax = xmax;
    }

}
