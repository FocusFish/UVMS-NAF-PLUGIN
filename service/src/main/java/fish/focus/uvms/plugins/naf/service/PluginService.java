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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.schema.exchange.common.v1.AcknowledgeType;
import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.CommandType;
import fish.focus.schema.exchange.common.v1.CommandTypeType;
import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.common.v1.ReportType;
import fish.focus.schema.exchange.common.v1.ReportTypeType;
import fish.focus.schema.exchange.movement.v1.MovementPoint;
import fish.focus.schema.exchange.movement.v1.MovementType;
import fish.focus.schema.exchange.movement.v1.SetReportMovementType;
import fish.focus.schema.exchange.plugin.types.v1.EmailType;
import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.schema.exchange.plugin.v1.SetReportRequest;
import fish.focus.schema.exchange.service.v1.SettingListType;
import fish.focus.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import fish.focus.uvms.plugins.naf.StartupBean;
import fish.focus.uvms.plugins.naf.constants.NafCode;
import fish.focus.uvms.plugins.naf.constants.NafConfigKeys;
import fish.focus.uvms.plugins.naf.exception.PluginException;
import fish.focus.uvms.plugins.naf.mapper.NafMessageRequestMapper;
import fish.focus.uvms.plugins.naf.mapper.NafMessageResponseMapper;
import fish.focus.uvms.plugins.naf.message.NafMessageSenderBean;

/**
 **/
@Stateless
public class PluginService {
    
    @EJB
    private StartupBean startupBean;
    
    @EJB
    private NafMessageSenderBean sender;
    
    @EJB
    private ExchangeService exchangeService;

    private static final Logger LOG = LoggerFactory.getLogger(PluginService.class);

    @Inject
    @Metric(name = "naf_incoming", absolute = true)
    Counter nafIncoming;

    @Inject
    @Metric(name = "naf_outgoing", absolute = true)
    Counter nafOutgoing;

    @Inject
    MetricRegistry registry;

    /**
     *
     * @param reportRequest
     * @return
     */
    public AcknowledgeType setReport(SetReportRequest reportRequest) {
        AcknowledgeTypeType ackType = AcknowledgeTypeType.OK;
        ReportType report = reportRequest.getReport();
        LOG.debug(startupBean.getRegisterClassName() + ".report(" + report.getType().name() + ")");
        LOG.debug("timestamp: " + report.getTimestamp());
        MovementType movement = report.getMovement();
        String nafMessage = null;
        if (movement != null && ReportTypeType.MOVEMENT.equals(report.getType())) {
            MovementPoint pos = movement.getPosition();
            if (pos != null) {
                String from = startupBean.getSetting(NafConfigKeys.FROM_PARTY);
                nafMessage = NafMessageRequestMapper.mapToVMSMessage(report, from);
                LOG.info("Sending {}", nafMessage);
                try {
                    int response = sender.sendMessage(nafMessage, report.getRecipientInfo());
                    if (response != 200) {
                        LOG.error("Received error code {} when sending to {}", response, report.getRecipient());
                        ackType = AcknowledgeTypeType.NOK;
                        registry.counter("naf_outgoing_to_error", new Tag("to", report.getRecipient())).inc();
                    }
                } catch (PluginException e) {
                    LOG.error("Could not send message due to: {}", e.getMessage());
                    ackType = AcknowledgeTypeType.NOK;
                    registry.counter("naf_outgoing_to_error", new Tag("to", report.getRecipient())).inc();
                }
            }
            registry.counter("naf_outgoing_to", new Tag("to", report.getRecipient())).inc();
            nafOutgoing.inc();
        }
        return ExchangePluginResponseMapper.mapToAcknowledgeType(reportRequest.getReport().getLogId(), reportRequest.getReport().getUnsentMessageGuid(), ackType, nafMessage);
    }

    public void setMessageReceived(String message) throws PluginException {
        if (message != null) {
            SetReportMovementType movement = NafMessageResponseMapper.mapToMovementType(message, startupBean.getRegisterClassName());
            movement.setOriginalIncomingMessage(message);
            exchangeService.sendMovementReportToExchange(movement, "NAF");
            if (NafCode.FROM.matches(message)) {
                registry.counter("naf_incoming_from", new Tag("from", NafCode.FROM.getValue(message))).inc();
            }
            nafIncoming.inc();
        }
    }

    /**
     *
     * @param command
     * @return
     */
    public AcknowledgeTypeType setCommand(CommandType command) {
        LOG.info(startupBean.getRegisterClassName() + ".setCommand(" + command.getCommand().name() + ")");
        LOG.debug("timestamp: " + command.getTimestamp());
        PollType poll = command.getPoll();
        EmailType email = command.getEmail();
        if (poll != null && CommandTypeType.POLL.equals(command.getCommand())) {
            LOG.info("POLL: " + poll.getPollId());
        }
        if (email != null && CommandTypeType.EMAIL.equals(command.getCommand())) {
            LOG.info("EMAIL: subject=" + email.getSubject());
        }
        return AcknowledgeTypeType.OK;
    }

    /**
     * Set the config values for the naf
     *
     * @param settings
     * @return
     */
    public AcknowledgeTypeType setConfig(SettingListType settings) {
        LOG.info(startupBean.getRegisterClassName() + ".setConfig()");
        try {
            for (KeyValueType values : settings.getSetting()) {
                LOG.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
                startupBean.getSettings().put(values.getKey(), values.getValue());
            }
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            LOG.error("Failed to set config in {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }

    }

    /**
     * Start the naf. Use this to enable functionality in the naf
     *
     * @return
     */
    public AcknowledgeTypeType start() {
        LOG.info(startupBean.getRegisterClassName() + ".start()");
        try {
            startupBean.setIsEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            startupBean.setIsEnabled(Boolean.FALSE);
            LOG.error("Failed to start {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }

    }

    /**
     * Start the naf. Use this to disable functionality in the naf
     *
     * @return
     */
    public AcknowledgeTypeType stop() {
        LOG.info(startupBean.getRegisterClassName() + ".stop()");
        try {
            startupBean.setIsEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            startupBean.setIsEnabled(Boolean.TRUE);
            LOG.error("Failed to stop {}", startupBean.getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

}
