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
package fish.focus.uvms.plugins.naf.mapper;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import fish.focus.schema.exchange.common.v1.ReportType;
import fish.focus.schema.exchange.movement.asset.v1.AssetIdList;
import fish.focus.schema.exchange.movement.asset.v1.AssetIdType;
import fish.focus.schema.exchange.movement.v1.MovementPoint;
import fish.focus.schema.exchange.movement.v1.MovementSourceType;
import fish.focus.schema.exchange.movement.v1.MovementType;
import fish.focus.schema.exchange.movement.v1.MovementTypeType;
import fish.focus.uvms.plugins.naf.constants.NafCode;

/**
 **/
public class NafMessageRequestMapper {
	
	private NafMessageRequestMapper() {}
    
    private static final NumberFormat latFormatter;
    private static final NumberFormat longFormatter;

    static {
        latFormatter = NumberFormat.getInstance(Locale.ENGLISH);
        latFormatter.setRoundingMode(RoundingMode.HALF_UP);
        latFormatter.setMinimumFractionDigits(3);
        latFormatter.setMaximumFractionDigits(3);
        latFormatter.setMinimumIntegerDigits(2);
        longFormatter = NumberFormat.getInstance(Locale.ENGLISH);
        longFormatter.setRoundingMode(RoundingMode.HALF_UP);
        longFormatter.setMinimumFractionDigits(3);
        longFormatter.setMaximumFractionDigits(3);
        longFormatter.setMinimumIntegerDigits(3);
    }

    public static String mapToVMSMessage(ReportType report, String from) {
        MovementType movement = report.getMovement();
        StringBuilder naf = new StringBuilder();
        
        appendStartRecord(naf);
        
        // Actual data
        append(naf, NafCode.TO.getCode(), report.getRecipient());
        append(naf, NafCode.FROM.getCode(), from);
        append(naf, NafCode.TYPE_OF_MESSAGE.getCode(), movement.getMovementType().name());
        if (movement.getIrcs() != null) {
            append(naf, NafCode.RADIO_CALL_SIGN.getCode(), movement.getIrcs().replace("-", ""));
        }
        if (movement.getTripNumber() != null) {
            append(naf, NafCode.TRIP_NUMBER.getCode(), movement.getTripNumber());
        }
        append(naf, NafCode.VESSEL_NAME.getCode(), movement.getAssetName());
        append(naf, NafCode.FLAG.getCode(), movement.getFlagState() );
        if (movement.getInternalReferenceNumber() != null) {
            append(naf, NafCode.INTERNAL_REFERENCE_NUMBER.getCode(), movement.getInternalReferenceNumber());
        } else {
            appendAsset(naf, NafCode.INTERNAL_REFERENCE_NUMBER.getCode(), AssetIdType.CFR, movement);
        }
        append(naf, NafCode.EXTERNAL_MARK.getCode(), movement.getExternalMarking());
        if (!movement.getMovementType().equals(MovementTypeType.EXI)) {
            appendPosition(naf, movement);
            if (movement.getReportedSpeed() != null) {
                append(naf, NafCode.SPEED.getCode(), (int) (movement.getReportedSpeed() * 10));
            }
            if (movement.getReportedCourse() != null) {
                append(naf, NafCode.COURSE.getCode(), movement.getReportedCourse().intValue());
            }
        }
        append(naf, NafCode.DATE.getCode(), getDateString(movement));
        append(naf, NafCode.TIME.getCode(), getTimeString(movement));
        if (movement.getActivity() != null && movement.getActivity().getMessageType() != null) {
            append(naf, NafCode.ACTIVITY.getCode(), movement.getActivity().getMessageType().value());
        }
        
        appendEndRecord(naf);
        
        return naf.toString();
    }

    private static void appendEndRecord(StringBuilder naf) {
        naf.append(NafCode.END_RECORD.getCode());
        naf.append(NafCode.DELIMITER);
    }

    private static void appendStartRecord(StringBuilder naf) {
        naf.append(NafCode.DELIMITER);
        naf.append(NafCode.START_RECORD.getCode());
        naf.append(NafCode.DELIMITER);
    }

    private static void appendPosition(StringBuilder naf, MovementType movement) {
        MovementPoint position = movement.getPosition();
        append(naf, NafCode.LATITUDE_DECIMAL.getCode(), latFormatter.format(position.getLatitude()));
        append(naf, NafCode.LONGITUDE_DECIMAL.getCode(), longFormatter.format(position.getLongitude()));
    }

    static boolean appendAsset(StringBuilder naf, String nafCode, AssetIdType assetId, MovementType movement) {
        if (movement.getAssetId() != null) {
            for (AssetIdList assetIdList : movement.getAssetId().getAssetIdList()) {
                if (assetId.equals(assetIdList.getIdType())) {
                    append(naf, nafCode, assetIdList.getValue());
                    return true;
                }
            }
        }
        
        return false;
    }

    static String getTimeString(MovementType movement) {
        StringBuilder time = new StringBuilder();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(movement.getPositionTime());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            time.append(0);
        }
        time.append(hour);
        int min = calendar.get(Calendar.MINUTE);
        if (min < 10) {
            time.append("0");
        }
        time.append(min);
        return time.toString();
    }

    static String getDateString(MovementType movement) {
        StringBuilder date = new StringBuilder();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(movement.getPositionTime());
        date.append(calendar.get(Calendar.YEAR));
        int month = calendar.get(Calendar.MONTH) + 1;
        if (month < 10) {
            date.append("0");
        }
        date.append(month);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            date.append("0");
        }
        date.append(day);
        return date.toString();
    }
    
    private static void append(StringBuilder naf, String key, Number value) {
        if (value != null && Math.floor(value.doubleValue()) == value.doubleValue()) {
            append(naf, key, String.valueOf(value.intValue()));
        } else {
            append(naf, key, String.valueOf(value));
        }
    }
    
    static void append(StringBuilder naf, String key, String value) {
        if (value != null) {
            naf.append(key);
            naf.append(NafCode.SUBDELIMITER);
            naf.append(value);
            naf.append(NafCode.DELIMITER);
        }
    }

    static String getLatitudeString(Double coord) {
        StringBuilder sb = new StringBuilder();
        
        Double latitude = coord;
        if (latitude < 0) {
        	latitude = -latitude;
            sb.append("S");
        } else {
            sb.append("N");
        }
        
        int deg = (int) Math.floor(latitude);
        int min = (int)((latitude - deg) * 60);
        
        if (deg < 10) {
            sb.append(0);
        }
        sb.append(deg);
        if (min < 10) {
            sb.append(0);
        }
        sb.append(min);
        return sb.toString();
    }

    static String getLongitudeString(Double coord) {
        StringBuilder sb = new StringBuilder();
        
        Double longitude = coord;
        if (longitude < 0) {
        	longitude = -longitude;
            sb.append("W");
        } else {
            sb.append("E");
        }
        
        int deg = (int) Math.floor(longitude);
        int min = (int)((longitude - deg) * 60);
        
        if (deg < 100) {
            sb.append(0);
        }
        if (deg < 10) {
            sb.append(0);
        }
        sb.append(deg);
        if (min < 10) {
            sb.append(0);
        }
        sb.append(min);
        return sb.toString();
    }
}