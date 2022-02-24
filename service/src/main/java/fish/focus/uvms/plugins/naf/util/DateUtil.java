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
package fish.focus.uvms.plugins.naf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DateUtil.class);

    private static final String DATE_TIME_FORMAT = "yyyyMMdd HHmm z";

    private DateUtil() {}

    private static Date parseToUTC(String format, String dateString) {
        Instant instant = ZonedDateTime.parse(dateString, DateTimeFormatter.ofPattern(format)).toInstant();
        Date dateTime = Date.from(instant);
        LOG.debug("DateTime: {}", dateTime);
        return dateTime;
    }
    
    public static Date parseToUTCDateTime(String dateString) {
        return parseToUTC(DATE_TIME_FORMAT, dateString);
    }
}