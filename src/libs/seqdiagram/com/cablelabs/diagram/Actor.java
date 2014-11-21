package com.cablelabs.diagram;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

public class Actor {

    private List<String> interfaces;
    private String name;
    private Image image;

    public Actor(String name) {
        this.name = name;
    }

    public void addInterface(String iface) {
        if (interfaces == null)
            interfaces = new ArrayList<String>();

        if (!interfaces.contains(iface))
            interfaces.add(iface);
    }

    /**
     * @return the interfaces
     */
    public List<String> getInterfaces() {
        return interfaces;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }


    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image= image;
    }
    
    public boolean isPlatform() {
        return name.equals(SDTCanvas.PLATFORM);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
        
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Actor) {
            Actor a = (Actor)o;
            if (a != null) {
                return name.equals(a.name);
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
