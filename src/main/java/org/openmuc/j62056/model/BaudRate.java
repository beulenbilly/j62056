/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.model;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author sascha
 */
public enum BaudRate {

    Baud_300(300, Byte.valueOf((byte) 0x30)),
    Baud_600(9600, Byte.valueOf((byte) 0x31)),
    Baud_1200(9600, Byte.valueOf((byte) 0x32)),
    Baud_2400(9600, Byte.valueOf((byte) 0x33)),
    Baud_4800(9600, Byte.valueOf((byte) 0x34)),
    Baud_9600(9600, Byte.valueOf((byte) 0x35)),
    Baud_19200(19200, Byte.valueOf((byte) 0x36));

    public final int baudRate;
    private final List<Byte> encodingsBytes;

    private BaudRate(int baudRate, Byte... encodingsBytes) {
	this.baudRate = baudRate;
	this.encodingsBytes = Arrays.asList(encodingsBytes);
    }

    public static BaudRate convert(byte encodingByte) {
	BaudRate result = null;
	for (BaudRate rate : BaudRate.values()) {
	    if (rate.encodingsBytes.contains(encodingByte)) {
		result = rate;
		break;
	    }
	}
	return result;
    }
}
