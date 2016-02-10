/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056;

import java.io.IOException;

/**
 *
 * @author bilke
 */
public class MessageNotCompleteException extends IOException {

    private final int bytesRead;
    private final byte[] bytes;

    public MessageNotCompleteException(int bytesRead, byte[] bytes, String message) {
	super(message);
	this.bytesRead = bytesRead;
	this.bytes = bytes;
    }

    public MessageNotCompleteException(int bytesRead, byte[] bytes, String message, Throwable cause) {
	super(message, cause);
	this.bytesRead = bytesRead;
	this.bytes = bytes;
    }

    public int getBytesRead() {
	return bytesRead;
    }

    public byte[] getBytes() {
	return bytes;
    }

}
