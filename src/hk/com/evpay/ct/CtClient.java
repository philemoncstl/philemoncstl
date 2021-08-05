package hk.com.evpay.ct;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.websocket.server.ServerContainer;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import com.ckzone.octopus.util.OctCheckerThread;
import com.ckzone.octopus.util.OctDownloadJob;
import com.ckzone.octopus.util.OctUtil;
import com.ckzone.octopus.util.SchedulerUtil;
import com.ckzone.util.StringUtil;

import hk.com.cstl.common.qr.SerialScanner;
import hk.com.cstl.evcs.lms.CpChargingProfile;
import hk.com.cstl.evcs.lms.LMS;
import hk.com.cstl.evcs.lms.LmsCons;
import hk.com.cstl.evcs.lms.LmsServEvent;
import hk.com.cstl.evcs.lms.LmsServEventType;
import hk.com.cstl.evcs.lms.LmsSpecAdapter;
import hk.com.cstl.evcs.model.CpModel;
import hk.com.cstl.evcs.ocpp.CpWebSocket;
import hk.com.cstl.evcs.ocpp.OcppMgr;
import hk.com.cstl.evcs.ocpp.eno.ChargingProfileStatus;
import hk.com.cstl.evcs.ocpp.eno.TriggerMessageStatus;
import hk.com.cstl.evcs.ocpp.msg.cs.SetChargingProfile;
import hk.com.cstl.evcs.ocpp.msg.cs.TriggerMessage;
import hk.com.cstl.evcs.ocpp.msg.cs.TriggerMessageResp;
import hk.com.evpay.ct.tool.ToolUtil;
import hk.com.evpay.ct.tool.TranHistCtrl;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.PrinterCheckerThread;
import hk.com.evpay.ct.util.UiUtil;
import hk.com.evpay.ct.util.iUC285Util;
import hk.com.evpay.ct.ws.CtOcppHandler;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class CtClient extends JFrame{
	private static final Logger logger = Logger.getLogger(CtClient.class);
	
	private CtrlPanel pnlCt;
	
	private OctCheckerThread octCheckerThread;
	private PrinterCheckerThread printerCheckerThread;
	private WsConnCheckerThread wsConnCheckerThread;
	
	private LmsSpecAdapter lmsAdapter = null;
	
	public static final String VER = "v1.1.2 (2020-01-16)";
	
	public static CtClient CUR_INST = null;
	
	public static void main(String[] args) {
		OctUtil.logger.info("Application started, ver:" + VER);
		/*CtModel ct = new CtModel();
		ct.setCtId(1);
		CtUtil.saveCt(ct);*/
		CtClient f = null;
		try {
			f = new CtClient();
			CUR_INST = f;
		} catch (Exception e) {
			logger.error("Failed to init CT Client!", e);
		}
		
		CtConfig cfg = CtUtil.getConfig();
		int width = cfg.getCtWidth();
		int height = cfg.getCtHeight();		
		
		if(cfg.isFullScreen()) {
			f.setUndecorated(true);
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			width = gd.getDisplayMode().getWidth();
			height = gd.getDisplayMode().getHeight();
			UiUtil.setEmptyCursor(f);
		}
		else {
			height += 30;	//top bar
		}
		//f.pack();
		
		logger.info("Screen size:" + width + "x" + height);
		f.setSize(width, height);

		f.startJettyServer();

        f.setVisible(true);       
        f.applicationStarted();
                
        logger.info("Application started");
        
        OcppMgr.setHandlerClass(CtOcppHandler.class);
        
        CtWebSocketClient.startWebSocket();
        logger.info("CT Name:" + CtUtil.getServConfig().getNameChi());
        //UiUtil.printFontList();
        
        // start listen contactless payment callback
		iUC285Util.startListeningCallback(8888, 8889);
		iUC285Util.restartUsbAndEftpayment();
	}
	
	public CtClient() throws IOException {
		super("CT Client");
		
		init();
		
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                int rtn = JOptionPane.showConfirmDialog(CtClient.this, "Are you sure to close CT?",
                		"Close CT Confirmation", JOptionPane.YES_NO_OPTION);
                if(rtn == 0) {
                	closeApplication();
                }
            }
        });
	}
	
	
	
	private void init() {		
		setLayout(new BorderLayout(0, 0));
		
		JPanel pnlCenter = new JPanel();
		add(pnlCenter, BorderLayout.CENTER);
		pnlCenter.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		
		pnlCt = new CtrlPanel();
		pnlCenter.add(pnlCt);
		ToolUtil.initTool(pnlCt);
		OctUtil.addEventListener(pnlCt);
				
		//CK @ 20191112, add qr support
		CtConfig config = CtUtil.getConfig();
		logger.info("qrScannerPort:" + config.getQrScannerPort());
		if(!StringUtil.isEmpty(config.getQrScannerPort())) {
			SerialScanner ss = new SerialScanner(config.getQrScannerPort(), pnlCt);
			try {
				ss.connect();
			} catch (Exception e) {
				logger.error("Failed to conect " + config.getQrScannerPort(), e);
			}
		}
	}
	
	private void closeApplication() {
		try {
			if(octCheckerThread != null) {
				octCheckerThread.setCont(false);
			}
			
			if(printerCheckerThread != null) {
				printerCheckerThread.setCont(false);
			}
			
			if(wsConnCheckerThread != null) {
				wsConnCheckerThread.setCont(false);
			}
			
			logger.info("Application will be exited after 2 sec.");
			//CtUtil.saveCurrentCt();
			OctUtil.reset();
			OctUtil.portClose();			
			Thread.sleep(2000);
			OctUtil.logger.info("Closing application");
		} catch (Throwable e) {
			logger.error(e);
		} finally {
			System.exit(0);
		}
	}
	
	public LmsSpecAdapter getLmsAdapter() {
		return lmsAdapter;
	}

	public void setLmsAdapter(LmsSpecAdapter lmsAdapter) {
		this.lmsAdapter = lmsAdapter;
	}
	
	public LmsServEvent newLmsEvent(LmsServEventType type, String cpNo) {
		LmsServEvent e = new LmsServEvent(type, CtUtil.getConfig().getCtId(), cpNo);
		return e;
	}
	
	public void triggerLmsEvent(LmsServEvent e) {
		if(this.lmsAdapter != null) {
			this.lmsAdapter.triggerEvent(e);
		}
		else {
			logger.info("lms disabled, event not sent:" + e.getType() + ", cp:" + e.getCpNo() + ", parm:" + e.getParmMap());
		}
	}

	private void initLms() {
		//CK @ 20191204 load management support
		lmsAdapter = new LmsSpecAdapter() {
			@Override
			public HashMap<String, String> updateChargingProfilesInOneZone(ArrayList<CpChargingProfile> profileSet) {
				super.updateChargingProfilesInOneZone(profileSet);
				
				final HashMap<String, String> map = new HashMap<String, String>();
				final HashMap<String, SetChargingProfile> profileMap = new HashMap<String, SetChargingProfile>();
				
				//init the request and result map
				for(CpChargingProfile p : profileSet) {					
					map.put(p.getCpNo(), ChargingProfileStatus.NotSent.toString());					
					CpPanel pnl = CtrlPanel.getCpPanelByNo(p.getCpNo());
					if(pnl != null) {
						if(pnl.getCpEp() == null) {
							map.put(p.getCpNo(), ChargingProfileStatus.Disconnected.toString());
						}
						else {
							new Thread() {
								public void run() {
									logger.info("CP:" + p.getCpNo() + ", setChargingProfile:" + p.getProfile());
									SetChargingProfile cpReq = CpWebSocket.setChargingProfile(pnl.getCpEp(), 1, p.getProfile());
									profileMap.put(p.getCpNo(), cpReq);
									map.put(p.getCpNo(), ChargingProfileStatus.WaitingResult.toString());
								};
							}.start();
						}
					}
					else {
						logger.warn("Failed to set charging profile as CP panel is null:" + p.getCpNo());
					}
				}
				
				try {
					Thread.sleep(100);
				} catch(Exception e) {}
				
				logger.info("profileMap.size():" + profileMap.size());
				if(profileMap.size() > 0) {
					long timeout = System.currentTimeMillis() + LmsCons.SET_CHARGING_PROFILE_TIMEOUT_MS;
					while(timeout > System.currentTimeMillis()) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
						
						//check whether all the requests are responsed
						boolean responsed = true;
						for(SetChargingProfile cpReq : profileMap.values()) {
							//logger.info("Checking resp, cp:" + cpReq.getCpNo() + ", resp:" + cpReq.getResponse() + ", respDttm:" + cpReq.getResponseDttm());
							if(cpReq.getResponseDttm() == null) {
								responsed = false;
							}
						}
						
						if(responsed) {
							logger.info("All responsed!");
							break;
						}
					}
					
					//consolidate the response
					for(SetChargingProfile cpReq : profileMap.values()) {
						if(cpReq.getResponse() == null) {
							map.put(cpReq.getCpNo(), ChargingProfileStatus.Timeout.toString());
						}
						else {
							map.put(cpReq.getCpNo(), cpReq.getResponse().getStatus().toString());
						}
					}
				}
				
				logger.info("Resp map:" + map);
				
				return map;
			}
		
			@Override
			public TriggerMessageResp sendTriggerMessage(String zoneNo, String cpNo, TriggerMessage msg) {
				logger.info("sendTriggerMessage:" + zoneNo + ", cp:" + cpNo + ", msg:" + msg.getRequestData());
				TriggerMessageResp resp = null;
				
				TriggerMessageStatus status = null;
				
				CpPanel pnl = CtrlPanel.getCpPanelByNo(cpNo);
				if(pnl != null) {
					if(pnl.getCpEp() == null) {
						status = TriggerMessageStatus.NotConnected;
					}
					else {
						new Thread() {
							public void run() {
								logger.info("CP:" + cpNo + ", triggerMessage:" + msg);
								CpWebSocket.triggerMessage(pnl.getCpEp(), msg);

							};
						}.start();
						

						long timeout = System.currentTimeMillis() + LmsCons.SEND_TRIGGER_MSG_TIMEOUT_MS;
						while(timeout > System.currentTimeMillis()) {
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
							}
							
							//already response
							if(msg.getResponseDttm() != null) {
								try {
									status = TriggerMessageStatus.valueOf(msg.getResponse().getStatus());
								}
								catch(Exception e) {
									status = TriggerMessageStatus.Error;
								}
								break;
							}
						}
						
						if(msg.getResponseDttm() == null) {
							status = TriggerMessageStatus.Timeout;
						}
					}
				}
				else {
					logger.warn("Failed to send trigger message as CP panel is null:" + cpNo);
					status = TriggerMessageStatus.Invalid;
				}
				
				resp = new TriggerMessageResp();
				resp.setStatus(String.valueOf(status));
				
				return resp;
			}
		};
		LMS.setSpec(lmsAdapter);
		
		//send the CPs config for this CT
		LmsServEvent e = newLmsEvent(LmsServEventType.AddZone, null);
		List<String> cpList = new ArrayList<String>();
		for(CpModel m : pnlCt.getCt().getCpList()) {
			cpList.add(m.getCpNo());
		}
		logger.info("Cp list:" + cpList);
		e.addParm(LmsCons.PARM_CP_LIST, cpList);
		lmsAdapter.triggerEvent(e);
	}
	
	public void applicationStarted() {
		CtConfig cfg = CtUtil.getConfig();
		if(cfg.isFullScreen()) {
			this.setAlwaysOnTop(true);
			
			logger.info("Entering full screen mode...");
			try {
				UiUtil.setFullScreenWindow(this, true);
			} catch (Exception e) {
				logger.error("Failed to enter full screen:" + e.getMessage(), e);
			}
		}
		
		logger.info("LMS:" + cfg.isLms());
		if(cfg.isLms()) {
			try {
				initLms();
			} catch(Exception e) {
				logger.error("Failed to init load management", e);
			}
		}
		
		try {
			SchedulerUtil.startScheduler();			
		} catch(Exception e) {
			logger.error("Failed to start scheduler:" + e.getMessage(), e);
		}
		
		if(!cfg.isDisableDeviceCheck()) {
			octCheckerThread = new OctCheckerThread();
			octCheckerThread.start();
	
			printerCheckerThread = new PrinterCheckerThread();
			printerCheckerThread.start();
		}
		
		wsConnCheckerThread = new WsConnCheckerThread(pnlCt);
		wsConnCheckerThread.start();
		
		new Thread() {
			public void run() {
				TranHistCtrl.loadTranHist();
			}
		}.start();
		
		
		//download Octopus file
		new Thread() {
			public void run() {
				try {
					Thread.sleep(10000);
					new OctDownloadJob().execute(null);
				} catch (Exception e) {
					logger.error("Failed to execute Octopus download job.", e);
				}
			}
		}.start();
	}
	
	
	public void startJettyServer() {
		int port = 8080;
		logger.info("Starting server @ port:" + port);
		// The Server
        Server server = new Server();

        // HTTP connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        // Set a handler
        //server.setHandler(new DefaultHandler());
        
        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Start the server
        try {
        	ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(CpWebSocket.class);
            
			server.start();
			//server.join();
		} catch (Exception e) {
			logger.error("Failed to start server.", e);
		}
        logger.info("Server started.");
	}

	public CtrlPanel getPnlCt() {
		return pnlCt;
	}
}
