/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.config;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author sascha
 */
public class ParityTest {

    @Test
    public void testConvertValueString() {
	String s = "odd";
	Parity expResult = Parity.ODD;
	Parity result = Parity.convert(s);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertValueInt() {
	String s = "3";
	Parity expResult = Parity.MARK;
	Parity result = Parity.convert(s);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertInvalid() {
	String s = "asjdf";
	Parity result = Parity.convert(s);
	Assert.assertNull(result);
    }

    @Test
    public void testConvertEmpty() {
	String s = "";
	Parity result = Parity.convert(s);
	Assert.assertNull(result);
    }

    @Test
    public void testConvertNull() {
	String s = null;
	Parity result = Parity.convert(s);
	Assert.assertNull(result);
    }

}
