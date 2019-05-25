/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package ru.mgvk.kura.gateway;

import java.util.Collections;
import java.util.Map;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * The Class TimerOptions is responsible to contain all the Timer related
 * configurable options
 */
final class GatewayOptions {

    private static final String PROP_IP         = "ip";
    private static final String PROP_DATA       = "data";
    private static final String PROP_IN         = "inWiresCount";
    private static final String PROP_OUT        = "outWiresCount";
    private static final String PROP_PID        = "kura.service.pid";
    private static final String PROP_RE_IP_PORT = "remote_ip_port";
    private static final String PROP_LO_PORT    = "local_port";

    private static GatewayOptions      instance;
    private        Map<String, Object> properties;


    private GatewayOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(properties);
    }

    static GatewayOptions getInstance(Map<String, Object> properties) {
        if (instance == null) {
            instance = new GatewayOptions(properties);
        } else {
            instance.update(properties);
        }

        return instance;
    }

    private void update(Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(properties);
    }

    String getRemoteIP() {
        String       ip           = "";
        final Object remoteIpPort = this.properties.get(PROP_RE_IP_PORT);
        try {
            if (nonNull(remoteIpPort) && remoteIpPort instanceof String) {
                ip = ((String) remoteIpPort).split(":")[0];
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return ip;
    }

    Integer getRemotePort() {
        int          port         = 0;
        final Object remoteIpPort = this.properties.get(PROP_RE_IP_PORT);
        try {
            if (nonNull(remoteIpPort) && remoteIpPort instanceof String) {
                port = Integer.parseInt(((String) remoteIpPort).split(":")[1]);
            }
        } catch (Exception e) {
        }
        return port;
    }

    Integer getLocalPort() {
        int          port         = 0;
        final Object remoteIpPort = this.properties.get(PROP_LO_PORT);
        try {
            port = (int) remoteIpPort;
        } catch (Exception e) {
        }
        return port;
    }


    String getData() {
        String       data  = "";
        final Object _data = this.properties.get(PROP_DATA);
        if (nonNull(_data) && _data instanceof String) {
            data = (String) _data;
        }
        return data;
    }

    Integer getRecieverWiresCount() {
        Integer      res   = 0;
        final Object count = this.properties.get(PROP_IN);
        if (nonNull(count) && count instanceof Integer) {
            res = (Integer) count;
        }
        return res;
    }

    Integer getEmitterWiresCount() {
        Integer      res   = 0;
        final Object count = this.properties.get(PROP_OUT);
        if (nonNull(count) && count instanceof Integer) {
            res = (Integer) count;
        }
        return res;
    }

    public String getCurrentPID() {
        String       pid  = "";
        final Object data = this.properties.get(PROP_PID);
        if (nonNull(data) && data instanceof String) {
            pid = (String) data;
        }
        return pid;
    }

}