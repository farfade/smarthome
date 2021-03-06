/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.events;

/**
 * Abstract implementation of the {@link Event} interface.
 * 
 * @author Stefan Bußweiler - Initial contribution
 */
public abstract class AbstractEvent implements Event {

    private final String topic;

    private final String payload;

    private final String source;

    /**
     * Must be called in subclass constructor to create a new event.
     * 
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     */
    public AbstractEvent(String topic, String payload, String source) {
        this.topic = topic;
        this.payload = payload;
        this.source = source;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public String getSource() {
        return source;
    }

}
