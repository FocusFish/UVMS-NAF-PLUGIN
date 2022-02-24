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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import fish.focus.schema.exchange.common.v1.ReportType;
import fish.focus.schema.exchange.movement.asset.v1.AssetId;
import fish.focus.schema.exchange.movement.asset.v1.AssetIdList;
import fish.focus.schema.exchange.movement.asset.v1.AssetIdType;
import fish.focus.schema.exchange.movement.v1.MovementActivityType;
import fish.focus.schema.exchange.movement.v1.MovementActivityTypeType;
import fish.focus.schema.exchange.movement.v1.MovementPoint;
import fish.focus.schema.exchange.movement.v1.MovementType;
import fish.focus.schema.exchange.movement.v1.MovementTypeType;
import fish.focus.schema.exchange.movement.v1.RecipientInfoType;
import fish.focus.uvms.commons.date.DateUtils;
import eu.europa.ec.fisheries.uvms.plugins.naf.constants.NafCode;

/**
 **/
public class NafMessageRequestMapperTest {
    
    @Test
    public void mapToRequestTest() {
        MovementType movement = new MovementType();
        movement.setReportedCourse(123.0);
        movement.setReportedSpeed(7.3);
        
        Date date = new Date();
        XMLGregorianCalendar greg = DateUtils.dateToXmlGregorian(date);
        movement.setPositionTime(date);
        
        movement.setExternalMarking("EXT");
        movement.setFlagState("SWE");
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(10.7);
        point.setLongitude(11.3);
        movement.setPosition(point);
        
        MovementActivityType activityType = new MovementActivityType();
        activityType.setMessageType(MovementActivityTypeType.FIS);
        movement.setActivity(activityType);
        
        AssetId assetId = new AssetId();
        AssetIdList ircs = new AssetIdList();
        ircs.setIdType(AssetIdType.IRCS);
        ircs.setValue("IRCS");
        AssetIdList imo = new AssetIdList();
        imo.setIdType(AssetIdType.IMO);
        imo.setValue("123456");
        assetId.getAssetIdList().add(ircs);
        assetId.getAssetIdList().add(imo);
        movement.setIrcs("IRCS");
        movement.setAssetId(assetId);
        movement.setAssetName("Sven");
        movement.setFlagState("SWE");
        movement.setTripNumber(12d);
        movement.setInternalReferenceNumber("SWE123");
        
        movement.setMovementType(MovementTypeType.POS);
        
        String targetNaf = "//SR//AD/SWE//FR/UNK//TM/POS//RC/IRCS//TN/12//NA/Sven//FS/SWE";
        targetNaf       += "//IR/SWE123//XR/EXT//LT/10.700//LG/011.300//SP/73//CO/123";
        
        String month = (greg.getMonth() < 10) ? "0" + greg.getMonth() : "" + greg.getMonth();
        String day = (greg.getDay() < 10) ? "0" + greg.getDay() : "" + greg.getDay();
        targetNaf += "//DA/" + greg.getYear() + month + day;
        
        String hour = (greg.getHour() < 10) ? "0" + greg.getHour() : "" + greg.getHour();
        String min = (greg.getMinute()< 10) ? "0" + greg.getMinute(): "" + greg.getMinute();
        targetNaf += "//TI/" + hour + min;
        
        targetNaf += "//AC/FIS//ER//";
        
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        RecipientInfoType info = new RecipientInfoType();
        info.setKey("NAF_ENDPOINT");
        info.setValue("https://localhost/#MESSAGE#");
        report.getRecipientInfo().add(info);
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        Assert.assertEquals(targetNaf, naf);
    }
    
    @Test
    public void getTimeStringTest() {
        MovementType movement = new MovementType();
        Date date = new Date();
        movement.setPositionTime(date);

        XMLGregorianCalendar greg = DateUtils.dateToXmlGregorian(date);
        String time = NafMessageRequestMapper.getTimeString(movement);
        String hour = (greg.getHour() < 10) ? "0" + greg.getHour() : "" + greg.getHour();
        String min = (greg.getMinute()< 10) ? "0" + greg.getMinute(): "" + greg.getMinute();
        String target = "" + hour + min;
        Assert.assertEquals(target, time);
    }
    
    @Test
    public void getDateStringTest() {
        MovementType movement = new MovementType();
        Date date = new Date();
        XMLGregorianCalendar greg = DateUtils.dateToXmlGregorian(date);
        movement.setPositionTime(date);
        
        String dateString = NafMessageRequestMapper.getDateString(movement);
        String month = (greg.getMonth() < 10) ? "0" + greg.getMonth() : "" + greg.getMonth();
        String day = (greg.getDay() < 10) ? "0" + greg.getDay() : "" + greg.getDay();
        String target = "" + greg.getYear() + month + day;
        Assert.assertEquals(target, dateString);
    }
    
    @Test
    public void appendTest() {
        StringBuilder naf = new StringBuilder();
        NafMessageRequestMapper.append(naf, NafCode.TO.getCode(), "VALUE");
        String target = "AD/VALUE//";
        Assert.assertEquals(target, naf.toString());
    }
    
    @Test
    public void getLatitudeStringTest() {
        String latN = NafMessageRequestMapper.getLatitudeString(12.93);
        String targetN = "N1255";
        
        String latS = NafMessageRequestMapper.getLatitudeString(-12.93);
        String targetS = "S1255";
        
        Assert.assertEquals(targetN, latN);
        Assert.assertEquals(targetS, latS);
    }
    
    @Test
    public void getLongitudeStringTest() {
        String lonE = NafMessageRequestMapper.getLongitudeString(12.93);
        String targetE = "E01255";
        
        String lonW = NafMessageRequestMapper.getLongitudeString(-12.93);
        String targetW = "W01255";
        
        Assert.assertEquals(targetE, lonE);
        Assert.assertEquals(targetW, lonW);
    }
    
    @Test
    public void getLongitudeStringTest2() {
        String lonE = NafMessageRequestMapper.getLongitudeString(112.93);
        String targetE = "E11255";
        
        String lonW = NafMessageRequestMapper.getLongitudeString(-112.93);
        String targetW = "W11255";
        
        Assert.assertEquals(targetE, lonE);
        Assert.assertEquals(targetW, lonW);
    }
    
    @Test
    public void appendAssetsTest() {
        MovementType movement = new MovementType();
        AssetId assetId = new AssetId();
        AssetIdList ircs = new AssetIdList();
        ircs.setIdType(AssetIdType.IRCS);
        ircs.setValue("IRCS");
        AssetIdList imo = new AssetIdList();
        imo.setIdType(AssetIdType.IMO);
        imo.setValue("123456");
        assetId.getAssetIdList().add(ircs);
        assetId.getAssetIdList().add(imo);
        movement.setAssetId(assetId);
        
        StringBuilder naf = new StringBuilder();
        NafMessageRequestMapper.appendAsset(naf, NafCode.RADIO_CALL_SIGN.getCode(), AssetIdType.IRCS, movement);
        String target = "RC/IRCS//";
        
        Assert.assertEquals(target, naf.toString());
    }

    @Test
    public void appendNullAttributeTest() {
        ReportType report = new ReportType();
        MovementType movement = new MovementType();
        movement.setMovementType(MovementTypeType.POS);
        movement.setReportedCourse(123d);
        MovementPoint position = new MovementPoint();
        position.setLatitude(1d);
        position.setLongitude(1d);
        movement.setPosition(position);
        movement.setPositionTime(new Date());
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");

        assertFalse(NafCode.EXTERNAL_MARK.matches(naf));
    }
 
    @Test
    public void appendDecimalLatitudeTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(10.1);
        point.setLongitude(1.0);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LATITUDE_DECIMAL.getValue(naf), CoreMatchers.is("10.100"));
    }
    
    @Test
    public void appendDecimalLatitude2Test() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(1.1);
        point.setLongitude(1.0);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LATITUDE_DECIMAL.getValue(naf), CoreMatchers.is("01.100"));
    }
    
    @Test
    public void appendPositiveDecimalLatitudeTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(10.123123);
        point.setLongitude(1.0);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LATITUDE_DECIMAL.getValue(naf), CoreMatchers.is("10.123"));
    }
    
    @Test
    public void appendNegativeDecimalLatitudeTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(-10.123456);
        point.setLongitude(1.0);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LATITUDE_DECIMAL.getValue(naf), CoreMatchers.is("-10.123"));
    }
    
    @Test
    public void appendDecimalLongitudeTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(1.1);
        point.setLongitude(123.1);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LONGITUDE_DECIMAL.getValue(naf), CoreMatchers.is("123.100"));
    }
    
    @Test
    public void appendDecimalLongitude2Test() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(1.1);
        point.setLongitude(12.123);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LONGITUDE_DECIMAL.getValue(naf), CoreMatchers.is("012.123"));
    }
    
    @Test
    public void appendPositiveDecimalLongitudeTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(1.0);
        point.setLongitude(123.45678);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LONGITUDE_DECIMAL.getValue(naf), CoreMatchers.is("123.457"));
    }
    
    @Test
    public void appendNegativeDecimalLongitudeTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(1.0);
        point.setLongitude(-123.45678);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LONGITUDE_DECIMAL.getValue(naf), CoreMatchers.is("-123.457"));
    }
    
    @Test
    public void appendNegativeDecimalLongitude2Test() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());
        
        MovementPoint point = new MovementPoint();
        point.setLatitude(1.0);
        point.setLongitude(-12.34567);
        movement.setPosition(point);
        
        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");
        
        assertThat(NafCode.LONGITUDE_DECIMAL.getValue(naf), CoreMatchers.is("-012.346"));
    }

    @Test
    public void appendIrcsWithDashTest() {
        MovementType movement = new MovementType();
        movement.setPositionTime(new Date());

        MovementPoint point = new MovementPoint();
        point.setLatitude(1.0);
        point.setLongitude(-12.34567);
        movement.setPosition(point);

        movement.setIrcs("ABC-1234");

        movement.setMovementType(MovementTypeType.POS);
        ReportType report = new ReportType();
        report.setRecipient("SWE");
        report.setMovement(movement);
        String naf = NafMessageRequestMapper.mapToVMSMessage(report, "UNK");

        assertThat(NafCode.RADIO_CALL_SIGN.getValue(naf), CoreMatchers.is("ABC1234"));
    }
}