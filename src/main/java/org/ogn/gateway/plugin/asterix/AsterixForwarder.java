/**
 * Copyright (c) 2018 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.asterix;

import java.util.Optional;

import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftBeaconWithDescriptor;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.forwarder.OgnAircraftBeaconForwarder;
import org.ogn.commons.udp.MulticastPublisher;
import org.ogn.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * ASTERIX plug-in for OGN gateway
 * 
 * @author wbuczak
 */
public class AsterixForwarder implements OgnAircraftBeaconForwarder {

	private static final Logger				LOG			= LoggerFactory.getLogger(AsterixForwarder.class);

	private static final String				VERSION		= "0.0.1";

	private static final MulticastPublisher	publisher	= new MulticastPublisher(1);

	@Configuration
	private static class Config {
		@Value("${ogn.gateway.asterix.multicast_group:#{systemProperties['OGN_GATEWAY_ASTERIX_MULTICAST_GROUP']}}")
		String multicastGroup;

	}

	/**
	 * default constructor
	 */
	public AsterixForwarder() {

	}

	@Override
	public String getName() {
		return "ASTERIX cat. 62 forwarder";
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public String getDescription() {
		return "converts OGN aircraft beacons to ASTERIX cat 62 and sends (UDP multicast))";
	}

	@Override
	public void init() {

	}

	@Override
	public void stop() {
		publisher.stop();
	}

	@Override
	public void onBeacon(AircraftBeacon beacon, Optional<AircraftDescriptor> descriptor) {
		send(new AircraftBeaconWithDescriptor(beacon, descriptor));
	}

	private void send(AircraftBeaconWithDescriptor beacon) {

		if (LOG.isTraceEnabled())
			LOG.trace("sending beacon: {}", JsonUtils.toJson(beacon));

		// publisher.send();

	}

	private byte[] convertToAsterix(AircraftBeaconWithDescriptor beacon) {
		// TODO Auto-generated method stub
		return new byte[1];
	}

}