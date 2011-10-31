// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.*;

import java.nio.channels.*;

import org.apache.log4j.Logger;

import org.deltava.beans.*;
import org.deltava.beans.acars.ConnectionStats;
import org.deltava.beans.system.IPAddressInfo;

import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.ScopeInfoMessage;

import org.deltava.util.system.SystemData;

/**
 * An ACARS server connection.
 * @author Luke
 * @version 4.1
 * @since 1.0
 */

public class ACARSConnection implements Comparable<ACARSConnection>, ViewEntry {

	protected transient static final Logger log = Logger.getLogger(ACARSConnection.class);
	
	// Channels
	private TCPChannel _tcp;
	private UDPChannel _udp;
	
	// Voice settings
	private long _maxVoiceSeq;
	private boolean _voiceCapable;
	private boolean _voiceEcho;
	private int _warnings;

	private int _protocolVersion = 1;
	private int _clientVersion;
	private int _beta;
	
	private long _dispatcherID;
	private long _viewerID;
	
	private boolean _isDispatch;
	private boolean _isViewer;
	private boolean _isATC;
	
	// Dispatch service location/range
	private GeoLocation _loc;
	private int _range;
	
	// Radar scope range
	private ScopeInfoMessage _scope;

	// Connection information
	private final long _id;
	private Pilot _userInfo;
	private UserData _userData;
	private IPAddressInfo _addrInfo;
	private InfoMessage _fInfo;
	private PositionMessage _pInfo;
	private int _updateInterval = 5000;
	private boolean _isUserBusy;
	private boolean _isUserHidden;
	private boolean _isMuted;

	// Activity monitors
	private final long _startTime = System.currentTimeMillis();
	private long _timeOffset;

	// MP field
	private final int _maxDistance = SystemData.getInt("mp.max_range", 40);

	/**
	 * Creates a new ACARS connection.
	 * @param cid the connection ID
	 * @param sc the TCP/IP SocketChannel
	 */
	public ACARSConnection(long cid, SocketChannel sc) {
		super();
		_id = cid;

		// Get a write selector
		try {
			long startTime = System.currentTimeMillis();
			_tcp = new TCPChannel(cid, sc);
			
			// Check execution time
			long execTime = _tcp.getLastActivityTime() - startTime;
			if (execTime > 1250)
				log.warn("Excessive connect time - " + execTime + "ms");
		} catch (IOException ie) {
			// Log our error and shut the connection
			log.error("Cannot set non-blocking I/O from " + sc.socket().getRemoteSocketAddress());
			try {
				sc.close();
			} catch (Exception e) {
				// empty
			}
		}
	}
	
	
	/**
	 * Connects the UDP socket for voice communications.
	 * @param dc the Channel to write on
	 * @param srcAddr the source SocketAddress
	 */
	public void enableVoice(DatagramChannel dc, InetSocketAddress srcAddr) {
		if (isVoiceEnabled()) {
			boolean isNew = !srcAddr.toString().equals(_udp.getAddress());
			if (isNew) {
				_udp.setRemoteAddress(srcAddr);
				log.warn("Switched voice source address for " + getUserID() + " to " + getRemoteVoiceAddr());
			}
				
			return;
		}
		
		try {
			_udp = new UDPChannel(_id, dc, srcAddr);
			_isMuted = false;
		} catch (IOException ie) {
			log.error("Error creating Voice socket for " + getUserID() + " - " + ie.getMessage(), ie);
		}
	}

	/**
	 * Closes the UDP pseudo-connection and disables voice.
	 */
	public void disableVoice() {
		if (_udp != null)
			_udp.close();
		
		_udp = null;
	}
	
	/**
	 * Closes the connection.
	 */
	public void close() {
		disableVoice();
		_tcp.close();
	}

	public boolean equals(Object o2) {
		return (o2 instanceof ACARSConnection) ? (_id == ((ACARSConnection) o2)._id) : false;
	}
	
	public ConnectionStats getTCPStatistics() {
		return _tcp.getStatistics();
	}
	
	public ConnectionStats getUDPStatistics() {
		return (_udp == null) ? null : _udp.getStatistics();
	}

	SocketChannel getChannel() {
		return _tcp.getChannel();
	}

	public int getFlightID() {
		return (_fInfo == null) ? 0 : _fInfo.getFlightID();
	}

	public long getID() {
		return _id;
	}
	
	public IPAddressInfo getAddressInfo() {
		return _addrInfo;
	}

	public InfoMessage getFlightInfo() {
		return _fInfo;
	}

	public PositionMessage getPosition() {
		return _pInfo;
	}
	
	public ScopeInfoMessage getScope() {
		return _scope;
	}
	
	public GeoLocation getMPLocation() {
		return (_scope != null) ? _scope : _pInfo;
	}
	
	public int getMPRange() {
		if (_scope != null) 
			return _scope.getRange();
		else if (getIsMP())
			return _maxDistance;
		else
			return -1;
	}
	
	public boolean getUserBusy() {
		return _isUserBusy;
	}

	public boolean getUserHidden() {
		return _isUserHidden;
	}
	
	public boolean getMuted() {
		return _isMuted;
	}
	
	public int getWarnings() {
		return _warnings;
	}
	
	public boolean getIsMP() {
		return (_fInfo != null) && (_fInfo.getLivery() != null);
	}

	public long getLastActivity() {
		return _tcp.getLastActivityTime();
	}
	
	public int getProtocolVersion() {
		return _protocolVersion;
	}

	public boolean getIsViewer() {
		return _isViewer;
	}
	
	public boolean getIsATC() {
		return _isATC;
	}
	
	public boolean getIsDispatch() {
		return _isDispatch;
	}
	
	public long getDispatcherID() {
		return _dispatcherID;
	}
	
	public long getViewerID() {
		return _viewerID;
	}
	
	public GeoLocation getLocation() {
		return _isDispatch ? _loc : _pInfo;
	}
	
	public int getDispatchRange() {
		return _range;
	}

	public int getClientVersion() {
		return _clientVersion;
	}
	
	public int getBeta() {
		return _beta;
	}
	
	public String getVersion() {
		StringBuilder buf = new StringBuilder();
		if (_isDispatch)
			buf.append("Dispatch ");
		else if (_isATC)
			buf.append("ATC ");
		
		buf.append("Build ");
		buf.append(_clientVersion);
		if (_beta > 0) {
			buf.append(" Beta ");
			buf.append(_beta);
		}

		return buf.toString();
	}

	public long getStartTime() {
		return _startTime;
	}
	
	public long getVoiceSequence() {
		return _maxVoiceSeq;
	}
	
	public int getUpdateInterval() {
		return _updateInterval;
	}

	public String getRemoteAddr() {
		return _tcp.getRemoteAddress();
	}
	
	public String getRemoteVoiceAddr() {
		return _udp.getAddress().substring(1);
	}

	public String getRemoteHost() {
		return _tcp.getRemoteHost();
	}
	
	public long getTimeOffset() {
		return _timeOffset;
	}

	public Pilot getUser() {
		return _userInfo;
	}

	public UserData getUserData() {
		return _userData;
	}

	public String getUserID() {
		return isAuthenticated() ? _userInfo.getPilotCode() : getRemoteAddr();
	}

	public boolean isAuthenticated() {
		return (_userInfo != null);
	}
	
	public boolean isVoiceCapable() {
		return _voiceCapable;
	}
	
	public boolean isVoiceEnabled() {
		return (_udp != null);
	}
	
	public boolean isVoiceEcho() {
		return _voiceEcho;
	}

	public void setFlightInfo(InfoMessage msg) {
		_fInfo = msg;
	}

	public void setPosition(PositionMessage msg) {
		_pInfo = msg;
	}
	
	public void setScope(ScopeInfoMessage msg) {
		_scope = msg;
	}
	
	public void setProtocolVersion(int pv) {
		_protocolVersion = Math.max(_protocolVersion, pv);
	}

	public void setClientVersion(int ver) {
		_clientVersion = Math.max(1, ver);
	}
	
	public void setBeta(int beta) {
		_beta = Math.max(0, beta);
	}
	
	public void setIsViewer(boolean isViewer) {
		_isViewer = isViewer;
	}
	
	public void setIsATC(boolean isATC) {
		_isATC = isATC;
	}

	public void setIsDispatch(boolean isDispatch) {
		_isDispatch = isDispatch;
	}
	
	public void setMuted(boolean isMute) {
		_isMuted = isMute;
	}
	
	public void setDispatcherID(long conID) {
		_dispatcherID = conID;
	}
	
	public void setViewerID(long conID) {
		_viewerID = conID;
	}	
	
	public void setWarnings(int warns) {
		_warnings = Math.max(0, warns);
	}
	
	public void setTimeOffset(long ofs) {
		_timeOffset = ofs;
	}

	public void setUser(Pilot p) {
		_userInfo = p;
	}

	public void setUserBusy(boolean isBusy) {
		_isUserBusy = isBusy;
	}

	public void setUserHidden(boolean isHidden) {
		_isUserHidden = isHidden;
	}
	
	public void setUserLocation(UserData ud) {
		_userData = ud;
	}
	
	public void setAddressInfo(IPAddressInfo addrInfo) {
		_addrInfo = addrInfo;
	}
	
	public void setUpdateInterval(int interval) {
		_updateInterval = Math.min(60000, Math.max(250, interval));
	}
	
	public void setDispatchRange(GeoLocation loc, int range) {
		_loc = loc;
		_range = Math.max(0, range);
	}
	
	public void setVoiceCapable(boolean voiceOK) {
		_voiceCapable = voiceOK;
	}
	
	public void setVoiceEcho(boolean voiceEcho) {
		_voiceEcho = voiceEcho;
	}
	
	public void setVoiceSequence(long seq) {
		_maxVoiceSeq = Math.max(seq, _maxVoiceSeq);
	}

	public String getRowClassName() {
		if (_isViewer)
			return "opt3";
		else if (_isDispatch)
			return "opt2";
		
		return null;
	}
	
	public int hashCode() {
		return Long.valueOf(_id).hashCode();
	}
	
	public int compareTo(ACARSConnection c2) {
		if (!isAuthenticated())
			return -1;
		else if (!c2.isAuthenticated())
			return 1;

		Pilot usr = c2.getUser();
		return Integer.valueOf(_userInfo.getPilotNumber()).compareTo(Integer.valueOf(usr.getPilotNumber()));
	}

	/**
	 * Reads a control message from the connection.
	 * @return a message
	 * @throws IOException if an I/O error occurs
	 */
	String read() throws IOException {
		return _tcp.read();
	}
	
	/**
	 * Logs reading a voice packet.
	 * @param bytes the number of bytes
	 */
	public void logVoice(int bytes) {
		_udp.read(bytes);
	}
	
	/**
	 * Queues a control message to be written.
	 * @param msg the message text
	 */
	public void write(String msg) {
		_tcp.queue(msg);
	}
	
	/**
	 * Queues a data packet to be written.
	 * @param data the voice packet
	 */
	public void write(byte[] data) {
		_udp.queue(data);
	}
}