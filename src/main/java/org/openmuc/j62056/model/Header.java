/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.model;

/**
 *
 * @author sascha
 */
public class Header {

    private String identifier;
    private String manufacturesId;
    private byte baudRateByte;
    private BaudRate baudrate;

    public Header() {
    }

    public Header(String identifier, String manufacturesId, byte baudRateByte, BaudRate baudrate) {
	this.identifier = identifier;
	this.manufacturesId = manufacturesId;
	this.baudRateByte = baudRateByte;
	this.baudrate = baudrate;
    }

    public String getIdentifier() {
	return identifier;
    }

    public void setIdentifier(String identifier) {
	this.identifier = identifier;
    }

    public String getManufacturesId() {
	return manufacturesId;
    }

    public void setManufacturesId(String manufacturesId) {
	this.manufacturesId = manufacturesId;
    }

    public byte getBaudRateByte() {
	return baudRateByte;
    }

    public void setBaudRateByte(byte baudRateByte) {
	this.baudRateByte = baudRateByte;
    }

    public BaudRate getBaudrate() {
	return baudrate;
    }

    public void setBaudrate(BaudRate baudrate) {
	this.baudrate = baudrate;
    }

    @Override
    public String toString() {
	return "Header{" + "identifier=" + identifier + ", manufacturesId=" + manufacturesId + ", baudRateByte=" + baudRateByte + ", baudrate=" + baudrate + '}';
    }

}
