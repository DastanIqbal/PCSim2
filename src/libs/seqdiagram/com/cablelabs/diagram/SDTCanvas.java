package com.cablelabs.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import com.cablelabs.log.LogAPI;
import com.cablelabs.log.LogCategory;

public class SDTCanvas extends JPanel implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;
    
    private static final LogAPI logger = LogAPI.getInstance();
    private static final String subCat = "SDTCanvas";

    public static final Color BLUE = Color.BLUE;
    public static final Color GREEN = new Color(0, 128, 0); // Color.GREEN;
    public static final Color ORANGE = new Color(255, 128, 0); // Color.ORANGE;
    
    protected static final String PLATFORM = "PCSim2";
   
    protected static int HORIZONTAL_COMPUTER_OFFSET = 300;
    protected static int HORIZONTAL_MARGIN = 50;
    
    protected static int VERTICAL_LINE_START = 100; // the x value where vertical lines start
    
    protected static int EVENT_GAP = 50; // y distance between events
    
    protected static int VERTICLE_LINE = 120;

    private Image backgroundImage;

    private Image computerImage;
    private HashMap<String, Integer> ipPortXLoc = new HashMap<String, Integer>();
    private HashMap<Actor, ActorDrawer> actorXLoc = new HashMap<Actor, ActorDrawer>();
    private List<Actor> actors;
    
    private SDTDiagramPanel diagram = null;

    private TextArea messageTextArea = null;
    private Shape selectedShape;

    private HashMap<String, Shape> shapes = new HashMap<String, Shape>();
    private Shape toolTip = null;

    private int neededWidth;
    private int neededHeight;

    public SDTCanvas(SDTDiagramPanel diagram) {
        this.messageTextArea = diagram.getMessageTextArea();
        this.diagram = diagram;
        addMouseListener(this);
        addMouseMotionListener(this);


        Toolkit toolkit = Toolkit.getDefaultToolkit();
        //URL url = SeqDiagram.class.getResource("images/logoMain.gif");
        //backgroundImage = toolkit.getImage(url);
        URL url = SDTDiagramPanel.class.getResource("images/comp.gif");
        computerImage = toolkit.getImage(url);
    }



    public void clearDisplayInfo() {
        Iterator<String> e = shapes.keySet().iterator();
        boolean repaint = false;
        while (e.hasNext()) {
            String name = e.next();
            Shape s = shapes.get(name);
            if (s != null)
                if (s.displayInfo) {
                    s.displayInfo = false;
                    repaint = true;
                }
        }
        if (repaint)
            repaint();
    }

    public void displayInfo(int x, int y) {
        Shape s = getShape(x, y);
        s.displayInfo = true;
        repaint();
    }

    public void drawBackground(Graphics g) {
        // if we have a background image, fill the back with it
        // otherwise black by default

        if (backgroundImage != null
                && backgroundImage.getWidth(this) != -1
                && backgroundImage.getHeight(this) != -1) {
            int widthImage = backgroundImage.getWidth(this);
            int heightImage = backgroundImage.getHeight(this);

            int nbImagesX = getSize().width / widthImage + 1;
            int nbImagesY = getSize().height / heightImage + 1;

            // we don't draw the image above the top

            for (int i = 0; i < nbImagesX; i++)
                for (int j = 0; j < nbImagesY; j++)
                    g.drawImage(
                            backgroundImage,
                            i * widthImage,
                            j * heightImage + 95,
                            this);
        } else {

            g.setColor(Color.white);
            g.fillRect(0, 95, getSize().width, getSize().height);
        }

    }



    public void drawShapes(Graphics g) {
        Iterator<String> e = shapes.keySet().iterator();
        while (e.hasNext()) {
            String name = e.next();
            Shape s = shapes.get(name);
            if (s instanceof Seperator){
                ((Seperator)s).setXMax(Math.max(neededWidth,this.getSize().width));
            }
            
            if (s.visible) {
                s.draw(g);
            }
        }

        if (getParent() != null) {
            //getParent().doLayout();
            getParent().validate();
        }
    }

    public void drawTop(Graphics g) {
        int canvasWidth = getSize().width;
        int canvasHeight = getSize().height;

        g.setColor(new Color(0, 0, 125));
        g.fillRect(0, 0, canvasWidth, VERTICLE_LINE - 5);

        // Draw the String using the current Font and color
        g.setColor(Color.white);
        // display the label
        Font f = g.getFont();
        Font newFont = new Font(f.getName(), Font.BOLD /*| Font.ITALIC*/, 17);
        g.setFont(newFont);

        // String, x,y
        if (diagram != null && diagram.getDisplayedFileName() != null) {
            String s = "Trace retrieved from " + diagram.getDisplayedFileName();
            g.drawString(s, 40, 25);
            if (neededWidth < g.getFontMetrics().stringWidth(s) + 60){
                neededWidth = g.getFontMetrics().stringWidth(s) + 60;
            }
        }

        // draw a separation line:
        g.setColor(Color.black);
        // x,y -> x,y
        g.drawLine(0, VERTICLE_LINE, canvasWidth, VERTICLE_LINE);

        // draw the actors above the separation line and their vertical line:
        Iterator<ActorDrawer> iter = actorXLoc.values().iterator();
        while (iter.hasNext()) {
            
            ActorDrawer ad = iter.next();
            ad.draw(g, canvasWidth, canvasHeight);     
        }
    }


    public Shape getShape(int x, int y) {
        Iterator<String> e = shapes.keySet().iterator();
        while (e.hasNext()) {
            String name = e.next();
            Shape s = shapes.get(name);
            if (s == null)
                return null;
            if (s.isCursorInShape(x, y))
                return s;
        }
        return null;
    }

    public Shape getShapeInfo(int x, int y) {
        //logger.debug(LogCategory.APPLICATION, subCat,  "getArrow: x:"+x+" y:"+y);
        Iterator<String> e = shapes.keySet().iterator();
        while (e.hasNext()) {
            String name = e.next();
            Shape s = shapes.get(name);
            if (s == null)
                return null;
            else if (s.statusInfo) {
                if (s.isCursorInInfo(x, y))
                    return s;
            }
        }
        return null;
    }

    public void hideShapes() {
        Iterator<String> e = shapes.keySet().iterator();
        while (e.hasNext()) {
            String name = e.next();
            Shape s = shapes.get(name);
            s.visible = false;
        }
    }

    public boolean isOnInfo(int x, int y) {
        Shape s = getShapeInfo(x, y);
        if (s == null)
            return false;
        else
            return true;
    }

    public boolean isOnShape(int x, int y) {
        Shape s = getShape(x, y);
        if (s == null)
            return false;
        else
            return true;
    }

    @Override
    public void mouseClicked(MouseEvent p1) {
    }

    @Override
    public void mouseDragged(MouseEvent p1) {
    }

    @Override
    public void mouseEntered(MouseEvent p1) {
    }

    @Override
    public void mouseExited(MouseEvent p1) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        if (isOnShape(x, y)) {
            showToolTip(x, y);
        } else
            unShowToolTip();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // logger.debug(LogCategory.APPLICATION, subCat,  "Mouse pressed in the canvas!!!");
        try {
            int x = e.getX();
            int y = e.getY();

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK)
                    == InputEvent.BUTTON3_MASK) {
                // The right button is pressed

                if (isOnShape(x, y)) {

                    toolTip.displayToolTip = false;
                    toolTip = null;
                    displayInfo(x, y);
                }
            } else {
                // The left button is pressed:
                if (isOnShape(x, y)) {
                    //logger.debug(LogCategory.APPLICATION, subCat,  "click on Arrow!!!");
                    selectEvent(x, y);
                }

            }
        } catch (Exception ex) {

        }
    }
    @Override
    public void mouseReleased(MouseEvent p1) {
        clearDisplayInfo();
    }


    @Override
    public void paintComponent(Graphics g) {
        //OFI: only call refresh when needed.

        try {
            // Paint is called each time:
            //  - resize of the window
            // logger.debug(LogCategory.APPLICATION, subCat,  );
            // logger.debug(LogCategory.APPLICATION, subCat,  "paint method called");
            // We draw the title and some decorations:
            drawBackground(g);
            drawTop(g);
            drawShapes(g);

        }
        catch (Exception e) {
            logger.error(SDTLogCat.APPLICATION, subCat, "Caught exception while painting: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

//    private void refreshFromMain() {
//        if (diagram.getDisplayedFileName() == null) return;
//        logger.trace(SDTLogCat.APPLICATION, subCat, "refreshing canvas.");
//        //this.events = events;
//        this.ipPortXLoc.clear();
//        this.shapes.clear();
//
//        calculateComputers();
//        calculateShapes();
//
//        this.setPreferredSize(new Dimension(neededWidth, neededHeight));
//        super.repaint();
//        super.revalidate();
//        logger.trace(SDTLogCat.APPLICATION, subCat, "neededWidth,neededHeight = (" + neededWidth + ", " + neededHeight + ")");
//    }

    public void refresh() {
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                refreshFromMain();
//            }
//        });
        
        if (diagram.getDisplayedFileName() == null) return;
        logger.trace(SDTLogCat.APPLICATION, subCat, "refreshing canvas.");
        //this.events = events;
        this.ipPortXLoc.clear();
        this.shapes.clear();

        calculateActors();
        calculateShapes();

        this.setPreferredSize(new Dimension(neededWidth, neededHeight));
        super.repaint();
        super.revalidate();
        logger.trace(SDTLogCat.APPLICATION, subCat, "neededWidth,neededHeight = (" + neededWidth + ", " + neededHeight + ")");
    }

    public void selectEvent(Event e) {
        String name = shapeKey(e);
        Shape s = shapes.get(name);
        if (s != null) {
            if (messageTextArea != null) {
                messageTextArea.setText(e.getMessage());
            }

            // We have to change the color of the old message and the new one:
            if (selectedShape != null) {
                selectedShape.selected = false;
            }
            s.selected = true;
            selectedShape = s;
            repaint();
            logger.debug(LogCategory.APPLICATION, subCat,  "Selected " + name);
        }
    }

    public void selectEvent(int x, int y) {
        // x,y: position of the cursor
        Shape newShape = getShape(x, y);
        if (newShape != null) {
            Event e = newShape.getEvent();
            if (messageTextArea != null) {
                messageTextArea.setText(e.getMessage());
            }

            // We have to change the color of the old message and the new one:
            if (selectedShape != null) {
                selectedShape.selected = false;
            }
            newShape.selected = true;
            selectedShape = newShape;
            repaint();
            logger.debug(LogCategory.APPLICATION, subCat,  "Selected event " + e.getSequence());
        }
    }

    public void showToolTip(int x, int y) {
        Shape oldToolTip = toolTip;
        toolTip = getShape(x, y);
        if (oldToolTip != null) {
            if (!oldToolTip.name.equals(toolTip.name)) {
                oldToolTip.displayToolTip = false;
                toolTip.displayToolTip = true;
                repaint();
            }
        } else {
            toolTip.displayToolTip = true;
            repaint();
        }
    }

    public void unselectAllShapes() {
        Iterator<String> e = shapes.keySet().iterator();
        while (e.hasNext()) {
            String name = e.next();
            Shape s = shapes.get(name);
            s.selected = false;
        }
    }

    public void unShowToolTip() {
        if (toolTip != null) {
            toolTip.displayToolTip = false;
            repaint();
            toolTip = null;
        }
    }

    private void calculateActors() {
        if (diagram == null) return;
        actorXLoc.clear();

        HashMap<String,Actor> comps = new HashMap<String,Actor>();

        ArrayList<Actor> newActors = new ArrayList<Actor>();
        
        Iterator<Event> iter = diagram.getSelectionEventIter();
        while (iter.hasNext()) {
            Event e = iter.next();
            String to = e.getTo();
            
            
            Actor toA = e.getParentConfig().getActor(to);
            if (toA == null) {
                // unknown actor make anew one with the ip as the name
                ArrayList<String> ips = new ArrayList<String>(1);
                ips.add(to);
                e.getParentConfig().updateNE(to, ips);
                toA = e.getParentConfig().getActor(to);
                if (toA == null) {
                    logger.error(SDTLogCat.APPLICATION, subCat, "Unable to create Actor for ip(" + to + ")");
                    continue;
                } else {
                    logger.trace(SDTLogCat.APPLICATION, subCat, "Creating new Actor for ip(" + to + ") with unknown NE");
                }
                
            }
            String from = e.getFrom();
            Actor fromA = e.getParentConfig().getActor(from);
            if (fromA == null) {
                // unknown actor make anew one with the ip as the name
                ArrayList<String> ips = new ArrayList<String>(1);
                ips.add(from);
                e.getParentConfig().updateNE(from, ips);
                fromA = e.getParentConfig().getActor(from);
                if (fromA == null) {
                    logger.error(SDTLogCat.APPLICATION, subCat, "Unable to create Actor for ip(" + from + ")");
                    continue;
                } else {
                    logger.trace(SDTLogCat.APPLICATION, subCat, "Creating new Actor for ip(" + from + ") with unknown NE");
                }
            }
            
            
            if (!newActors.contains(toA)) {
                if (toA.isPlatform()) {
                    newActors.add(0, toA);
                } else {
                    newActors.add(toA);
                }
            }
            if (!newActors.contains(fromA)) {
                if (fromA.isPlatform()) {
                    newActors.add(0, fromA);
                } else {
                    newActors.add(fromA);
                }
            }

            comps.put(from, fromA);
            comps.put(to, toA);
        }

        actors = newActors;
        synchronized(ipPortXLoc) {
            ipPortXLoc.clear();
            
            int x = VERTICAL_LINE_START;
            for (int i = 0; i < actors.size(); i++) {
                Actor actor = actors.get(i);
                List<String> interfaces = actor.getInterfaces();
                int interFaceW = 60;
                
                int startX = x;
                for (int j = 0; j < interfaces.size(); j++) {
                    String ip = interfaces.get(j);
                    if (comps.containsKey(ip)) {
                       ipPortXLoc.put(ip, x);
                       x += interFaceW;
                    }
                }
                x -= interFaceW; // subtract off the last gap, not all interfaces are included
                
                ActorDrawer ad = new ActorDrawer(actor, startX, x);
                
                actorXLoc.put(actor, ad);
                
                
                x += HORIZONTAL_COMPUTER_OFFSET;
            }
            
            
//            addComputer(PLATFORM);
//            Enumeration<String> elements = comps.keys();
//            while(elements.hasMoreElements()) {
//                addComputer(elements.nextElement());
//            }
        }
        
        
        logger.debug(LogCategory.APPLICATION, subCat,  "Calculated " + ipPortXLoc.size() + " computers.");
    }

    private void calculateShapes() {
        if (diagram == null) return;

        neededHeight = SDTCanvas.VERTICLE_LINE;

        try {
            synchronized(ipPortXLoc) {
                int count = 0;
                Configuration lastConfig = null;
                Test lastTest = null;
                Iterator<Event> iter = diagram.getSelectionEventIter();
                int positionY = VERTICLE_LINE + HORIZONTAL_MARGIN - EVENT_GAP;
                while (iter.hasNext()) {
                    count++;
                    Event e = iter.next();
                    positionY += EVENT_GAP;
                     
                    if (lastConfig != e.getParentConfig() || lastTest != e.getTest()) {
                        if (count == 1) {
                            positionY -= EVENT_GAP * 0.8f;
                        }
                        lastConfig = e.getParentConfig();
                        lastTest = e.getTest();
                        String sepName = "DUT: " + lastConfig.getName() + "     Test: " + lastTest.getName();
                        Seperator sep = new Seperator(sepName, 0, neededWidth, positionY-20, positionY+20);
                        shapes.put("SEP: " + sep.name + positionY, sep);
                        positionY += EVENT_GAP;
                    }
                    
                    int positionXFrom = 0;
                    int positionXTo = 0;
                    Color color = Color.BLACK;
                    String to = e.getTo();
                    String from = e.getFrom();
                    if (!ipPortXLoc.containsKey(to)) {
                        logger.error(SDTLogCat.APPLICATION, subCat, "Unknown location for ip: " + to);
                        continue;
                    }
                    if (!ipPortXLoc.containsKey(from)) {
                        logger.error(SDTLogCat.APPLICATION, subCat, "Unknown location for ip: " + from);
                        continue;
                    }
                    positionXTo = ipPortXLoc.get(to);
                    positionXFrom = ipPortXLoc.get(from);
                    if (e.getParentConfig().isPlatformAddress(from)) {
                        color = ORANGE;
                    }
                    else if (e.getParentConfig().isPlatformAddress(to)) {
                       
                        color = GREEN;
                    }
//                    else {
//                        positionXTo = ipPortXLoc.get(to);
//                        positionXFrom = ipPortXLoc.get(from);
//                    }
                    
                    neededHeight = positionY + EVENT_GAP;

                    String name = shapeKey(e);
                    boolean selected = false;
                    if (selectedShape != null && selectedShape.name.equals(name)) {
                        selected = true;
                    }

                    boolean info = false;
                    Shape s = null;
                    if (positionXFrom == positionXTo) {
                        // This is an internal event
                        Circle circle = new Circle(selected, name,  positionXFrom,
                                positionY - 20, positionY + 20, 40, true, info);
                        circle.setColor(color);
                        circle.setEvent(e);


                        shapes.put(name, circle);
                        s= circle;
                    }
                    else {
                        Arrow arrow = new Arrow(selected, name, positionXFrom, positionXTo,
                                positionY - 31, positionY + 31, true, info);

                        arrow.setColor(color);
                        arrow.setEvent(e);
                        shapes.put(name, arrow);
                        s = arrow;
                    }

                    if (selected) {
                        selectedShape = s;
                    }

                }
            }


        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String shapeKey(Event e) {
        String key = e.getParentConfig().getName() 
                + "|" + e.getTimeStampStr()
                + "|" + e.getSequence();
        return key;
    }
    
    private void updateNeededWidth(int width) {
        if (neededWidth < width) {
            neededWidth = width;
            this.setPreferredSize(new Dimension(neededWidth, neededHeight));
            super.repaint();
            super.revalidate();
            logger.trace(SDTLogCat.APPLICATION, subCat, "neededWidth,neededHeight = (" + neededWidth + ", " + neededHeight + ")");
        }
    }
    
    private class ActorDrawer implements ImageObserver {
        private static final int MIN_WIDTH = 100;
        private Font nameFont;
        private Font ipFont;
        
        Actor actor;
        int left, top, width, height;   
        
        boolean checkSizes = true;
        
        public ActorDrawer(Actor actor, int left, int right) {
            this.actor = actor;
            this.left = left;
            this.width = right - left;
            this.top = VERTICLE_LINE/3;
            this.height = VERTICLE_LINE - 10 - top;
        }
        
        public void draw(Graphics g, int canvasWidth, int canvasHeight) {
            if (checkSizes) updateSizes(g);

            drawNames(g); // this must be before drawRect because the width is increased if needed
            drawRect((Graphics2D)g);
            drawVerticalLines(g, canvasWidth, canvasHeight);
        }

        

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            // don't care if things change
            return false;
        }
        
        private void drawRect(Graphics2D g) {
            Stroke origStroke = g.getStroke();
            
            
            g.setColor(Color.LIGHT_GRAY);

            //final float dash[] = { 15f, 5f, 4f, 5f };
            final BasicStroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                            10.0f, null, 0.0f);
            g.setStroke(dashed);
            g.drawRoundRect(left, top, width, height, 12, 12);
            
            
            g.setStroke(origStroke);
        }
        
        private void drawNames(Graphics g) {
            int centerX = left + width/2;
            
            g.setFont(nameFont);
            FontMetrics metrics = g.getFontMetrics();
            
            // Draw the name of the computer and its address below its icon:
            // String, x,y
            g.setColor(Color.white);
            
            int iterfaceYLow = VERTICLE_LINE - 15;
            int iterfaceYHigh = iterfaceYLow - 10;
            int nameY = iterfaceYHigh - 15;
            drawCenteredString(g, actor.getName(), centerX, nameY);

            if (actor.getImage() != null) {
                g.drawImage(actor.getImage(),
                        centerX - actor.getImage().getWidth(this) / 2,
                        top + 5,
                        this);
            }
            
            g.setFont(ipFont);
            metrics = g.getFontMetrics();
            
            List<String> interfaces = actor.getInterfaces();
            int y = iterfaceYHigh;
            for (int j = 0; j < interfaces.size(); j++) {
                String ip = interfaces.get(j);
                if (ipPortXLoc.containsKey(ip)) {
                    if (y == iterfaceYLow) {
                        y = iterfaceYHigh;
                    } else {
                        y = iterfaceYLow;
                    }
                    
                    int strW = metrics.stringWidth(ip);
                    int x = ipPortXLoc.get(ip) - strW/2;                  
                    g.drawString(ip, x, y);
                }
            }
            
        }
        
        private void drawVerticalLines(Graphics g, int canvasWidth, int canvasHeight) {
            // Vertical lines
            g.setColor(Color.black);
            
            for (String ip : actor.getInterfaces()) {
                if (!ipPortXLoc.containsKey(ip)) continue;
                int x = ipPortXLoc.get(ip);
                
                g.drawLine(x, VERTICLE_LINE, x, canvasHeight);
            }   
        }
        
        private void drawCenteredString(Graphics g, String s, int x, int y) {
            FontMetrics metrics = g.getFontMetrics();
            int strW = metrics.stringWidth(s);
            g.drawString(s, x - strW/2, y);
            
        }
        
        private void updateSizes(Graphics g) {
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // ensure a minimum width
            if (width < MIN_WIDTH) {
                int more = (MIN_WIDTH - width)/2;
                this.left -= more;
                this.width = MIN_WIDTH;
            }

            // Check if the name of the Actor fits in the box
            Font f = g.getFont();
            nameFont = new Font(f.getName(), Font.BOLD, 14);
            g.setFont(nameFont);
            FontMetrics metrics = g.getFontMetrics();
            int nameW = metrics.stringWidth(actor.getName());
            
            if (width - 18 < nameW) {
                int newW = (nameW + 20);
                int more = (newW - width)/2;
                this.left -= more;
                this.width = newW;
            }
            
            // check if the ips of the interface fit within the box
            ipFont = new Font(f.getName(), 0, 9);
            g.setFont(ipFont);
            metrics = g.getFontMetrics();
            List<String> interfaces = actor.getInterfaces();
            for (int i = 0; i < interfaces.size(); i++) {
                String ip = interfaces.get(i);
                if (ipPortXLoc.containsKey(ip)) {
                    int strW = metrics.stringWidth(ip);
                    int x = ipPortXLoc.get(ip) - strW/2;
                    if  (x < left) {
                        int diff = x - left;
                        width += diff *2;
                        left = x - 5;
                    }
                    if (x + strW + 5 > left + width) {
                        int diff = (x + strW + 5) - (left + width);
                        width += diff *2;
                        left -= diff;
                    }                  
                }
            }
            
            
            updateNeededWidth(left+width);
            checkSizes = false;
        }
        
    }
    
}
