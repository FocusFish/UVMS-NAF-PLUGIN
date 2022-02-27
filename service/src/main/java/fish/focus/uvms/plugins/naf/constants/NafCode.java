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
package fish.focus.uvms.plugins.naf.constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum NafCode {
	START_RECORD("SR"),
    END_RECORD("ER"),
    FROM("FR"),
    TO("AD"),
    TYPE_OF_MESSAGE("TM"),
    DATE("DA"),
    TIME("TI"),
    INTERNAL_REFERENCE_NUMBER("IR"),
    FLAG("FS"),
    RADIO_CALL_SIGN("RC"),
    VESSEL_NAME("NA"),
    EXTERNAL_MARK("XR"),
    LATITUDE_DECIMAL("LT"),
    LONGITUDE_DECIMAL("LG"),
    SPEED("SP"),
    COURSE("CO"),
    TEST_RECORD("TEST"),
    ACTIVITY("AC"),
    IMO_NUMBER("IM"),
    TRIP_NUMBER("TN"),
    LATITUDE("LA"),
    LONGITUDE("LO");
	
    public static final String DELIMITER = "//";
    public static final String SUBDELIMITER = "/";

	private String code;
	private Pattern pattern;

	private NafCode(String code) {
		this.code = code;
		this.pattern = Pattern.compile(getPattern());
	}

	private String getPattern() {
		// '//CODE/([^/]+)//'
		return DELIMITER + code + SUBDELIMITER + "([^" + SUBDELIMITER + "]+)" + DELIMITER;
	}
	
	public String getCode() {
		return code;
	}

	public boolean matches(String nafMessage) {
		Matcher matcher = pattern.matcher(nafMessage);
		return matcher.find();
	}

    public String getValue(String nafMessage) {
    	Matcher matcher = pattern.matcher(nafMessage);
    	matcher.find();
    	return matcher.group(1);
    }
}