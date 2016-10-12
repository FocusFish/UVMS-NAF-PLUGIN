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
package eu.europa.ec.fisheries.uvms.plugins.naf.producer;

import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.jms.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.uvms.plugins.naf.constants.ModuleQueue;

@Singleton
public class PluginMessageProducer {

    @Resource(mappedName = ExchangeModelConstants.NO_PREFIX_EXCHANGE_MESSAGE_IN_QUEUE)
    private Queue exchangeQueue;

    @Resource(mappedName = ExchangeModelConstants.NO_PREFIX_PLUGIN_EVENTBUS)
    private Topic eventBus;

    @Resource(lookup = ExchangeModelConstants.NO_PREFIX_CONNECTION_FACTORY)
    private ConnectionFactory connectionFactory;

    private Connection connection = null;
    private Session session = null;

    final static Logger LOG = LoggerFactory.getLogger(PluginMessageProducer.class);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {
        try {
            connectQueue();

            TextMessage message = session.createTextMessage();
            message.setJMSDestination(requestMessage.getJMSReplyTo());
            message.setJMSCorrelationID(requestMessage.getJMSMessageID());
            message.setText(text);

            getProducer(session, requestMessage.getJMSReplyTo()).send(message);

        } catch (JMSException e) {
            LOG.error("[ Error when sending jms message. ] {}", e.getMessage());
            throw new JMSException(e.getMessage());
        } finally {
            disconnectQueue();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String sendModuleMessage(String text, ModuleQueue queue) throws JMSException {
        try {
            connectQueue();

            TextMessage message = session.createTextMessage();
            message.setText(text);

            switch (queue) {
                case EXCHANGE:
                    getProducer(session, exchangeQueue).send(message);
                    break;
                default:
                    LOG.error("[ Sending Queue is not implemented ]");
                    break;
            }

            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error("[ Error when sending data source message. ] {}", e.getMessage());
            throw new JMSException(e.getMessage());
        } finally {
            disconnectQueue();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String sendEventBusMessage(String text, String serviceName) throws JMSException {
        try {
            connectQueue();

            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);

            getProducer(session, eventBus).send(message);

            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error("[ Error when sending message. ] {0}", e.getMessage());
            throw new JMSException(e.getMessage());
        } finally {
            disconnectQueue();
        }
    }

    private void connectQueue() throws JMSException {
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    private void disconnectQueue() {
        try {
            connection.stop();
            connection.close();
        } catch (JMSException e) {
            LOG.error("[ Error when stopping or closing JMS queue. ] {}", e.getMessage(), e.getStackTrace());
        }
    }

    private javax.jms.MessageProducer getProducer(Session session, Destination destination) throws JMSException {
        javax.jms.MessageProducer producer = session.createProducer(destination);
        return producer;
    }
}