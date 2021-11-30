package hk.com.evpay.ct;

import java.awt.Insets;

import javax.xml.bind.annotation.XmlRootElement;

import hk.com.cstl.evcs.model.EvCons;

@XmlRootElement
public class CtConfig {
	public static final Insets SCREEN_INSET = new Insets(10, 10, 10, 10);
	private String ctId = "1";
	private String locationReceipt = "";
	
	private boolean disableDeviceCheck = false;
	private boolean debugUi = true;
	private boolean fullScreen = false;	
	private int ctWidth = 1280;
	private int ctHeight = 800;	
	private int northHeight = 230;
	private int southHeight = 120;
	private int westWidth = 130;
	private int eastWidth = 130;
	private int cpHgap = 30;
	private int cpVgap = 30;
	private int cpWidth = 163;
	private int cpHeight = 182;
	
	private long checkOperationResultTimeoutMs = 10000;
	
	private long housekeepingTimeForNonResponseCsRequest = 1200000;
	
	private int postpaidMaxServiceFee = 500;

	private long checkWebSocketIntervalMs = 120000;
	private String webSocketUrl = "ws://localhost:9080/hhl/ctws/";
	
	private long remoteStartStopTimeCheckMs = 10000;
	
	private int goHomeDelaySec = 30;
	
	private int octopusPollTimeoutSec = 28;
	
	private long checkCpHeartbeatResponseTimeoutMs = 2000;
	
	private long takeReceiptDelayMs = 5000;
	
	private long postpaidShowReceiptDelayMs = 10000;
	
	private String contactlessDevice = EvCons.CONTACTLESS_DEVICE_NONE;
	private String contactlessDeviceUrl = "http://192.168.1.12";
	private String contactlessDeviceUsername = "cornerstone";
	private String contactlessDevicePassword = "88888888";
	private long contactlessDeviceCheckMs = 60000;
	
	private String receiptHeading;
	
	private String qrScannerPort;
	private long qrScanTimeoutMs = 600000;
	
	private boolean lms = true;
	
	private String version = "v1.3.0";
	
	public boolean isLms() {
		return lms;
	}

	public void setLms(boolean lms) {
		this.lms = lms;
	}

	public boolean isFullScreen() {
		return fullScreen;
	}

	public void setFullScreen(boolean fullScreen) {
		this.fullScreen = fullScreen;
	}

	public boolean isDebugUi() {
		return debugUi;
	}

	public void setDebugUi(boolean debugUi) {
		this.debugUi = debugUi;
	}

	public int getNorthHeight() {
		return northHeight;
	}

	public void setNorthHeight(int northHeight) {
		this.northHeight = northHeight;
	}

	public int getSouthHeight() {
		return southHeight;
	}

	public void setSouthHeight(int southHeight) {
		this.southHeight = southHeight;
	}

	public int getCpVgap() {
		return cpVgap;
	}

	public void setCpVgap(int cpVgap) {
		this.cpVgap = cpVgap;
	}

	public int getCpHgap() {
		return cpHgap;
	}

	public void setCpHgap(int cpHgap) {
		this.cpHgap = cpHgap;
	}

	public int getCtWidth() {
		return ctWidth;
	}

	public void setCtWidth(int ctWidth) {
		this.ctWidth = ctWidth;
	}

	public int getCtHeight() {
		return ctHeight;
	}

	public void setCtHeight(int ctHeight) {
		this.ctHeight = ctHeight;
	}



	public long getHousekeepingTimeForNonResponseCsRequest() {
		return housekeepingTimeForNonResponseCsRequest;
	}

	public void setHousekeepingTimeForNonResponseCsRequest(long housekeepingTimeForNonResponseCsRequest) {
		this.housekeepingTimeForNonResponseCsRequest = housekeepingTimeForNonResponseCsRequest;
	}

	public long getCheckOperationResultTimeoutMs() {
		return checkOperationResultTimeoutMs;
	}

	public void setCheckOperationResultTimeoutMs(long checkOperationResultTimeoutMs) {
		this.checkOperationResultTimeoutMs = checkOperationResultTimeoutMs;
	}

	public int getWestWidth() {
		return westWidth;
	}

	public void setWestWidth(int westWidth) {
		this.westWidth = westWidth;
	}

	public int getCpWidth() {
		return cpWidth;
	}

	public void setCpWidth(int cpWidth) {
		this.cpWidth = cpWidth;
	}

	public int getCpHeight() {
		return cpHeight;
	}

	public void setCpHeight(int cpHeight) {
		this.cpHeight = cpHeight;
	}

	public long getCheckWebSocketIntervalMs() {
		return checkWebSocketIntervalMs;
	}

	public void setCheckWebSocketIntervalMs(long checkWebSocketIntervalMs) {
		this.checkWebSocketIntervalMs = checkWebSocketIntervalMs;
	}

	public String getCtId() {
		return ctId;
	}

	public void setCtId(String ctId) {
		this.ctId = ctId;
	}

	public String getWebSocketUrl() {
		return webSocketUrl;
	}

	public void setWebSocketUrl(String webSocketUrl) {
		this.webSocketUrl = webSocketUrl;
	}

	public int getPostpaidMaxServiceFee() {
		return postpaidMaxServiceFee;
	}

	public void setPostpaidMaxServiceFee(int postpaidMaxServiceFee) {
		this.postpaidMaxServiceFee = postpaidMaxServiceFee;
	}

	public long getRemoteStartStopTimeCheckMs() {
		return remoteStartStopTimeCheckMs;
	}

	public void setRemoteStartStopTimeCheckMs(long remoteStartStopTimeCheckMs) {
		this.remoteStartStopTimeCheckMs = remoteStartStopTimeCheckMs;
	}

	public String getLocationReceipt() {
		return locationReceipt;
	}

	public void setLocationReceipt(String locationReceipt) {
		this.locationReceipt = locationReceipt;
	}

	public int getGoHomeDelaySec() {
		return goHomeDelaySec;
	}

	public void setGoHomeDelaySec(int goHomeDelaySec) {
		this.goHomeDelaySec = goHomeDelaySec;
	}

	public int getOctopusPollTimeoutSec() {
		return octopusPollTimeoutSec;
	}

	public void setOctopusPollTimeoutSec(int octopusPollTimeoutSec) {
		this.octopusPollTimeoutSec = octopusPollTimeoutSec;
	}

	public long getCheckCpHeartbeatResponseTimeoutMs() {
		return checkCpHeartbeatResponseTimeoutMs;
	}

	public void setCheckCpHeartbeatResponseTimeoutMs(long checkCpHeartbeatResponseTimeoutMs) {
		this.checkCpHeartbeatResponseTimeoutMs = checkCpHeartbeatResponseTimeoutMs;
	}

	public boolean isDisableDeviceCheck() {
		return disableDeviceCheck;
	}

	public void setDisableDeviceCheck(boolean disableDeviceCheck) {
		this.disableDeviceCheck = disableDeviceCheck;
	}

	public long getTakeReceiptDelayMs() {
		return takeReceiptDelayMs;
	}

	public void setTakeReceiptDelayMs(long takeReceiptDelayMs) {
		this.takeReceiptDelayMs = takeReceiptDelayMs;
	}

	public long getPostpaidShowReceiptDelayMs() {
		return postpaidShowReceiptDelayMs;
	}

	public void setPostpaidShowReceiptDelayMs(long postpaidShowReceiptDelayMs) {
		this.postpaidShowReceiptDelayMs = postpaidShowReceiptDelayMs;
	}

	public String getContactlessDevice() {
		return contactlessDevice;
	}

	public void setContactlessDevice(String contactlessDevice) {
		this.contactlessDevice = contactlessDevice;
	}

	public String getContactlessDeviceUrl() {
		return contactlessDeviceUrl;
	}

	public void setContactlessDeviceUrl(String contactlessDeviceUrl) {
		this.contactlessDeviceUrl = contactlessDeviceUrl;
	}

	public String getContactlessDeviceUsername() {
		return contactlessDeviceUsername;
	}

	public void setContactlessDeviceUsername(String contactlessDeviceUsername) {
		this.contactlessDeviceUsername = contactlessDeviceUsername;
	}

	public String getContactlessDevicePassword() {
		return contactlessDevicePassword;
	}

	public void setContactlessDevicePassword(String contactlessDevicePassword) {
		this.contactlessDevicePassword = contactlessDevicePassword;
	}

	public long getContactlessDeviceCheckMs() {
		return contactlessDeviceCheckMs;
	}

	public void setContactlessDeviceCheckMs(long contactlessDeviceCheckMs) {
		this.contactlessDeviceCheckMs = contactlessDeviceCheckMs;
	}

	public String getReceiptHeading() {
		return receiptHeading;
	}

	public void setReceiptHeading(String receiptHeading) {
		this.receiptHeading = receiptHeading;
	}

	public String getQrScannerPort() {
		return qrScannerPort;
	}

	public void setQrScannerPort(String qrScannerPort) {
		this.qrScannerPort = qrScannerPort;
	}

	public long getQrScanTimeoutMs() {
		return qrScanTimeoutMs;
	}

	public void setQrScanTimeoutMs(long qrScanTimeoutMs) {
		this.qrScanTimeoutMs = qrScanTimeoutMs;
	}

	public int getEastWidth() {
		return eastWidth;
	}

	public void setEastWidth(int eastWidth) {
		this.eastWidth = eastWidth;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}	
	
	
}
