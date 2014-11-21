package com.cablelabs.diagram;

import com.cablelabs.log.LogCategory;


public class SDTLogCat extends LogCategory {

    public SDTLogCat() {
        super();
    }
    
    public SDTLogCat(String name) {
        super(name);
    }
    
    @Override
    public String getApplicationName() {
        return "SDT";
    }
}
