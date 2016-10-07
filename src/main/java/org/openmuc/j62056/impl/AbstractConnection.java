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
package org.openmuc.j62056.impl;

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
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.MessageNotCompleteException;
import org.openmuc.j62056.model.BaudRate;
import org.openmuc.j62056.model.Header;

public abstract class AbstractConnection implements AutoCloseable {

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
    public AbstractConnection(String serialPort, boolean handleEcho, int baudRateChangeDelay) {
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
    public AbstractConnection(String serialPort) {
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
    @Override
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
    public abstract List<DataSet> read() throws IOException, TimeoutException;

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
	    if (datasets[i] == (byte) 0x28) {
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
	return endsWith(datasets, offset + 3, MESSAGE_COMPLETION_CHARACTERS);
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
	return endsWith(datasets, offset + 2, COMPLETION_CHARACTERS);
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

    protected Header convert(byte[] data) {
	Header header = null;
	if ((null != data) && (data.length > 0)) {
	    int start = -1;
	    int end = -1;
	    for (int i = 0; i < (data.length - 1); i++) {
		byte b = data[i];
		switch (data[i]) {
		    case 0x2F:
			if ((i + 1) < data.length) {
			    start = i + 1;
			}
			break;
		    case 0x0D:
			if (0x0A == data[i + 1]) {
			    end = i;
			    break;
			}
		    default:
			break;
		}
		if ((start != -1) && (end != -1)) {
		    break;
		}
	    }
	    if ((start != -1) && (end != -1)) {
		String mId = new String(data, start, 3, getCharset());
		byte baudRateByte = data[start + 3];
		String id = new String(data, start + 4, end - start - 4, getCharset());
		header = new Header(id, mId, baudRateByte, BaudRate.convert(baudRateByte));
	    }
	}
	return header;
    }

    protected SerialPort getSerialPort() {
	return serialPort;
    }

    protected boolean isHandleEcho() {
	return handleEcho;
    }

    protected DataOutputStream getOs() {
	return os;
    }

    protected DataInputStream getIs() {
	return is;
    }

    protected int getBaudRateChangeDelay() {
	return baudRateChangeDelay;
    }

    protected static Charset getCharset() {
	return charset;
    }

}
