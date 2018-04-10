/**
 * Copyright (c) 2018 OGN, All Rights Reserved.
 */

package org.ogn.gateway.plugin.asterix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.ogn.commons.beacon.AircraftBeacon;
import org.ogn.commons.beacon.AircraftBeaconWithDescriptor;
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

	private static final Logger	LOG		= LoggerFactory.getLogger(AsterixForwarder.class);

	private static final String	VERSION	= "0.0.1";

	private List<InetAddress>	broadCastAddresses;
	private DatagramSocket		socket;
	private SocketAddress		serverAddress;
	private DatagramPacket		datagram;

	private static class MulticastPublisher {
		private DatagramSocket	socket;
		private InetAddress		group;
		private byte[]			buf;

		public void multicast(String multicastMessage) throws IOException {
			socket = new DatagramSocket();
			group = InetAddress.getByName("230.0.0.0");
			buf = multicastMessage.getBytes();

			final DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
			socket.send(packet);
			socket.close();
		}
	}

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

			broadCastAddresses = listAllBroadcastAddresses();

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
		send(new AircraftBeaconWithDescriptor(beacon, descriptor));
	}

	private static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
		final List<InetAddress> broadcastList = new ArrayList<>();
		final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			final NetworkInterface networkInterface = interfaces.nextElement();

			if (networkInterface.isLoopback() || !networkInterface.isUp()) {
				continue;
			}

			networkInterface.getInterfaceAddresses().stream().map(InterfaceAddress::getBroadcast)
					.filter(Objects::nonNull).forEach(broadcastList::add);
		}
		return broadcastList;
	}

	private void broadcastBeacon(String broadcastMessage, InetAddress address) throws IOException {
		// socket = new DatagramSocket();
		socket.setBroadcast(true);

		final byte[] buffer = broadcastMessage.getBytes();

		final DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 4445);
		socket.send(packet);
		// socket.close();
	}

	/*
	 * private static int swap(int value) { int b1 = (value >> 0) & 0xff; int b2 = (value >> 8) & 0xff; int b3 = (value
	 * >> 16) & 0xff; int b4 = (value >> 24) & 0xff; return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0; } private static
	 * short swap(short value) { int b1 = value & 0xff; int b2 = (value >> 8) & 0xff; return (short) (b1 << 8 | b2 <<
	 * 0); }
	 */

	private void send(AircraftBeaconWithDescriptor beacon) {

		if (LOG.isTraceEnabled())
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