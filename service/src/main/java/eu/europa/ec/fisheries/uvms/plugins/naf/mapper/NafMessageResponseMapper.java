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

import fish.focus.schema.exchange.movement.asset.v1.AssetId;
import fish.focus.schema.exchange.movement.asset.v1.AssetIdList;
import fish.focus.schema.exchange.movement.asset.v1.AssetIdType;
import fish.focus.schema.exchange.movement.v1.*;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.plugins.naf.constants.NafCode;
import eu.europa.ec.fisheries.uvms.plugins.naf.exception.PluginException;
import eu.europa.ec.fisheries.uvms.plugins.naf.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.Date;

/**
 **/
public class NafMessageResponseMapper {
	
	private NafMessageResponseMapper() {}
    
    private static final Logger LOG = LoggerFactory.getLogger(NafMessageResponseMapper.class);

    static String dateString;
    static String timeString;
    
    public static SetReportMovementType mapToMovementType(String nafMessage, String pluginName) throws PluginException {
        dateString = "";
        timeString = "";
		SetReportMovementType movementType = new SetReportMovementType();
		try {
			String decodedNafMessage = URLDecoder.decode(nafMessage, "UTF-8");
			if (!isValidMessage(decodedNafMessage)) {
				throw new PluginException("NAF message is not valid");
			}
			MovementBaseType movement = new MovementBaseType();
			movement.setComChannelType(MovementComChannelType.NAF);
			movement.setSource(MovementSourceType.NAF);

			for (NafCode nafCode : NafCode.values()) {
				if (nafCode.matches(decodedNafMessage)) {
					String value = nafCode.getValue(decodedNafMessage);
					mapEntry(nafCode, value, movement);
				}
			}

			if (movement.getFlagState() == null && NafCode.FROM.matches(decodedNafMessage)) {
			    movement.setFlagState(NafCode.FROM.getValue(decodedNafMessage));
			}
			mapDateTime(movement);
			movementType.setMovement(movement);
			movementType.setPluginType(PluginType.NAF);
			movementType.setPluginName(pluginName);
			movementType.setTimestamp(new Date());
		} catch (UnsupportedEncodingException e) {
			throw new PluginException(e.getMessage());
		}
		return movementType;
    }
    
    private static boolean isValidMessage(String message) {
    	String startRecord = NafCode.DELIMITER + NafCode.START_RECORD.getCode() + NafCode.DELIMITER;
    	String endRecord = NafCode.DELIMITER + NafCode.END_RECORD.getCode() + NafCode.DELIMITER;
    	return message.startsWith(startRecord) && message.endsWith(endRecord);
    	
    }

	static void mapEntry(NafCode code, String value, MovementBaseType movement) {
		switch (code) {
		case RADIO_CALL_SIGN:
			mapIRCS(value, movement);
			break;
		case TRIP_NUMBER:
			mapTripNumber(value, movement);
			break;
		case VESSEL_NAME:
			movement.setAssetName(value);
			break;
		case INTERNAL_REFERENCE_NUMBER:
			mapCFR(value, movement);
			break;
		case EXTERNAL_MARK:
			movement.setExternalMarking(value);
			break;
		case LATITUDE:
		    mapLatitude(movement, value);
		    break;
		case LATITUDE_DECIMAL:
			mapDecimalLatitude(movement, value);
			break;
		case LONGITUDE:
		    mapLongitude(movement, value);
		    break;
		case LONGITUDE_DECIMAL:
			mapDecimalLongitude(movement, value);
			break;
		case SPEED:
			mapSpeed(movement, value);
			break;
		case COURSE:
			movement.setReportedCourse(Double.parseDouble(value));
			break;
		case DATE:
			dateString = value;
			break;
		case TIME:
			timeString = value;
			break;
		case ACTIVITY:
			mapActivity(movement, value);
			break;
		case FLAG:
			movement.setFlagState(value);
			break;
		case TYPE_OF_MESSAGE:
		    MovementTypeType movementType = getTypeOfMessage(value);
		    movement.setMovementType(movementType);
			break;
		case TO:
		default:
			break;
		}
	}

    private static void mapTripNumber(String value, MovementBaseType movement) {
        try {
            Double tripNumber = Double.valueOf(value);
            movement.setTripNumber(tripNumber);
        } catch (NumberFormatException e) {
            LOG.error("Received malformed TN: {}", value);
        }
    }

    private static void mapActivity(MovementBaseType movement, String value) {
        MovementActivityType activity = new MovementActivityType();
        activity.setMessageType(MovementActivityTypeType.valueOf(value));
        movement.setActivity(activity);
    }

    static void mapSpeed(MovementBaseType movement, String value) {
        BigDecimal bd = BigDecimal.valueOf(Double.valueOf(value) / 10).setScale(4, RoundingMode.HALF_EVEN);
        double speed = bd.doubleValue();
        movement.setReportedSpeed(speed);
    }

    static void mapDateTime(MovementBaseType movement) {
        if ((dateString == null || dateString.isEmpty()) ||
                (timeString == null || timeString.isEmpty())) {
            return;
        }
        while (timeString.length() < 4) {
            timeString = "0" + timeString;
        }
        Date date = DateUtil.parseToUTCDateTime(dateString + " " + timeString + " UTC");
        LOG.debug("Time String: {}; Converted date: {}", dateString + " " + timeString + " UTC", date);
        movement.setPositionTime(date);
    }

    static void mapIRCS(String value, MovementBaseType movement) {
    	if (movement.getAssetId() == null) {
    		AssetId assetId = new AssetId();
    		movement.setAssetId(assetId);
    	}
        AssetIdList ircs = new AssetIdList();
        ircs.setIdType(AssetIdType.IRCS);
        ircs.setValue(value);
        movement.getAssetId().getAssetIdList().add(ircs);
        movement.setIrcs(value);
    }

    static void mapCFR(String value, MovementBaseType movement) {
    	if (movement.getAssetId() == null) {
    		AssetId assetId = new AssetId();
    		movement.setAssetId(assetId);
    	}
        AssetIdList cfr = new AssetIdList();
        cfr.setIdType(AssetIdType.CFR);
        cfr.setValue(value);
        movement.getAssetId().getAssetIdList().add(cfr);
        movement.setInternalReferenceNumber(value);
    }

    static MovementPoint getMovementPoint(MovementBaseType movement) {
        if (movement.getPosition() == null) {
            movement.setPosition(new MovementPoint());
        }
        return movement.getPosition();
    }

    private static void mapDecimalLongitude(MovementBaseType movement, String value) {
        MovementPoint pos = getMovementPoint(movement);
        pos.setLongitude(Double.valueOf(value));
    }

    private static void mapDecimalLatitude(MovementBaseType movement, String value) {
        MovementPoint pos = getMovementPoint(movement);
        pos.setLatitude(Double.valueOf(value));
    }

    private static void mapLongitude(MovementBaseType movement, String value) {
        MovementPoint pos = getMovementPoint(movement);
        double deg = (charToDouble(value.charAt(1)) * 100) + (charToDouble(value.charAt(2)) * 10) + charToDouble(value.charAt(3));
        double min = (charToDouble(value.charAt(4)) * 10) + charToDouble(value.charAt(5));
        double decimalDegrees = deg + (min / 60);
        BigDecimal bd = BigDecimal.valueOf(decimalDegrees).setScale(4, RoundingMode.HALF_EVEN);
        decimalDegrees = bd.doubleValue();
        if (value.charAt(0) == 'W') {
            decimalDegrees *= -1;
        }
        pos.setLongitude(decimalDegrees);
    }

    private static void mapLatitude(MovementBaseType movement, String value) {
        MovementPoint pos = getMovementPoint(movement);
        double deg = (charToDouble(value.charAt(1)) * 10) + charToDouble(value.charAt(2));
        double min = (charToDouble(value.charAt(3)) * 10) + charToDouble(value.charAt(4));
        double decimalDegrees = deg + (min / 60);
        BigDecimal bd = BigDecimal.valueOf(decimalDegrees).setScale(4, RoundingMode.HALF_EVEN);
        decimalDegrees = bd.doubleValue();
        if (value.charAt(0) == 'S') {
            decimalDegrees *= -1;
        }
        pos.setLatitude(decimalDegrees);
    }
    
    private static double charToDouble(char val) {
        String str = Character.toString(val);
        return Double.valueOf(str);
    }

    private static MovementTypeType getTypeOfMessage(String value) {
        try {
            if (value.length() > 3) {
                value = value.substring(0, 3);
            }
            return MovementTypeType.valueOf(value);
        } catch (Exception e) {
            LOG.warn("Could not map type of message: {}", value);
            return MovementTypeType.POS;
        }
    }
}