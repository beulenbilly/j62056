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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.openmuc.j62056.config.Mode;
import org.openmuc.j62056.config.Parity;

public class ReadMeter {

    private static void printUsage() {
	System.out.println("SYNOPSIS\n\torg.openmuc.j62056.ReadMeter [-e] [-d <baud_rate_change_delay>] [-m <mode>] [-rt <read timeout>] [-br <baudrate>] [-p <parity>] [-db <databits>] [-sb <stop bits>] <serial_port>");
	System.out.println("DESCRIPTION\n\tReads the meter connected to the given serial port and prints the received data to stdout. First prints the identification string received from the meter. Then the data sets received are printed. Each data set is printed on a single line with the format: \"<id>;<value>;<unit>\". Errors are printed to stderr.");
	System.out.println("OPTIONS");
	System.out.println("\t<serial_port>\n\t    The serial port used for communication. Examples are /dev/ttyS0 (Linux) or COM1 (Windows)\n");
	System.out.println("\t-e\n\t    Enable handling of echos caused by some optical tranceivers\n");
	System.out.println("\t-d <baud_rate_change_delay>\n\t    Delay of baud rate change in ms. Default is 0. USB to serial converters often require a delay of up to 250ms.\n");
	System.out.println("\t-m <mode>\n\t    Mode of the connection a,b,c,d or e\n");
	System.out.println("\t-rt <read timeout>\n\t    time to wait for data, dafault is 5000ms\n");
	System.out.println("\t-br <baud rate>\n\t    if you have to change the baud rate, default depends on the mode (300 for Mode C, 2400 for Mode D)\n");
	System.out.println("\t-p <parity>\n\t    if you have to change the parity, default depends on the mode\n");
	System.out.println("\t-db <databits>\n\t    if you have to change the databits, default depends on the mode\n");
	System.out.println("\t-sb <stop bits>\n\t    if you have to change the stop bits, default depends on the mode\n");
    }

    public static void main(String[] args) {
	if (args.length < 1 || args.length > 17) {
	    printUsage();
	    System.exit(1);
	}

	String serialPortName = "";
	boolean echoHandling = false;
	int baudRateChangeDelay = 0;
	Mode mode = Mode.C;
	int readTimeout = -1;
	int baudRate = -1;
	Parity parity = null;
	int dataBits = -1;
	int stopBits = -1;
	for (int i = 0; i < args.length; i++) {
	    switch (args[i]) {
		case "-e":
		    echoHandling = true;
		    break;
		case "-d":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			baudRateChangeDelay = Integer.parseInt(args[i]);
		    } catch (NumberFormatException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		case "-m":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			mode = Mode.valueOf(args[i].toUpperCase());
		    } catch (IllegalArgumentException | NullPointerException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		case "-rt":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			readTimeout = Integer.parseInt(args[i]);
		    } catch (NumberFormatException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		case "-br":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			baudRate = Integer.parseInt(args[i]);
		    } catch (NumberFormatException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		case "-db":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			dataBits = Integer.parseInt(args[i]);
		    } catch (NumberFormatException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		case "-sb":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			stopBits = Integer.parseInt(args[i]);
		    } catch (NumberFormatException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		case "-p":
		    i++;
		    if (i == args.length) {
			printUsage();
			System.exit(1);
		    }
		    try {
			parity = Parity.convert(args[i]);
		    } catch (NumberFormatException e) {
			printUsage();
			System.exit(1);
		    }
		    break;
		default:
		    serialPortName = args[i];
		    break;
	    }
	}

	Connection connection = new Connection(serialPortName, echoHandling, baudRateChangeDelay, mode);

	if (readTimeout > -1) {
	    connection.setTimeout(readTimeout);
	}
	if (baudRate > 0) {
	    connection.setBaudRate(baudRate);
	}
	if (null != parity) {
	    connection.setParity(parity.value);
	}
	if (dataBits > 0) {
	    connection.setDatabits(dataBits);
	}
	if (stopBits > 0) {
	    connection.setStopbits(stopBits);
	}

	try {
	    connection.open();
	} catch (IOException e) {
	    System.err.println("Failed to open serial port: " + e.getMessage());
	    System.exit(1);
	}

	List<DataSet> dataSets = null;
	try {
	    dataSets = connection.read();
	} catch (IOException e) {
	    System.err.println("IOException while trying to read: " + e.getMessage());
	    connection.close();
	    System.exit(1);
	} catch (TimeoutException e) {
	    System.err.print("Read attempt timed out");
	    connection.close();
	    System.exit(1);
	}

	Iterator<DataSet> dataSetIt = dataSets.iterator();

	// print identification string
	System.out.println(dataSetIt.next().getId());

	// print data sets on the following lines
	while (dataSetIt.hasNext()) {
	    DataSet dataSet = dataSetIt.next();
	    System.out.println(dataSet.getId() + ";" + dataSet.getValue() + ";" + dataSet.getUnit());
	}

	connection.close();

    }

}
