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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.openmuc.j62056.config.Mode;
import org.openmuc.j62056.impl.AbstractConnection;
import org.openmuc.j62056.impl.ModeDConnection;

public class Connection {

    private AbstractConnection connection;

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
	this(serialPort, handleEcho, baudRateChangeDelay, Mode.D);
    }

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
     * @param mode the mode
     */
    public Connection(String serialPort, boolean handleEcho, int baudRateChangeDelay, Mode mode) {
	if (serialPort == null) {
	    throw new IllegalArgumentException("serialPort may not be NULL");
	}
	if (null == mode) {
	    throw new IllegalArgumentException("mode may not be NULL");
	}
	switch (mode) {
	    case D:
		connection = new ModeDConnection(serialPort, handleEcho, baudRateChangeDelay);
		break;
	    case E:
		break;
	    default:
		throw new IllegalArgumentException("Mode is not implemented: " + mode);
	}
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
	connection.setTimeout(timeout);
    }

    /**
     * Returns the timeout in ms.
     *
     * @return the timeout in ms.
     */
    public int getTimeout() {
	return connection.getTimeout();
    }

    /**
     * Opens the serial port associated with this connection.
     *
     * @throws IOException if any kind of error occurs opening the serial port.
     */
    public void open() throws IOException {

	connection.open();

    }

    /**
     * Closes the serial port.
     */
    public void close() {
	connection.close();
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
	return connection.read();
    }

}
