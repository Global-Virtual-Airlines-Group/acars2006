// Copyright 2016, 2017, 2018 Global Virtual Airlines Group. All Rights Reserved.
package org.deltava.acars.message;

import org.deltava.beans.*;

/**
 * An ACARS System Information message bean.
 * @author Luke
 * @version 8.4
 * @since 6.4
 */

public class SystemInfoMessage extends AbstractMessage {
	
	private String _os;
	private String _clr;
	private String _net;
	private int _ram;
	
	private String _locale;
	private String _tz;
	
	private Simulator _sim = Simulator.UNKNOWN;
	private String _bridgeInfo;
	
	private String _cpu;
	private int _cpuSpeed;
	private int _sockets;
	private int _cores;
	private int _threads;
	private boolean _is64Bit;
	
	private String _gpu;
	private String _driver;
	private boolean _isSLI;
	private int _vram;
	private int _x;
	private int _y;
	private int _bpp;
	private int _screens;

	/**
	 * Creates the message.
	 * @param msgFrom the originating Pilot
	 */
	public SystemInfoMessage(Pilot msgFrom) {
		super(MessageType.SYSINFO, msgFrom);
	}

	public String getOSVersion() {
		return _os;
	}
	
	public String getCLRVersion() {
		return _clr;
	}
	
	public String getDotNETVersion() {
		return _net;
	}
	
	public String getLocale() {
		return _locale;
	}
	
	public Simulator getSimulator() {
		return _sim;
	}
	
	public String getBridgeInfo() {
		return _bridgeInfo;
	}
	
	public String getTimeZone() {
		return _tz;
	}
	
	public int getMemorySize() {
		return _ram;
	}
	
	public String getCPU() {
		return _cpu;
	}
	
	public int getCPUSpeed() {
		return _cpuSpeed;
	}
	
	public int getSockets() {
		return _sockets;
	}
	
	public int getCores() {
		return _cores;
	}
	
	public int getThreads() {
		return _threads;
	}
	
	public boolean is64Bit() {
		return _is64Bit;
	}
	
	public boolean isSLI() {
		return _isSLI;
	}
	
	public String getGPU() {
		return _gpu;
	}
	
	public String getGPUDriverVersion() {
		return _driver;
	}
	
	public int getVideoMemorySize() {
		return _vram;
	}
	
	public int getWidth() {
		return _x;
	}
	
	public int getHeight() {
		return _y;
	}
	
	public int getColorDepth() {
		return _bpp;
	}
	
	public int getScreenCount() {
		return _screens;
	}
	
	public void setOSVersion(String v) {
		_os = v;
	}
	
	public void setCLRVersion(String v) {
		_clr = v;
	}
	
	public void setDotNETVersion(String v) {
		_net = v;
	}
	
	public void setLocale(String l) {
		_locale = l;
	}
	
	public void setTimeZone(String tz) {
		_tz = tz;
	}
	
	public void setSimulator(Simulator s) {
		_sim = s;
	}
	
	public void setBridgeInfo(String info) {
		_bridgeInfo = info;
	}
	
	public void setMemorySize(int kb) {
		_ram = Math.max(0, kb);
	}
	
	public void setCPU(String cpu) {
		_cpu = cpu;
	}
	
	public void setCPUSpeed(int mhz) {
		_cpuSpeed = Math.max(0,  mhz);
	}
	
	public void setSockets(int s) {
		_sockets = Math.max(1,  s);
	}
	
	public void setCores(int c) {
		_cores = Math.max(_sockets, c);
	}
	
	public void setThreads(int t) {
		_threads = Math.max(_cores, t);
	}
	
	public void setIs64Bit(boolean is64) {
		_is64Bit = is64;
	}
	
	public void setGPU(String gpu) {
		_gpu = gpu;
	}
	
	public void setIsSLI(boolean sli) {
		_isSLI = sli;
	}
	
	public void setGPUDriverVersion(String v) {
		_driver = v;
	}
	
	public void setVideoMemorySize(int kb) {
		_vram = Math.max(16, kb);
	}
	
	public void setScreenSize(int w, int h) {
		_x = Math.max(0, w);
		_y = Math.max(0, h);
	}
	
	public void setColorDepth(int bpp) {
		_bpp = Math.max(0,  Math.min(64, bpp));
	}
	
	public void setScreenCount(int s) {
		_screens = Math.max(1, s);
	}
}