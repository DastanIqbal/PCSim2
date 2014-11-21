/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/

package com.cablelabs.gui.scripts.visualizer;

import java.awt.Font;

import javax.swing.UIManager;

/**
 * A Generic Transformer to make a font for everything bold.
 * Designed for use with the JUNG framework.
 *
 * @author rvail
 *
 * @param <T>
 */
public class BoldFontTransformer<T> extends ConstTransformer<T, Font> {

    BoldFontTransformer() {
        super(UIManager.getFont("TextField.font").deriveFont(Font.BOLD, UIManager.getFont("TextField.font").getSize()));
    }

}
