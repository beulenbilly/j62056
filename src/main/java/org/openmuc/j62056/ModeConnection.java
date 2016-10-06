/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author sascha
 */
public interface ModeConnection extends AutoCloseable {

    /**
     * Opens the serial port associated with this connection.
     *
     * @throws IOException if any kind of error occurs opening the serial port.
     */
    public void open() throws IOException;

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
    public List<DataSet> read() throws IOException, TimeoutException;

}
