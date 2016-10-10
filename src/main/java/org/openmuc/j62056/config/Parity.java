/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.config;

/**
 *
 * @author sascha
 */
public enum Parity {

    NONE(0), ODD(1), EVEN(2), MARK(3), SPACE(4);

    public final int value;

    Parity(int value) {
	this.value = value;
    }

    public static Parity convert(String s) {
	Parity result = null;
	if (null != s) {
	    try {
		result = valueOf(s.toUpperCase());
	    } catch (IllegalArgumentException ex) {
		try {
		    int ui = Integer.valueOf(s);
		    for (Parity p : values()) {
			if (p.value == ui) {
			    result = p;
			    break;
			}
		    }
		} catch (NumberFormatException ex2) {
		    //nop
		}
	    }
	}
	return result;
    }
}
