/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author sascha
 */
public class BaudRateTest {

    public BaudRateTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testConvertCZero() {
	byte encodingByte = (byte) 0x0;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertNull(result);
    }

    @Test
    public void testConvertC19200() {
	byte encodingByte = (byte) 0x36;
	BaudRate expResult = BaudRate.Baud_19200;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertC9600() {
	byte encodingByte = (byte) 0x35;
	BaudRate expResult = BaudRate.Baud_9600;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertC4800() {
	byte encodingByte = (byte) 0x34;
	BaudRate expResult = BaudRate.Baud_4800;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertC2400() {
	byte encodingByte = (byte) 0x33;
	BaudRate expResult = BaudRate.Baud_2400;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertC1200() {
	byte encodingByte = (byte) 0x32;
	BaudRate expResult = BaudRate.Baud_1200;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertC600() {
	byte encodingByte = (byte) 0x31;
	BaudRate expResult = BaudRate.Baud_600;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

    @Test
    public void testConvertC300() {
	byte encodingByte = (byte) 0x30;
	BaudRate expResult = BaudRate.Baud_300;
	BaudRate result = BaudRate.convert(encodingByte);
	Assert.assertEquals(expResult, result);
    }

}
