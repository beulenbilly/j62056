/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.impl;

import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.MessageNotCompleteException;
import static org.openmuc.j62056.impl.AbstractConnection.MESSAGE_COMPLETION_CHARACTERS;

/**
 *
 * @author sascha
 */
public class ModeDConnection extends AbstractConnection {

    public ModeDConnection(String serialPort) {
	super(serialPort);
    }

    public ModeDConnection(String serialPort, boolean handleEcho, int baudRateChangeDelay) {
	super(serialPort, handleEcho, baudRateChangeDelay);
    }

    /**
     * Requests a data message from the remote device using IEC 62056-21 Mode C.
     * The data message received is parsed and a list of data sets is returned.
     *
     * @return A list of data sets contained in the data message response from
     * the remote device. The first data set will contain the "identification"
     * of the meter as the id and empty strings for value and unit.
     * @throws IOException if any kind of error other than timeout occurs while
     * trying to read the remote device. Note that the connection is not closed
     * when an IOException is thrown.
     * @throws TimeoutException if no response at all (not even a single byte)
     * was received from the meter within the timeout span.
     */
    public List<DataSet> read() throws IOException, TimeoutException {

	if (getSerialPort() == null) {
	    throw new IllegalStateException("Connection is not open.");
	}

	String identification = signOn(getSerialPort(), getOs(), getIs(), getTimeout(), isHandleEcho(), getBaudRateChangeDelay());

	//ignoring ETX and BCC
	byte[] dataSets = readData(getIs(), 4, MESSAGE_COMPLETION_CHARACTERS, getTimeout());
	boolean withCheckCharacter = false;
	int offset = 0;
	if (dataSets[0] == 0x02) {
	    withCheckCharacter = true;
	    offset = 1;
	}

	if (withCheckCharacter) {
	    if (dataSets.length < 8) {
		throw new IOException("Data message does not have minimum length of 8.");
	    }
	    try {
		readData(getIs(), 1, null, 1000);
	    } catch (RuntimeException ex) {
		//ignore exception
	    }
	} else if (dataSets.length < 5) {
	    throw new IOException("Data message does not have minimum length of 5.");
	}

	List<DataSet> result = new ArrayList<>();
	result.add(new DataSet(identification, "", ""));
	result.addAll(readDataSets(dataSets, offset));
	return result;
    }

    /**
     * handle the sign on.
     *
     * @param serialPort the serialport
     * @param os the outputstram
     * @param is the inputstream of the serialport
     * @param timeout the timeout to read data
     * @param handleEcho handle the echo
     * @param baudRateChangeDelay the delay to change the baud rate
     * @return the readed id of the tariff device
     * @throws IOException
     * @throws TimeoutException
     */
    protected String signOn(SerialPort serialPort, OutputStream os, InputStream is, int timeout, boolean handleEcho, int baudRateChangeDelay) throws IOException, TimeoutException {

	setSerialPortParams(serialPort, baudRateChangeDelay, 300, SerialPort.DATABITS_7, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);

	sendData(os, REQUEST_MESSAGE);

	int offset = 0;
	if (handleEcho) {
	    offset = 5;
	}
	byte[] signOnResponse;
	int bytesToReadAtLeast = 6;
	if (handleEcho) {
	    bytesToReadAtLeast = 11;
	}
	try {
	    signOnResponse = readData(is, bytesToReadAtLeast, COMPLETION_CHARACTERS, timeout);
	} catch (MessageNotCompleteException ex) {
	    if (ex.getBytesRead() == offset) {
		TimeoutException e = new TimeoutException("Timout while reading signon response");
		e.initCause(ex);
		throw e;
	    }
	    throw ex;
	}

	int baudRateSetting = signOnResponse[offset + 4];
	int baudRate = getBaudRate(baudRateSetting);
	if (baudRate == -1) {
	    throw new IOException("Syntax error in identification message received: unknown baud rate received.");
	}

	byte[] ack = Arrays.copyOf(ACKNOWLEDGE, ACKNOWLEDGE.length);
	ack[2] = (byte) baudRateSetting;
	sendData(os, ack);

	if (handleEcho) {
	    readData(is, ack.length, COMPLETION_CHARACTERS, timeout);
	}

	setSerialPortParams(serialPort, baudRateChangeDelay, 300, SerialPort.DATABITS_7, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);

	//optional field are not considered
	return new String(signOnResponse, offset + 5, signOnResponse.length - offset - 7, getCharset());
    }

    /**
     * Returns the baud rate chosen by the server for this communication.
     *
     * @param baudCharacter Encoded baud rate (see 6.3.14 item 13c)
     * @return The chosen baud rate or -1 on error
     */
    private int getBaudRate(int baudCharacter) {
	int result = -1;
	switch (baudCharacter) {
	    case 0x30:
		result = 300;
		break;
	    case 0x31:
		result = 600;
		break;
	    case 0x32:
		result = 1200;
		break;
	    case 0x33:
		result = 2400;
		break;
	    case 0x34:
		result = 4800;
		break;
	    case 0x35:
		result = 9600;
		break;
	    case 0x36:
		result = 19200;
		break;
	}
	return result;
    }

}
