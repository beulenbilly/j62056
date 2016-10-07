/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmuc.j62056.impl;

import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.openmuc.j62056.DataSet;
import org.openmuc.j62056.MessageNotCompleteException;

/**
 *
 * @author sascha
 */
public class ModeDConnectionTest {

    public ModeDConnectionTest() {
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWithoutStart() throws Exception {
	String bytes = "ESY5Q3DA3024 V3.04\r\n\r\n"
		+ "1-0:0.0.0*255(1ESY1160142770)\r\n"
		+ "1-0:1.8.0*255(00000504.9023619*kWh)\r\n"
		+ "1-0:21.7.0*255(-000115.94*W)\r\n"
		+ "1-0:96.5.5*255(80)\r\n"
		+ "0-0:96.1.255*255(1ESY1160142770)\r\n"
		+ "!\r\n";
	ModeDConnection instance = createInstance(bytes);
	List<DataSet> result = instance.read();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWithoutStartDataset() throws Exception {
	String bytes = "/ESY5Q3DA3024 V3.04\r\n"
		+ "1-0:0.0.0*255(1ESY1160142770)\r\n"
		+ "1-0:1.8.0*255(00000504.9023619*kWh)\r\n"
		+ "1-0:21.7.0*255(-000115.94*W)\r\n"
		+ "1-0:96.5.5*255(80)\r\n"
		+ "0-0:96.1.255*255(1ESY1160142770)\r\n"
		+ "!\r\n";
	ModeDConnection instance = createInstance(bytes);
	List<DataSet> result = instance.read();
    }

    @Test
    public void testRead() throws Exception {
	String bytes = "/ESY5Q3DA3024 V3.04\r\n\r\n"
		+ "1-0:0.0.0*255(1ESY1160142770)\r\n"
		+ "1-0:1.8.0*255(00000504.9023619*kWh)\r\n"
		+ "1-0:21.7.0*255(-000115.94*W)\r\n"
		+ "1-0:96.5.5*255(80)\r\n"
		+ "0-0:96.1.255*255(1ESY1160142770)\r\n"
		+ "!\r\n";
	ModeDConnection instance = createInstance(bytes);
	List<DataSet> result = instance.read();

	Assert.assertNotNull(result);
	Assert.assertTrue(result.size() == 6);
	for (int i = 0; i < result.size(); i++) {
	    DataSet ds = result.get(i);
	    switch (i) {
		case 0:
		    Assert.assertEquals("Q3DA3024 V3.04", ds.getId());
		    Assert.assertEquals("", ds.getValue());
		    Assert.assertEquals("", ds.getUnit());
		    break;
		case 1:
		    Assert.assertEquals("1-0:0.0.0*255", ds.getId());
		    Assert.assertEquals("1ESY1160142770", ds.getValue());
		    Assert.assertEquals("", ds.getUnit());
		    break;
		case 2:
		    Assert.assertEquals("1-0:1.8.0*255", ds.getId());
		    Assert.assertEquals("00000504.9023619", ds.getValue());
		    Assert.assertEquals("kWh", ds.getUnit());
		    break;
		case 3:
		    Assert.assertEquals("1-0:21.7.0*255", ds.getId());
		    Assert.assertEquals("-000115.94", ds.getValue());
		    Assert.assertEquals("W", ds.getUnit());
		    break;
		case 4:
		    Assert.assertEquals("1-0:96.5.5*255", ds.getId());
		    Assert.assertEquals("80", ds.getValue());
		    Assert.assertEquals("", ds.getUnit());
		    break;
		case 5:
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

    private ModeDConnection createInstance(final String readedBytes) {
	return new ModeDConnection("/dev/null") {
	    @Override
	    protected byte[] readData(InputStream is, int readAtLeastBytes, byte[] readEnd, int timeout) throws IOException, MessageNotCompleteException {
		return readedBytes.getBytes(getCharset());
	    }

	    @Override
	    protected SerialPort getSerialPort() {
		return Mockito.mock(SerialPort.class);
	    }

	};
    }
}
