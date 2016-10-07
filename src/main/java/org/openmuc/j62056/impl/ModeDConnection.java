/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.openmuc.j62056.DataSet;
import static org.openmuc.j62056.impl.AbstractConnection.MESSAGE_COMPLETION_CHARACTERS;
import org.openmuc.j62056.model.Header;

/**
 *
 * @author sascha
 */
public class ModeDConnection extends AbstractConnection {

    public ModeDConnection(String serialPort, boolean handleEcho, int baudRateChangeDelay) {
	super(serialPort, handleEcho, baudRateChangeDelay);
    }

    public ModeDConnection(String serialPort) {
	super(serialPort);
    }

    @Override
    public List<DataSet> read() throws IOException, TimeoutException {
	if (getSerialPort() == null) {
	    throw new IllegalStateException("Connection is not open.");
	}

	//ignoring ETX and BCC
	byte[] dataSets = readData(getIs(), 12, MESSAGE_COMPLETION_CHARACTERS, getTimeout());

	Header header = convert(dataSets);

	String identification = null;

	if (null != header) {
	    identification = header.getIdentifier();
	} else {
	    throw new IllegalArgumentException("Could not convert header: " + new String(dataSets, getCharset()));
	}
	boolean withCheckCharacter = false;
	int offset = findDataStart(dataSets);
	if (offset < 0) {
	    throw new IOException("Start of data message not found.");
	}
	if (dataSets[offset] == 0x02) {
	    withCheckCharacter = true;
	    offset += 1;
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

    private int findDataStart(byte[] data) {
	int result = -1;
	if ((null != data) && (data.length > 0)) {
	    for (int i = 0; i < (data.length - 3); i++) {
		byte b = data[i];
		switch (data[i]) {
		    case 0x0D:
			if ((0x0A == data[i + 1]) && (0x0D == data[i + 2]) && (0x0A == data[i + 3])) {
			    result = i + 4;
			    break;
			}
		    default:
			break;
		}
	    }
	}
	return result;
    }
}
