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
package fish.focus.uvms.plugins.naf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.schema.exchange.movement.v1.SetReportMovementType;

/**
 **/
public abstract class PluginDataHolder {

    private Object lock = new Object();

    static final String PLUGIN_PROPERTIES = "plugin.properties";
    static final String PROPERTIES = "settings.properties";
    static final String CAPABILITIES = "capabilities.properties";

    private Properties nafApplicaitonProperties;
    private Properties nafProperties;
    private Properties nafCapabilities;

    private final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> capabilities = new ConcurrentHashMap<>();
    private final List<SetReportMovementType> failedSendList = new ArrayList<>();

    public ConcurrentMap<String, String> getSettings() {
        return settings;
    }

    public ConcurrentMap<String, String> getCapabilities() {
        return capabilities;
    }


    public void addCachedMovement(SetReportMovementType setReportMovementType) {
        synchronized(lock) {
            failedSendList.add(setReportMovementType);
        }
    }

    public List<SetReportMovementType>  getAndClearCachedMovementList() {
        List<SetReportMovementType>  tmp = new ArrayList<> ();
        synchronized(lock) {
            tmp.addAll(failedSendList);
            failedSendList.clear();
            return tmp;
        }
    }


    public Properties getPluginApplicaitonProperties() {
        return nafApplicaitonProperties;
    }

    public void setPluginApplicaitonProperties(Properties nafApplicaitonProperties) {
        this.nafApplicaitonProperties = nafApplicaitonProperties;
    }

    public Properties getPluginProperties() {
        return nafProperties;
    }

    public void setPluginProperties(Properties nafProperties) {
        this.nafProperties = nafProperties;
    }

    public Properties getPluginCapabilities() {
        return nafCapabilities;
    }

    public void setPluginCapabilities(Properties nafCapabilities) {
        this.nafCapabilities = nafCapabilities;
    }

}