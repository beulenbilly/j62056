/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.MessageNotCompleteException;

/**
 *
 * @author bilke
 */
public class AbstractConnectionTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private AbstractConnection instance;

    @Before
    public void setUp() {
	instance = new AbstractConnection("/dev/null") {
	    @Override
	    public List<DataSet> read() throws IOException, TimeoutException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	    }

	};
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testEndsWithNull() {
	byte[] input = null;
	int inputEnd = 0;
	byte[] endBytes = null;
	Assert.assertTrue(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithNotReadEnough() {
	byte[] input = null;
	int inputEnd = 0;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithNullInput() {
	byte[] input = null;
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithWrongInputEnd() {
	byte[] input = {25};
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithWrongEnd() {
	byte[] input = {25, 27, 32, 56, 64, 84, 32};
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithWrongEnd2() {
	byte[] input = {25, 27, 3, 3, 2, 24, 11};
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithWrongEnd3() {
	byte[] input = {25, 27, 32, 2, 3, 84, 32};
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithWrongEnd4() {
	byte[] input = {25, 27, 1, 2, 34, 84, 32};
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertFalse(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testEndsWithCorrectEnd() {
	byte[] input = {25, 27, 1, 2, 3, 84, 32};
	int inputEnd = 5;
	byte[] endBytes = {1, 2, 3};
	Assert.assertTrue(instance.endsWith(input, inputEnd, endBytes));
    }

    @Test
    public void testReadDataNone() throws IOException {
	exception.expect(MessageNotCompleteException.class);
	InputStream is = Mockito.mock(InputStream.class);
	instance.readData(is, 10, AbstractConnection.COMPLETION_CHARACTERS, 1000);
    }

    @Test
    public void testReadDataIoException() throws IOException {
	exception.expect(IOException.class);
	InputStream is = Mockito.mock(InputStream.class);
	Mockito.when(is.available()).thenReturn(2);
	Mockito.when(is.read(Mockito.any(byte[].class), Mockito.any(int.class), Mockito.any(int.class))).thenThrow(new IOException());
	instance.readData(is, 10, AbstractConnection.COMPLETION_CHARACTERS, 1000);
    }

    @Test
    public void testReadDataWrongEnd() throws IOException {
	final byte[] answer = {12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3};
	exception.expect(MessageNotCompleteException.class);
	InputStream is = createInputStream(answer);
	instance.readData(is, 10, AbstractConnection.COMPLETION_CHARACTERS, 1000);
    }

    @Test
    public void testReadDataCorrectEnd() throws IOException {
	final byte[] answer = {12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3};
	InputStream is = createInputStream(answer);
	byte[] end = {2, 1, 3};
	Assert.assertArrayEquals(answer, instance.readData(is, 10, end, 1000));
    }

    @Test
    public void testReadDataAck() throws IOException {
	final byte[] answer = AbstractConnection.ACKNOWLEDGE;
	InputStream is = createInputStream(answer);
	Assert.assertArrayEquals(answer, instance.readData(is, answer.length, AbstractConnection.COMPLETION_CHARACTERS, 1000));
    }

    @Test
    public void testReadDataEndMiddle() throws IOException {
	final byte[] answer = {12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3, 24, 43, 21, 13};
	InputStream is = createInputStream(answer);
	byte[] end = {2, 1, 3};
	Assert.assertArrayEquals(new byte[]{12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3}, instance.readData(is, 10, end, 1000));
	Assert.assertTrue(is.available() == 4);
    }

    @Test
    public void testReadDataInSteps() throws IOException {
	final byte[] answer = {12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3, 24, 43, 21, 13};
	InputStream is = createInputStream(answer);
	byte[] end = {2, 1, 3};
	Assert.assertArrayEquals(new byte[]{12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3}, instance.readData(is, 10, end, 1000));
	Assert.assertTrue(is.available() == 4);
	Assert.assertArrayEquals(new byte[]{24, 43, 21}, instance.readData(is, 3, null, 1000));
	Assert.assertTrue(is.available() == 1);
	Assert.assertArrayEquals(new byte[]{13}, instance.readData(is, 1, null, 1000));
	Assert.assertTrue(is.available() == 0);
    }

    @Test
    public void testSendData() throws IOException {
	OutputStream os = Mockito.mock(OutputStream.class);
	byte[] data = {21, 12, 42, 12, 4, 2, 35};
	instance.sendData(os, data);
	Mockito.verify(os).write(data);
	Mockito.verify(os).flush();
    }

    @Test
    public void testSendDataIoExceptionSending() throws IOException {
	exception.expect(IOException.class);
	OutputStream os = Mockito.mock(OutputStream.class);
	Mockito.doThrow(new IOException()).when(os).write(Mockito.any(byte[].class));
	byte[] data = {21, 12, 42, 12, 4, 2, 35};
	instance.sendData(os, data);
    }

    @Test
    public void testSendDataIoExceptionFlush() throws IOException {
	exception.expect(IOException.class);
	OutputStream os = Mockito.mock(OutputStream.class);
	Mockito.doThrow(new IOException()).when(os).flush();
	byte[] data = {21, 12, 42, 12, 4, 2, 35};
	instance.sendData(os, data);
    }

    @Test
    public void testTermindatedWithCrLf() {
	byte[] data = {21, 12, 42, 12, 0x0d, 0x0a, 35};
	Assert.assertTrue(instance.termindatedWithCrLf(data, 4));
    }

    @Test
    public void testTermindatedWithCrLf2() {
	byte[] data = {21, 12, 42, 12, 0x0d, 0x0a};
	Assert.assertTrue(instance.termindatedWithCrLf(data, 4));
    }

    @Test
    public void testNotTermindatedWithCrLf() {
	byte[] data = {21, 12, 42, 12, 0x0d, 0x0a, 35};
	Assert.assertFalse(instance.termindatedWithCrLf(data, 5));
    }

    @Test
    public void testNotTermindatedWithCrLf2() {
	byte[] data = {21, 12, 42, 12, 0x0d, 0x0a, 35};
	Assert.assertFalse(instance.termindatedWithCrLf(data, 7));
    }

    @Test
    public void testNotTermindatedWithCrLfNull() {
	byte[] data = null;
	Assert.assertFalse(instance.termindatedWithCrLf(data, 7));
    }

    @Test
    public void testEndOfDataSets() {
	byte[] data = {21, 12, 42, 0x21, 0x0d, 0x0a, 35};
	Assert.assertTrue(instance.endOfDataSets(data, 3));
    }

    @Test
    public void testEndOfDataSets2() {
	byte[] data = {21, 12, 42, 0x21, 0x0d, 0x0a};
	Assert.assertTrue(instance.endOfDataSets(data, 3));
    }

    @Test
    public void testNoEndOfDataSets() {
	byte[] data = {21, 12, 42, 0x21, 0x0d, 0x0a};
	Assert.assertFalse(instance.endOfDataSets(data, 4));
    }

    @Test
    public void testNoEndOfDataSets2() {
	byte[] data = {21, 12, 42, 0x21, 0x0d, 0x0a};
	Assert.assertFalse(instance.endOfDataSets(data, 7));
    }

    @Test
    public void testNoEndOfDataSetsNull() {
	byte[] data = null;
	Assert.assertFalse(instance.endOfDataSets(data, 7));
    }

    @Test
    public void testReadDataSetsNull() throws IOException {
	List<DataSet> dataSets = instance.readDataSets(null, 0);
	Assert.assertNotNull(dataSets);
	Assert.assertTrue(dataSets.isEmpty());
    }

    @Test
    public void testReadDataSetsInValidOffset() throws IOException {
	List<DataSet> dataSets = instance.readDataSets("ksfdjalksdjflakjd".getBytes(), 25);
	Assert.assertNotNull(dataSets);
	Assert.assertTrue(dataSets.isEmpty());
    }

    @Test
    public void testReadValidDataSets() throws IOException {
	String dataBlock = new String(new byte[]{(byte) 0x02}) //STX
		+ "1-0:0.0.0*255(1ESY1160142770)\r\n"
		+ "1-0:1.8.0*255(00000504.9023619*kWh)\r\n"
		+ "1-0:21.7.0*255(-000115.94*W)\r\n"
		+ "1-0:96.5.5*255(80)\r\n"
		+ "0-0:96.1.255*255(1ESY1160142770)\r\n"
		+ "!\r\n"
		+ new String(new byte[]{(byte) 0x03, (byte) 0xff}); //ETX plus fake-BCC
	List<DataSet> dataSets = instance.readDataSets(dataBlock.getBytes(), 1);
	Assert.assertNotNull(dataSets);
	Assert.assertTrue(dataSets.size() == 5);
	for (int i = 0; i < dataSets.size(); i++) {
	    DataSet ds = dataSets.get(i);
	    switch (i) {
		case 0:
		    Assert.assertEquals("1-0:0.0.0*255", ds.getId());
		    Assert.assertEquals("1ESY1160142770", ds.getValue());
		    Assert.assertEquals("", ds.getUnit());
		    break;
		case 1:
		    Assert.assertEquals("1-0:1.8.0*255", ds.getId());
		    Assert.assertEquals("00000504.9023619", ds.getValue());
		    Assert.assertEquals("kWh", ds.getUnit());
		    break;
		case 2:
		    Assert.assertEquals("1-0:21.7.0*255", ds.getId());
		    Assert.assertEquals("-000115.94", ds.getValue());
		    Assert.assertEquals("W", ds.getUnit());
		    break;
		case 3:
		    Assert.assertEquals("1-0:96.5.5*255", ds.getId());
		    Assert.assertEquals("80", ds.getValue());
		    Assert.assertEquals("", ds.getUnit());
		    break;
		case 4:
		    Assert.assertEquals("0-0:96.1.255*255", ds.getId());
		    Assert.assertEquals("1ESY1160142770", ds.getValue());
		    Assert.assertEquals("", ds.getUnit());
		    break;
		default:
		    Assert.fail("not implemented");
		    break;
	    }
	}
    }

    private InputStream createInputStream(byte[] bytes) {
	return new ByteArrayInputStream(bytes);
    }

    public static void main(String[] args) {
	for (byte b : "!\r\n".getBytes()) {
	    System.out.println("Byte: " + b);
	}
    }

}
