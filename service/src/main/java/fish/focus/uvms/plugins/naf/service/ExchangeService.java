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
package fish.focus.uvms.plugins.naf.service;

import fish.focus.schema.exchange.module.v1.ExchangeModuleMethod;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.schema.exchange.movement.v1.SetReportMovementType;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.plugins.naf.StartupBean;
import fish.focus.uvms.plugins.naf.producer.FailedReportsProducer;
import fish.focus.uvms.plugins.naf.producer.PluginToExchangeProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 **/
@Stateless
public class ExchangeService {

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeService.class);

    @Inject
    private StartupBean startupBean;

    @EJB
    private PluginToExchangeProducer producer;
    
    @Inject
    private FailedReportsProducer failedReportsProducer;

    @Asynchronous
    public void sendMovementReportToExchange(SetReportMovementType reportType, String userName) {

        String text = "";

        try {
            text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, userName, null, Instant.now(), PluginType.NAF, PluginType.NAF.value(), null);
        } catch (RuntimeException e) {
            LOG.error("Couldn't map movement to String");
            sendToErrorQueue(text);
            return;
        }


        try {
            producer.sendMessageToSpecificQueueWithFunction(text, producer.getDestination(), null, ExchangeModuleMethod.SET_MOVEMENT_REPORT.value(), null);
        } catch (JMSException e) {
            LOG.error("Couldn't send NAF positionReport to exchange. Trying again later");
            MovementBaseType tmp = reportType.getMovement();
            if(tmp != null) {
                startupBean.addCachedMovement(reportType);
            }
            sendToErrorQueue(text);
        }
    }

    private void sendToErrorQueue(String movement) {
        try {
            Map<String, String> props = new HashMap<>();
            props.put("source", "NAF");
            failedReportsProducer.sendModuleMessageWithProps(movement, null, props);
        } catch (JMSException e) {
            LOG.error("Couldn't send to error queue");
        }
    }
}