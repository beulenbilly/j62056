/*
 * Copyright 2013-14 Fraunhofer ISE
 *
 * This file is part of j62056.
 * For more information visit http://www.openmuc.org
 *
 * j62056 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * j62056 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with j62056.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.j62056;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Connection {

    private final String serialPortName;
    private SerialPort serialPort;

    private final boolean handleEcho;
    private final int baudRateChangeDelay;
    private int timeout = 5000;

    private DataOutputStream os;
    private DataInputStream is;

    protected static final byte[] REQUEST_MESSAGE = new byte[]{(byte) 0x2F, (byte) 0x3F, (byte) 0x21, (byte) 0x0D, (byte) 0x0A};

    protected static final byte[] ACKNOWLEDGE = new byte[]{(byte) 0x06, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x0D, (byte) 0x0A};

    protected static final byte[] COMPLETION_CHARACTERS = new byte[]{(byte) 0x0D, (byte) 0x0A};

    protected static final byte[] MESSAGE_COMPLETION_CHARACTERS = new byte[]{(byte) 0x21, (byte) 0x0D, (byte) 0x0A};

    private static final int INPUT_BUFFER_LENGTH = 1024;
    //private final byte[] buffer = new byte[INPUT_BUFFER_LENGTH];

    private static final Charset charset = Charset.forName("US-ASCII");

    private static final int SLEEP_INTERVAL = 100;

    /**
     * Creates a Connection object. You must call <code>open()</code> before
     * calling <code>read()</code> in order to read data. The timeout is set by
     * default to 5s.
     *
     * @param serialPort examples for serial port identifiers are on Linux
     * "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows "COM1"
     * @param handleEcho tells the connection to throw away echos of outgoing
     * messages. Echos are caused by some optical transceivers.
     * @param baudRateChangeDelay tells the connection the time in ms to wait
     * before changing the baud rate during message exchange. This parameter can
     * usually be set to zero for regular serial ports. If a USB to serial
     * converter is used, you might have to use a delay of around 250ms because
     * otherwise the baud rate is changed before the previous message (i.e. the
     * acknowledgment) has been completely sent.
     */
    public Connection(String serialPort, boolean handleEcho, int baudRateChangeDelay) {
	if (serialPort == null) {
	    throw new IllegalArgumentException("serialPort may not be NULL");
	}

	serialPortName = serialPort;
	this.handleEcho = handleEcho;
	this.baudRateChangeDelay = baudRateChangeDelay;
    }

    /**
     * Creates a Connection object. The option handleEcho is set to false and
     * the baudRateChangeDelay is set to 0.
     *
     * @param serialPort examples for serial port identifiers on Linux are
     * "/dev/ttyS0" or "/dev/ttyUSB0" and on Windows "COM1"
     */
    public Connection(String serialPort) {
	this(serialPort, false, 0);
    }

    /**
     * Sets the maximum time in ms to wait for new data from the remote device.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeout the maximum time in ms to wait for new data.
     */
    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    /**
     * Returns the timeout in ms.
     *
     * @return the timeout in ms.
     */
    public int getTimeout() {
	return timeout;
    }

    /**
     * Opens the serial port associated with this connection.
     *
     * @throws IOException if any kind of error occurs opening the serial port.
     */
    public void open() throws IOException {

	CommPortIdentifier portIdentifier;
	try {
	    portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
	} catch (NoSuchPortException e) {
	    throw new IOException("Serial port with given name does not exist", e);
	}

	if (portIdentifier.isCurrentlyOwned()) {
	    throw new IOException("Serial port is currently in use.");
	}

	CommPort commPort;
	try {
	    commPort = portIdentifier.open(this.getClass().getName(), 2000);
	} catch (PortInUseException e) {
	    throw new IOException("Serial port is currently in use.", e);
	}

	if (!(commPort instanceof SerialPort)) {
	    commPort.close();
	    throw new IOException("The specified CommPort is not a serial port");
	}

	serialPort = (SerialPort) commPort;

	try {
	    os = new DataOutputStream(serialPort.getOutputStream());
	    is = new DataInputStream(serialPort.getInputStream());
	} catch (IOException e) {
	    serialPort.close();
	    serialPort = null;
	    throw new IOException("Error getting input or output or input stream from serial port", e);
	}

    }

    /**
     * Closes the serial port.
     */
    public void close() {
	if (serialPort == null) {
	    return;
	}
	serialPort.close();
	serialPort = null;
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

	if (serialPort == null) {
	    throw new IllegalStateException("Connection is not open.");
	}

	String identification = signOn(serialPort, os, is, timeout, handleEcho, baudRateChangeDelay);

	//ignoring ETX and BCC
	byte[] dataSets = readData(is, 4, MESSAGE_COMPLETION_CHARACTERS, timeout);
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
		readData(is, 1, null, 1000);
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
     * read the datasets.
     *
     * @param datasets the byte array with the data message
     * @param offset the start of the first data line
     * @return the readed data lines
     * @throws IOException
     */
    protected List<DataSet> readDataSets(final byte[] datasets, final int offset) throws IOException {
	List<DataSet> result = new ArrayList<>();
	if (null != datasets) {
	    int index = offset;
	    while (index < datasets.length) {
		int nextValueStart = findNextValueStart(datasets, index);
		if (nextValueStart < 0) {
		    throw new IOException("'(' (0x28) character is expected but not received inside data block of data message.");
		}
		String id = new String(datasets, index, nextValueStart - index, charset);
		index = nextValueStart + 1;

		int nextValueEnd = findNextValueEnd(datasets, index);
		if (nextValueEnd < 0) {
		    throw new IOException("'(' (0x29) character is expected but not received inside data block of data message.");
		}
		String value;
		String unit = "";
		int nextUnitStart = findNextUnitStart(datasets, index, nextValueEnd);
		if (nextUnitStart > 0) {
		    value = new String(datasets, index, nextUnitStart - index, charset);
		    index = nextUnitStart + 1;
		    unit = new String(datasets, index, nextValueEnd - index, charset);
		    index = nextValueEnd + 1;
		} else {
		    value = new String(datasets, index, nextValueEnd - index, charset);
		    index = nextValueEnd + 1;
		}
		result.add(new DataSet(id, value, unit));

		if (termindatedWithCrLf(datasets, index)) {
		    index += 2;
		}
		if (endOfDataSets(datasets, index)) {
		    break;
		}
	    }
	}
	return result;
    }

    /**
     * finds the next start of a value.
     *
     * @param datasets the byte array with the data lines
     * @param offset start position
     * @return the next start of a value or -1
     */
    protected int findNextValueStart(final byte[] datasets, final int offset) {
	int result = -1;
	for (int i = offset; i < datasets.length - 1; i++) {
	    if (datasets[i] == 0x28) {
		result = i;
		break;
	    }
	}
	return result;
    }

    /**
     * finds the next end of a value.
     *
     * @param datasets the byte array with the data lines
     * @param offset the start position
     * @return the next end of the value or -1
     */
    protected int findNextValueEnd(final byte[] datasets, final int offset) {
	int result = -1;
	for (int i = offset; i < datasets.length - 1; i++) {
	    if (datasets[i] == 0x29) {
		result = i;
		break;
	    }
	}
	return result;
    }

    /**
     * finds the next start of a unit.
     *
     * @param datasets the bytearray with the data lines
     * @param offset the start position
     * @param upto the end position
     * @return the next start of a unit or -1
     */
    protected int findNextUnitStart(final byte[] datasets, final int offset, final int upto) {
	int result = -1;
	for (int i = offset; (i < upto) && (i < datasets.length); i++) {
	    if (datasets[i] == 0x2A) {
		// found '*'; start of unit
		result = i;
		break;
	    }
	}
	return result;
    }

    /**
     * checks if the end of data block is reached.
     *
     * @param datasets the byte array with the data lines
     * @param offset the current position
     * @return true if the next bytes indicate the end of a data block
     */
    protected boolean endOfDataSets(final byte[] datasets, final int offset) {
	return endsWith(datasets, offset + 4, MESSAGE_COMPLETION_CHARACTERS);
    }

    /**
     * checks if the end of data line is terminated with CR+LF.
     *
     * @param datasets the byte array with the data lines
     * @param offset the current position
     * @return true if the next bytes indicate the termination with CR+LF of a
     * data line
     */
    protected boolean termindatedWithCrLf(final byte[] datasets, final int offset) {
	return endsWith(datasets, offset + 3, COMPLETION_CHARACTERS);
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
	return new String(signOnResponse, offset + 5, signOnResponse.length - offset - 7, charset);
    }

    protected void sendData(OutputStream os, byte[] bytes) throws IOException {
	os.write(bytes);
	os.flush();
    }

    /**
     * read data from the input stream and wait for readEnd bytes or timout.
     *
     * @param is the inputstream to read from
     * @param readAtLeastBytes amount of byte to read at least
     * @param readEnd the expected end of the received bytes
     * @param timeout timeout in ms, 0 wait infinite
     * @return the received bytes
     * @throws IOException
     * @throws MessageNotCompleteException if the message does not end with the
     * expected bytes
     */
    protected byte[] readData(InputStream is, int readAtLeastBytes, byte[] readEnd, int timeout) throws IOException, MessageNotCompleteException {
	boolean readSuccessful = false;
	byte[] readBuffer = new byte[INPUT_BUFFER_LENGTH];
	int timeval = 0;
	int numBytesReadTotal = 0;

	while (timeout == 0 || timeval < timeout) {
	    int availableBytes = is.available();
	    if ((availableBytes > 0) && (numBytesReadTotal + availableBytes <= INPUT_BUFFER_LENGTH)) {
		int bytesToRead = 1;
		if (numBytesReadTotal == 0) {
		    bytesToRead = readAtLeastBytes;
		}
		int numBytesRead = is.read(readBuffer, numBytesReadTotal, bytesToRead);
		numBytesReadTotal += numBytesRead;

		if (numBytesRead > 0) {
		    timeval = 0;
		}

		if ((numBytesReadTotal >= readAtLeastBytes) && ((null == readEnd) || endsWith(readBuffer, numBytesReadTotal, readEnd))) {
		    readSuccessful = true;
		    break;
		}
	    }

	    try {
		Thread.sleep(SLEEP_INTERVAL);
	    } catch (InterruptedException e) {
	    }

	    timeval += SLEEP_INTERVAL;
	}

	byte[] result = Arrays.copyOf(readBuffer, numBytesReadTotal);

	if (!readSuccessful) {
	    throw new MessageNotCompleteException(numBytesReadTotal, result, "Error while reading message");
	}
	return result;
    }

    protected boolean endsWith(final byte[] input, final int readedBytes, final byte[] endBytes) {
	boolean result = true;
	if (null != endBytes) {
	    if ((null != input) && (input.length >= readedBytes) && (endBytes.length <= readedBytes)) {
		for (int i = 1; i <= endBytes.length; i++) {
		    if (endBytes[endBytes.length - i] != input[readedBytes - i]) {
			result = false;
			break;
		    }
		}
	    } else {
		//less bytes read than expected
		result = false;
	    }
	}
	return result;
    }

    /**
     * configre the serialport.
     *
     * @param serialPort the serial port to configure
     * @param changeDelay time to wait for changing the settings
     * @param baudrate the baudrate
     * @param databits the databits @see gnu.io.SerialPort
     * @param stopbits the stopbits @see gnu.io.SerialPort
     * @param parity the parity @see gnu.io.SerialPort
     * @throws IOException if the serialport does not support the settings
     */
    protected void setSerialPortParams(SerialPort serialPort, int changeDelay, int baudrate, int databits, int stopbits, int parity) throws IOException {
	if (changeDelay > 0) {
	    try {
		Thread.sleep(changeDelay);
	    } catch (InterruptedException e1) {
	    }
	}
	try {
	    serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
	} catch (UnsupportedCommOperationException e) {
	    throw new IOException("Unable to set the given serial comm parameters", e);
	}
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
