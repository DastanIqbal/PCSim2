package com.cablelabs.gui.scripts;

import com.cablelabs.parser.TSDocument;


public interface PC2ScriptVisualizerI {

    public TSDocument getDocument();
    public void setDocument(TSDocument doc);

    public boolean isShouldExitOnClose();
    public void setShouldExitOnClose(boolean shouldExitOnClose);
    
    public void show();
}
