// Copyright 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2023 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.beans;

import java.io.*;
import java.net.*;

import java.nio.channels.*;

import org.apache.logging.log4j.*;

import org.deltava.beans.*;
import org.deltava.beans.acars.*;
import org.deltava.beans.econ.EliteStatus;
import org.deltava.beans.system.IPBlock;

import org.deltava.acars.message.*;
import org.deltava.acars.message.dispatch.ScopeInfoMessage;

import org.deltava.util.*;
import org.deltava.util.system.SystemData;

/**
 * An ACARS server connection.
 * @author Luke
 * @version 11.1
 * @since 1.0
 */

public class ACARSConnection implements Comparable<ACARSConnection>, RemoteAddressBean, ViewEntry, ClientVersion, Closeable {

	protected transient static final Logger log = LogManager.getLogger(ACARSConnection.class);
	
	// Channels
	private TCPChannel _tcp;
	private UDPChannel _udp;
	
	// Voice settings
	private long _maxVoiceSeq;
	private boolean _voiceCapable;
	private boolean _voiceEcho;
	private int _warningScore;

	private int _protocolVersion = 1;
	private int _version;
	private int _clientBuild;
	private int _beta;
	
	private long _dispatcherID;
	
	private boolean _isDispatch;
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
	private EliteStatus _eliteStatus;
	
	private IPBlock _addrInfo;
	private InfoMessage _fInfo;
	private PositionMessage _pInfo;
	private int _updateInterval = 5000;
	private boolean _isUserBusy;
	private boolean _isUserHidden;
	private boolean _isMuted;

	// Activity monitors
	private final long _startTime = System.currentTimeMillis();
	private long _timeOffset;

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
				log.warn("Excessive connect time - {}ms", Long.valueOf(execTime));
		} catch (IOException ie) {
			// Log our error and shut the connection
			try {
				log.error("Cannot set non-blocking I/O from {}", sc.getRemoteAddress());
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
	 * @return TRUE if voice source address switched, otherwise FALSE
	 */
	public boolean enableVoice(DatagramChannel dc, InetSocketAddress srcAddr) {
		if (isVoiceEnabled()) {
			boolean isNew = !NetworkUtils.getSourceAddress(srcAddr).equals(_udp.getAddress());
			if (isNew) {
				_udp.setRemoteAddress(srcAddr, dc);
				log.warn("Switched voice source address for {} to {}", getUserID(), getVoiceSourceAddr());
				return true;
			}
				
			return false;
		}
		
		try {
			_udp = new UDPChannel(_id, dc, srcAddr);
			_isMuted = false;
		} catch (IOException ie) {
			log.atError().withThrowable(ie).log("Error creating Voice socket for {} - {}", getUserID(), ie.getMessage());
		}
		
		return false;
	}

	/**
	 * Closes the UDP pseudo-connection and disables voice.
	 */
	public void disableVoice() {
		if (_udp != null)
			_udp.close();
		
		_udp = null;
	}
	
	@Override
	public void close() {
		disableVoice();
		_tcp.close();
	}

	@Override
	public boolean equals(Object o2) {
		return (o2 instanceof ACARSConnection ac2) ? (_id == ac2._id) : false;
	}
	
	public ConnectionStats getTCPStatistics() {
		return _tcp.getStatistics();
	}
	
	public ConnectionStats getUDPStatistics() {
		return (_udp == null) ? null : _udp.getStatistics();
	}

	/**
	 * Registers this channel with a Selector.
	 * @param s the Selector to register with
	 * @throws ClosedChannelException if the registration failed
	 */
	void register(Selector s) throws ClosedChannelException {
		SocketChannel sc = _tcp.getChannel();
		if (sc.isOpen() && (sc.keyFor(s) == null))
			sc.register(s, SelectionKey.OP_READ);
	}
	
	public int getFlightID() {
		return (_fInfo == null) ? 0 : _fInfo.getFlightID();
	}

	public long getID() {
		return _id;
	}
	
	public IPBlock getAddressInfo() {
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
		return (_scope != null) ? _scope.getRange() : -1;
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
	
	public int getWarningScore() {
		return _warningScore;
	}

	public long getLastActivity() {
		return _tcp.getLastActivityTime();
	}
	
	public int getProtocolVersion() {
		return _protocolVersion;
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
	
	public GeoLocation getLocation() {
		return _isDispatch ? _loc : _pInfo;
	}
	
	public int getRange() {
		return _range;
	}

	@Override
	public int getBeta() {
		return _beta;
	}
	
	@Override
	public int getClientBuild() {
		return _clientBuild;
	}
	
	@Override
	public ClientType getClientType() {
		if (_isDispatch)
			return ClientType.DISPATCH;
		if (_isATC)
			return ClientType.ATC;
		
		return ClientType.PILOT;
	}
	
	@Override
	public int getVersion() {
		return _version;
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

	@Override
	public String getRemoteAddr() {
		return _tcp.getRemoteAddr();
	}
	
	public String getDataSourceAddr() {
		return _tcp.getAddress();
	}
	
	public String getVoiceSourceAddr() {
		return _udp.getAddress();
	}

	@Override
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
	
	public EliteStatus getEliteStatus() {
		return _eliteStatus;
	}

	public String getUserID() {
		if (!isAuthenticated())
			return getRemoteAddr();
		
		String pCode = _userInfo.getPilotCode();
		return StringUtils.isEmpty(pCode) ? String.valueOf(_userInfo.getID()) : pCode;
	}

	public boolean isAuthenticated() {
		return (_userInfo != null);
	}
	
	public Compression getCompression() {
		return _tcp.getCompression();
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
	
	public void setCompression(Compression c) {
		_tcp.setCompression(c);
	}

	public void setClientBuild(int bld, int beta) {
		_clientBuild = Math.max(1, bld);
		if (beta < Integer.MAX_VALUE)
			_beta = Math.max(0, beta);
	}
	
	public void setVersion(int ver) {
		_version = Math.max(1, ver);
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
	
	public void setWarningScore(int score) {
		_warningScore = Math.max(0, score);
	}
	
	public void setTimeOffset(long ofs) {
		_timeOffset = ofs;
	}

	public void setUser(Pilot p) {
		if ((_userInfo != null) && (p != null) && (p.getID() != _userInfo.getID()))
			throw new IllegalArgumentException("Connection " + _id + " owned by " + _userInfo.getID() + ", attempting to transfer to " + p.getID());
				
		_userInfo = p;
	}
	
	public void setEliteStatus(EliteStatus es) {
		_eliteStatus = es;
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
	
	public void setAddressInfo(IPBlock addrInfo) {
		_addrInfo = addrInfo;
	}
	
	public void setUpdateInterval(int interval) {
		_updateInterval = Math.min(60000, Math.max(250, interval));
	}
	
	public void setRange(GeoLocation loc, int range) {
		_loc = loc;
		if (_isDispatch || _isATC)
			_range = Math.max(0, range);
		else
			_range = Math.max(0, Math.min(range, SystemData.getInt("mp.max_range", 40)));
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

	@Override
	public String getRowClassName() {
		return (_isDispatch) ? "opt2" : null;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(_id);
	}
	
	@Override
	public int compareTo(ACARSConnection c2) {
		int tmpResult = Boolean.compare(isAuthenticated(), c2.isAuthenticated());
		return (tmpResult == 0) ? Integer.compare(_userInfo.getPilotNumber(), c2.getUser().getPilotNumber()) : tmpResult;
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