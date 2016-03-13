/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

/**
 *
 * @author bilke
 */
public class ConnectionTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private Connection instance;

    @Before
    public void setUp() {
	instance = new Connection("/dev/null");
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
	instance.readData(is, 10, Connection.COMPLETION_CHARACTERS, 1000);
    }

    @Test
    public void testReadDataIoException() throws IOException {
	exception.expect(IOException.class);
	InputStream is = Mockito.mock(InputStream.class);
	Mockito.when(is.available()).thenReturn(2);
	Mockito.when(is.read(Mockito.any(byte[].class), Mockito.any(int.class), Mockito.any(int.class))).thenThrow(new IOException());
	instance.readData(is, 10, Connection.COMPLETION_CHARACTERS, 1000);
    }

    @Test
    public void testReadDataWrongEnd() throws IOException {
	final byte[] answer = {12, 32, 53, 23, 54, 12, 34, 21, 2, 1, 3};
	exception.expect(MessageNotCompleteException.class);
	InputStream is = createInputStream(answer);
	instance.readData(is, 10, Connection.COMPLETION_CHARACTERS, 1000);
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
	final byte[] answer = Connection.ACKNOWLEDGE;
	InputStream is = createInputStream(answer);
	Assert.assertArrayEquals(answer, instance.readData(is, answer.length, Connection.COMPLETION_CHARACTERS, 1000));
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
	Assert.assertTrue(instance.termindatedWithCrLf(data, 3));
    }

    @Test
    public void testTermindatedWithCrLf2() {
	byte[] data = {21, 12, 42, 12, 0x0d, 0x0a};
	Assert.assertTrue(instance.termindatedWithCrLf(data, 3));
    }

    @Test
    public void testNotTermindatedWithCrLf() {
	byte[] data = {21, 12, 42, 12, 0x0d, 0x0a, 35};
	Assert.assertFalse(instance.termindatedWithCrLf(data, 4));
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
	Assert.assertTrue(instance.endOfDataSets(data, 2));
    }

    @Test
    public void testEndOfDataSets2() {
	byte[] data = {21, 12, 42, 0x21, 0x0d, 0x0a};
	Assert.assertTrue(instance.endOfDataSets(data, 2));
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

    private InputStream createInputStream(byte[] bytes) {
	return new ByteArrayInputStream(bytes);
    }

}
