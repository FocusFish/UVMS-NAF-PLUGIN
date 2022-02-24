/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.naf.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.focus.schema.exchange.movement.asset.v1.AssetIdType;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.schema.exchange.movement.v1.MovementPoint;
import fish.focus.schema.exchange.movement.v1.MovementTypeType;
import fish.focus.schema.exchange.movement.v1.SetReportMovementType;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.plugins.naf.constants.NafCode;
import eu.europa.ec.fisheries.uvms.plugins.naf.exception.PluginException;
import eu.europa.ec.fisheries.uvms.plugins.naf.util.DateUtil;

/**
 * Unit test for simple App.
 */
public class NafMessageResponseMapperTest {
	
    private static final double DELTA_VALUE = 0.001;

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // Add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void mapToMovementTypeTest() {
        try {
            String nafMessage = "//SR//FR/SWE//AD/XNE//TM/POS//RD/20050110//RT/1201//RN/3434//RC/SSSS//ER//";
            String pluginName = "NafMapperTest";
            SetReportMovementType retVal = NafMessageResponseMapper.mapToMovementType(nafMessage, pluginName);

            assertEquals(pluginName, retVal.getPluginName());
            assertEquals(PluginType.NAF, retVal.getPluginType());
            assertNotNull(retVal.getTimestamp());
            assertNotNull(retVal.getMovement());
        } catch (PluginException ex) {
            Logger.getLogger(NafMessageResponseMapperTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void mapEntryTest() {
        MovementBaseType movement = new MovementBaseType();

        String course = "10.5";
        NafMessageResponseMapper.mapEntry(NafCode.COURSE, course, movement);
		assertEquals(10.5D, movement.getReportedCourse().doubleValue(), DELTA_VALUE);

        String speed = "70";
        NafMessageResponseMapper.mapEntry(NafCode.SPEED, speed, movement);
        assertEquals(7.0D, movement.getReportedSpeed().doubleValue(), DELTA_VALUE);

        String date = "20160208";
        NafMessageResponseMapper.mapEntry(NafCode.DATE, date, movement);
        assertEquals("20160208", NafMessageResponseMapper.dateString);

        String time = "1527";
        NafMessageResponseMapper.mapEntry(NafCode.TIME, time, movement);
        assertEquals("1527", NafMessageResponseMapper.timeString);

        String externalMark = "ABC-123";
        NafMessageResponseMapper.mapEntry(NafCode.EXTERNAL_MARK, externalMark, movement);
        assertEquals("ABC-123", movement.getExternalMarking());

        String flag = "SWE";
        NafMessageResponseMapper.mapEntry(NafCode.FLAG, flag, movement);
        assertEquals("SWE", movement.getFlagState());

        String latN = "N1213";
        NafMessageResponseMapper.mapEntry(NafCode.LATITUDE, latN, movement);
        assertEquals(12.2167D, movement.getPosition().getLatitude().doubleValue(), DELTA_VALUE);

        String latS = "S1213";
        NafMessageResponseMapper.mapEntry(NafCode.LATITUDE, latS, movement);
        assertEquals(-12.2167D, movement.getPosition().getLatitude().doubleValue(), DELTA_VALUE);

        String lonN = "E01417";
        NafMessageResponseMapper.mapEntry(NafCode.LONGITUDE, lonN, movement);
        assertEquals(14.2833D, movement.getPosition().getLongitude().doubleValue(), DELTA_VALUE);

        String lonS = "W01417";
        NafMessageResponseMapper.mapEntry(NafCode.LONGITUDE, lonS, movement);
        assertEquals(-14.2833D, movement.getPosition().getLongitude().doubleValue(), DELTA_VALUE);

        String callSign = "SMIT";
        NafMessageResponseMapper.mapEntry(NafCode.RADIO_CALL_SIGN, callSign, movement);
        assertEquals("SMIT", movement.getAssetId().getAssetIdList().get(0).getValue());
    }

    @Test
    public void mapDegreeLatLongTest() {
        MovementBaseType movement = new MovementBaseType();

        String latN = "N1213";
        NafMessageResponseMapper.mapEntry(NafCode.LATITUDE, latN, movement);
        assertEquals(12.2167D, movement.getPosition().getLatitude().doubleValue(), DELTA_VALUE);

        String latS = "S1213";
        NafMessageResponseMapper.mapEntry(NafCode.LATITUDE, latS, movement);
        assertEquals(-12.2167D, movement.getPosition().getLatitude().doubleValue(), DELTA_VALUE);

        String lonE = "E01417";
        NafMessageResponseMapper.mapEntry(NafCode.LONGITUDE, lonE, movement);
        assertEquals(14.2833D, movement.getPosition().getLongitude().doubleValue(), DELTA_VALUE);

        String lonW = "W01417";
        NafMessageResponseMapper.mapEntry(NafCode.LONGITUDE, lonW, movement);
        assertEquals(-14.2833D, movement.getPosition().getLongitude().doubleValue(), DELTA_VALUE);

        String lon2E = "E14117";
        NafMessageResponseMapper.mapEntry(NafCode.LONGITUDE, lon2E, movement);
        assertEquals(141.2833D, movement.getPosition().getLongitude().doubleValue(), DELTA_VALUE);

        String lon2W = "W14117";
        NafMessageResponseMapper.mapEntry(NafCode.LONGITUDE, lon2W, movement);
        assertEquals(-141.2833D, movement.getPosition().getLongitude().doubleValue(), DELTA_VALUE);
    }

    @Test
    public void mapDateTimeTest() {
        NafMessageResponseMapper.dateString = "20160208";
        NafMessageResponseMapper.timeString = "1558";
        MovementBaseType movement = new MovementBaseType();
        Date date = DateUtil.parseToUTCDateTime("20160208" + " " + "1558 UTC");

        NafMessageResponseMapper.mapDateTime(movement);
        assertEquals(movement.getPositionTime(), date);

        movement.setPositionTime(null);
        NafMessageResponseMapper.timeString = null;
        NafMessageResponseMapper.mapDateTime(movement);
        assertNull(movement.getPositionTime());

        movement.setPositionTime(null);
        NafMessageResponseMapper.timeString = "";
        NafMessageResponseMapper.mapDateTime(movement);
        assertNull(movement.getPositionTime());

        NafMessageResponseMapper.timeString = "1558";

        movement.setPositionTime(null);
        NafMessageResponseMapper.dateString = null;
        NafMessageResponseMapper.mapDateTime(movement);
        assertNull(movement.getPositionTime());

        movement.setPositionTime(null);
        NafMessageResponseMapper.dateString = "";
        NafMessageResponseMapper.mapDateTime(movement);
        assertNull(movement.getPositionTime());
    }

    @Test
    public void mapIRCSTest() {
        MovementBaseType movement = new MovementBaseType();
        NafMessageResponseMapper.mapIRCS("SMIT", movement);

        assertNotNull(movement.getAssetId());
        assertNotNull(movement.getAssetId().getAssetIdList());
        assertEquals(1, movement.getAssetId().getAssetIdList().size());
        assertEquals("SMIT", movement.getAssetId().getAssetIdList().get(0).getValue());
        assertEquals(AssetIdType.IRCS, movement.getAssetId().getAssetIdList().get(0).getIdType());
    }

    @Test
    public void mapCFRTest() {
        MovementBaseType movement = new MovementBaseType();
        NafMessageResponseMapper.mapCFR("SWE000008121", movement);

        assertNotNull(movement.getAssetId());
        assertNotNull(movement.getAssetId().getAssetIdList());
        assertEquals(1, movement.getAssetId().getAssetIdList().size());
        assertEquals(1, movement.getAssetId().getAssetIdList().size());
        assertEquals("SWE000008121", movement.getInternalReferenceNumber());
        assertEquals("SWE000008121", movement.getAssetId().getAssetIdList().get(0).getValue());
        assertEquals(AssetIdType.CFR, movement.getAssetId().getAssetIdList().get(0).getIdType());
    }

    @Test
    public void getMovementPointTest() {
        MovementPoint point = NafMessageResponseMapper.getMovementPoint(new MovementBaseType());
        assertNotNull(point);
    }

    @Test
    public void mapSpeedTest() {
        MovementBaseType movement = new MovementBaseType();
        String speed = "105";
        NafMessageResponseMapper.mapSpeed(movement, speed);

        assertEquals(10.5, movement.getReportedSpeed().doubleValue(), DELTA_VALUE);
    }
    
    @Test
    public void testParseNormalNAFMessage() throws PluginException {
        String message = "//SR//FR/SWE//AD/UVM//TM/POS//RC/F1007//IR/SWE0000F1007//XR/EXT3//LT/57.037//LG/12.214//"
                + "SP/50//CO/190//DA/20170817//TI/0500//NA/Ship1007//FS/SWE//ER//";
        SetReportMovementType setReportMovementType = NafMessageResponseMapper.mapToMovementType(message, "JUNIT");
        MovementBaseType movement = setReportMovementType.getMovement();
        assertEquals(MovementTypeType.POS, movement.getMovementType());
        assertEquals("F1007", movement.getIrcs());
        assertEquals("SWE0000F1007", movement.getInternalReferenceNumber());
        assertEquals("EXT3", movement.getExternalMarking());
        assertEquals(57.037d, movement.getPosition().getLatitude(), DELTA_VALUE);
        assertEquals(12.214d, movement.getPosition().getLongitude(), DELTA_VALUE);
        assertEquals(5d, movement.getReportedSpeed(), DELTA_VALUE);
        assertEquals(190d, movement.getReportedCourse(), DELTA_VALUE);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(2017, 7, 17, 5, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date expectedDate = c.getTime();
        assertEquals(expectedDate, movement.getPositionTime());
        assertEquals("Ship1007", movement.getAssetName());
        assertEquals("SWE", movement.getFlagState());
    }
    
    @Test
    public void testParseNAFMessageWithEmptyIRCSValue() throws PluginException {
        String message = "//SR//FR/SWE//AD/UVM//TM/POS//RC///IR/SWE0000F1007//XR/EXT3//LT/57.037//LG/12.214//"
                + "SP/50//CO/190//DA/20170817//TI/0500//NA/Ship1007//FS/SWE//ER//";
        SetReportMovementType setReportMovementType = NafMessageResponseMapper.mapToMovementType(message, "JUNIT");
        MovementBaseType movement = setReportMovementType.getMovement();
        assertEquals(MovementTypeType.POS, movement.getMovementType());
        assertTrue(StringUtils.isBlank(movement.getIrcs()));
        assertEquals("SWE0000F1007", movement.getInternalReferenceNumber());
        assertEquals("EXT3", movement.getExternalMarking());
        assertEquals(57.037d, movement.getPosition().getLatitude(), DELTA_VALUE);
        assertEquals(12.214d, movement.getPosition().getLongitude(), DELTA_VALUE);
        assertEquals(5d, movement.getReportedSpeed(), DELTA_VALUE);
        assertEquals(190d, movement.getReportedCourse(), DELTA_VALUE);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(2017, 7, 17, 5, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date expectedDate = c.getTime();
        assertEquals(expectedDate, movement.getPositionTime());
        assertEquals("Ship1007", movement.getAssetName());
        assertEquals("SWE", movement.getFlagState());
    }
    
    @Test(expected = PluginException.class)
    public void testParseInvalidStartRecord() throws PluginException {
    	String message = "//FR/SWE//AD/UVM//TM/POS//IR/SWE0000F1007//LT/57.037//LG/12.214//"
    					+ "SP/50//CO/190//DA/20170817//TI/0500//ER//";
    	NafMessageResponseMapper.mapToMovementType(message, "JUNIT");
    }
    
    @Test(expected = PluginException.class)
    public void testParseInvalidEndRecord() throws PluginException {
    	String message = "//SR//FR/SWE//AD/UVM//TM/POS//IR/SWE0000F1007//LT/57.037//LG/12.214//"
    					+ "SP/50//CO/190//DA/20170817//TI/0500//";
    	NafMessageResponseMapper.mapToMovementType(message, "JUNIT");
    }

    public void messageWithoutFlagstateTest() throws PluginException {
        String message = "//SR//FR/SWE//AD/UVM//TM/POS//IR/SWE0000F1007//LT/57.037//LG/12.214//"
                        + "SP/50//CO/190//DA/20170817//TI/0500//";
        SetReportMovementType movement = NafMessageResponseMapper.mapToMovementType(message, "JUNIT");

        assertEquals("SWE", movement.getMovement().getFlagState());
    }
}