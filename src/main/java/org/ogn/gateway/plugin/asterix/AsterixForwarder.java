/**
 * Copyright (c) 2018 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.asterix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftDescriptor;
import org.ogn.commons.beacon.forwarder.OgnAircraftBeaconForwarder;
import org.ogn.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ASTERIX plug-in for OGN gateway
 * 
 * @author wbuczak
 */
public class AsterixForwarder implements OgnAircraftBeaconForwarder {

	private static final Logger						LOG			= LoggerFactory.getLogger(AsterixForwarder.class);

	private static final String						VERSION		= "0.0.1";

	private DatagramSocket							socket;
	private SocketAddress							serverAddress;
	private DatagramPacket							datagram;

	private final Map<String, AircraftDescriptor>	descriptors	= new HashMap<>();

	/**
	 * default constructor
	 */
	public AsterixForwarder() {
		// NOP
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
		return "converts OGN aircraft beacons to ASTERIX cat 62 and sends (UDP broadast/multicast)";
	}

	@Override
	public void init() {
		try {
			socket = new DatagramSocket();
			// serverAddress = new InetSocketAddress(InetAddress.getByName(FR24_SRV_NAME), FR24_SRV_PORT);
		} catch (final Exception e) {
			LOG.error("could not connect to FR24 server", e);
		}
	}

	@Override
	public void stop() {
		if (socket != null) {
			try {
				socket.close();
			} catch (final Exception ex) {
				LOG.warn("exception caught while trying to close a socket", ex);
			}
		}
	}

	@Override
	public void onBeacon(AircraftBeacon beacon, Optional<AircraftDescriptor> descriptor) {
		boolean sendDescriptor = false;

		if (descriptor.isPresent()) {

			if (!descriptors.containsKey(descriptor.get())) {
				descriptors.put(descriptor.get().getRegNumber(), descriptor.get());
				sendDescriptor = true;
			} else {
				final AircraftDescriptor prevDescr = descriptors.get(descriptor.get().getRegNumber());
				// send the descriptor update ONLY if it has changed
				if (!prevDescr.equals(descriptor)) {
					descriptors.put(descriptor.get().getRegNumber(), descriptor.get());
					sendDescriptor = true;
				}
			}

		} // if

		// update descriptor ONLY if needed
		if (sendDescriptor) {
			send(beacon, descriptor);
		} else {
			send(beacon, Optional.empty());
		}
	}

	/*
	 * private static int swap(int value) { int b1 = (value >> 0) & 0xff; int b2 = (value >> 8) & 0xff; int b3 = (value
	 * >> 16) & 0xff; int b4 = (value >> 24) & 0xff; return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0; } private static
	 * short swap(short value) { int b1 = value & 0xff; int b2 = (value >> 8) & 0xff; return (short) (b1 << 8 | b2 <<
	 * 0); }
	 */

	private void send(AircraftBeacon beacon, Optional<AircraftDescriptor> descriptor) {

		LOG.trace("sending beacon: {}", JsonUtils.toJson(beacon));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
		final DataOutputStream dos = new DataOutputStream(baos);

		try {

			// Add data
			/*
			 * dos.writeInt(swap((int) (Long.parseLong(beacon.getId(), 16) | (beacon.getAddressType().getCode() << 24) |
			 * (beacon.getAircraftType().getCode()) << 26)));// Extended Id dos.writeInt(swap((int)
			 * (System.currentTimeMillis() / 1000L))); // Unix // current // timestamp dos.writeInt(swap((int)
			 * (beacon.getLat() * 10000000))); // 1e-7 deg // (south?) dos.writeInt(swap((int) (beacon.getLon() *
			 * 10000000))); // 1e-7 deg // (west?) dos.writeShort(swap((short) beacon.getAlt())); // Altitude in meters
			 * dos.writeShort(swap((short) (beacon.getClimbRate() * 10))); // Climb // rate // (m/s) // * 10
			 * dos.writeShort(swap((short) ((beacon.getGroundSpeed() * 10) / 3.6))); // Speed // from // km/h // to //
			 * m/s // * // 10 dos.writeShort(swap((short) ((beacon.getTrack() * 360) / 65536))); // Heading: //
			 * 65536/360deg dos.writeShort(swap((short) ((beacon.getTurnRate() * 360) / 65536))); // TurnRate: //
			 * 65536/360deg // - // not // really // sure. // Seems // to // depend // on // flarm???
			 * dos.writeByte(beacon.getAircraftType().getCode()); // acftType dos.writeByte(beacon.getErrorCount()); //
			 * RxErr dos.writeByte(0); // accHor dos.writeByte(0); // accVer dos.writeByte(0); // movMode
			 * dos.writeByte(0); // Flag byte[] data = baos.toByteArray(); if (datagram == null) datagram = new
			 * DatagramPacket(data, data.length, serverAddress); else // reuse old object to reduce GC pressure
			 * datagram.setData(data); socket.send(datagram); LOG.debug("Data sent to FR24 ({} bytes)", +data.length);
			 */

			// } catch (final PortUnreachableException e) {
			// LOG.error("FR24 is not not listening (PortUnreachable)", e);
		} catch (final Exception e) {
			LOG.error("Could not send beacon to FR24", e);
		}
	}

}