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
package eu.europa.ec.fisheries.uvms.plugins.naf.message;

import fish.focus.schema.exchange.movement.v1.RecipientInfoType;
import eu.europa.ec.fisheries.uvms.plugins.naf.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.naf.constants.NafConfigKeys;
import eu.europa.ec.fisheries.uvms.plugins.naf.exception.PluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 **/
@LocalBean
@Stateless
public class NafMessageSenderBean {

    private static final Logger LOG = LoggerFactory.getLogger(NafMessageSenderBean.class);
    
    @EJB
    private StartupBean startupBean;
    
    private int connectTimeout = 30000;
    private int readTimeout = 30000;
    
    public int sendMessage(String message, List<RecipientInfoType> recepientInfo) throws PluginException {
        
        String connectTimeoutString = startupBean.getSetting(NafConfigKeys.CONNECT_TIMEOUT);
        if (connectTimeoutString != null) {
            connectTimeout = Integer.valueOf(connectTimeoutString);
        }
        
        String readTimeoutString = startupBean.getSetting(NafConfigKeys.READ_TIMEOUT);
        if (readTimeoutString != null) {
            readTimeout = Integer.valueOf(readTimeoutString);
        }
        
        String useProxy = startupBean.getSetting(NafConfigKeys.USE_PROXY);
        if (useProxy != null && "true".equalsIgnoreCase(useProxy)) {
            return sendUsingProxy(message, recepientInfo);
        } else {
            return sendUsingLocalStore(message, recepientInfo);
        }
    }

    int sendUsingLocalStore(String message, List<RecipientInfoType> recepientInfo) throws PluginException {
        int responseCode = -1;

        String endpoint = extractEndpoint(recepientInfo);
        if (endpoint == null || endpoint.isEmpty()) {
            return responseCode;
        }
        
        try {
            String location = endpoint.replace("#MESSAGE#", URLEncoder.encode(message, CHARACTER_CODING) + "\n\r");
            
            URL url = new URL(location);
            HttpURLConnection urlc;
            if (url.getProtocol().equals("http")) {
                urlc = (HttpURLConnection) url.openConnection();
            } else {
                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new VMSHostNameVerifier());
                urlc = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            }
            urlc.setConnectTimeout(connectTimeout);
            urlc.setReadTimeout(readTimeout);
            responseCode = urlc.getResponseCode();
        } catch (MalformedURLException ex) {
            LOG.error("Malformed URL: {}", endpoint);
            throw new PluginException(ex.getMessage());
        } catch (IOException ex) {
            LOG.error("No response from: {}", endpoint);
            throw new PluginException(ex.getMessage());
        } catch (Exception e) {
            LOG.error("Could not send message {} to endpoint {}", message, endpoint, e);
            throw new PluginException(e.getMessage());
        }
        return responseCode;
    }

    String extractEndpoint(List<RecipientInfoType> recepientInfo) {
        for (RecipientInfoType info : recepientInfo) {
            if (info.getKey().contains("NAF") && info.getValue() != null) {
                return info.getValue();
            }
        }
        return null;
    }

    private int sendUsingProxy(String message, List<RecipientInfoType> recepientInfo) throws PluginException {
        int responseCode = -1;
        
        String proxy = startupBean.getSetting(NafConfigKeys.PROXY);
        String proxyPort = startupBean.getSetting(NafConfigKeys.PROXY_PORT);
        String endpoint = extractEndpoint(recepientInfo);
        if (endpoint == null || endpoint.isEmpty()) {
            return responseCode;
        }
        
        try {
            Proxy httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy, Integer.parseInt(proxyPort)));
            String location = endpoint.replace("#MESSAGE#", URLEncoder.encode(message, CHARACTER_CODING) + "\n\r");
            URL url = new URL(location);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(httpProxy);
            connection.setRequestMethod("GET");
            connection.setReadTimeout(readTimeout);
            connection.setConnectTimeout(connectTimeout);
            responseCode = connection.getResponseCode();
        } catch (MalformedURLException ex) {
            LOG.error("Malformed URL: {}", proxy);
            throw new PluginException(ex.getMessage());
        } catch (IOException ex) {
            LOG.error("No response from: {}", proxy);
            throw new PluginException(ex.getMessage());
        }
        return responseCode;
    }
    static final String CHARACTER_CODING = "ISO-8859-1";

    static class VMSHostNameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}