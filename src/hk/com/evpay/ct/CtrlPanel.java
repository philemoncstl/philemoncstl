package hk.com.evpay.ct;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.Logger;

import com.ckzone.octopus.OctEventData;
import com.ckzone.octopus.OctEventListener;
import com.ckzone.octopus.OctEventType;
import com.ckzone.octopus.OctStatus;
import com.ckzone.util.StringUtil;

import hk.com.cstl.common.qr.ScanEventListener;
import hk.com.cstl.evcs.lms.LmsCons;
import hk.com.cstl.evcs.lms.LmsServEvent;
import hk.com.cstl.evcs.lms.LmsServEventType;
import hk.com.cstl.evcs.model.CpModel;
import hk.com.cstl.evcs.model.CtModel;
import hk.com.cstl.evcs.model.TranModel;
import hk.com.cstl.evcs.ocpp.CpWebSocket;
import hk.com.cstl.evcs.ocpp.CpWebSocketEventListener;
import hk.com.cstl.evcs.ocpp.CpWebSocketServEndpoint;
import hk.com.cstl.evcs.ocpp.eno.ChargePointStatus;
import hk.com.cstl.evcs.ocpp.eno.PayMethod;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.i18n.I18nSupport;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.wp.WpCheckerThread;
import hk.com.evpay.ct.wp.WpCons;
import hk.com.evpay.ct.wp.WpStatusChangeListener;
import hk.com.evpay.ct.ws.CtWebSocketClient;

public class CtrlPanel extends CommonPanel implements CpWebSocketEventListener, MouseListener, OctEventListener, 
	WpStatusChangeListener, ScanEventListener{
	
	private static final Logger logger = Logger.getLogger(CtrlPanel.class);
	
	public static final int BG_HOME = 0;
	public static final int BG_WITH_TITLE = 1;
	public static final int BG_WITHOUT_TITLE = 2;
	public static final int BG_RECEIPT = 3;
	public static final int BG_HELP = 4;
	public static final int BG_DEV = 5;
	
	public static final DateFormat DF = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat DF_REF_NO = new SimpleDateFormat("MMddHHmm");
		
	private static BufferedImage[] BG = null;
	
	private static CtrlPanel  CURRENCT_INSTANCE = null;
	
	//north
	private JLabel lblTime;
	private I18nLabel lblTitle;
	
	//center
	private JPanel pnlCard;
	private CardLayout cardLayout;
	private CommonPanel pnlCurrent;
	private CommonPanel pnlLastOctInitCommError;
	
	private CpPanel pnlSelectedCp;
	private CpSelectionPanel pnlCpList;
	private HelpPanel pnlHelp;
	private ErrorPanel pnlError;
	
	//Prepaid
	private Step1SelectTime step1SelectTime;
	private Step2aReceiptNotAvailable step2aReceiptNotAvailable;
	private Step2ProcessPayment step2ProcPayment;
	private Step3PrintReceipt step3PrintReceipt;
	private Step4TakeReceipt step4GetReceipt;
	private Step5ChargingRecord step5ChargingRecord;
	private Step6StopCharging step6StopCharging;
	private Step7StopChargingTapCard step7StopChargingTapCard;
	private Step8UnplugCable step8UnplugCable;
	
	
	//Postpaid
	private PostStep1SelectPayment postStep1SelectPayment;
	private PostStep2ProcessPayment postStep2ProcPayment;
	private PostStep3ChargingRecord postStep3ChargingRecord;
	private PostStep4StopCharging postStep4StopCharging;
	private PostStep5StopChargingTapCard postStep5StopChargingTapCard;
	private PostStep6ShowReceipt postStep6PrintnReceipt;
	
	
	//west
	private I18nButtonLabel lblHome;
	private I18nButtonLabel lblLang;
	private I18nButtonLabel lblHelp;
	
	private GoHomeCountDownThread countDownThead;
	private GoHomeUnlockThread unlockThread;
	
	private PayMethod payMethod;
	
	private String qr;
	private long qrDttm;
	
	public static long QR_THREAD_MS = -1;
	
	public CtrlPanel() {
		super(null);
		initUI();
		
		CURRENCT_INSTANCE = this;
		
		CpWebSocket.addListener(this);
		WpCheckerThread.addListener(this);
	}
	
	public static void updateCtUi() {
		if(CURRENCT_INSTANCE != null) {
			logger.info("CT updated");
			updateUi(CURRENCT_INSTANCE);
		}
	}
	
	public static void updateCpUi(String cpNo) {
		if(CURRENCT_INSTANCE != null) {
			CpPanel pnlCp = CURRENCT_INSTANCE.getCpPanel(cpNo);
			if(pnlCp == null) {
				logger.info("Failed to update CP UI, CP not found:" + cpNo);
			}
			else {
				logger.info("Updating CP UI:" + cpNo);
				updateUi(pnlCp);				
			}
		}
	}
	
	public static CpWebSocketServEndpoint getCpWebSocket(String cpNo) {
		CpPanel pnl = getCpPanelByNo(cpNo);
		return pnl == null ? null : pnl.getCpEp();
	}
	
	public static CpPanel getCpPanelByNo(String cpNo) {
		if(CURRENCT_INSTANCE != null) {
			return CURRENCT_INSTANCE.getCpPanel(cpNo);
		}
		
		return null;
	}
	
	public static void rebootCt() {
		logger.info("Reboot CT received");
		new Thread() {
			public void run() {
				logger.info("Reboot CT in 5 seconds");
				try {
					Thread.sleep(5000);					
				} catch (InterruptedException e) {
				}
				
				try {
					logger.info("Reboot CT now");
					Runtime.getRuntime().exec("sudo reboot");
				} catch (IOException e) {
					logger.error("Failed to reboot CT", e);
				}
			};
		}.start();
	}
	
	public static BufferedImage[] getBg() {
		if(BG == null) {
			logger.info("Loading background image ...");
			BG = new BufferedImage[BG_DEV + 1];
			
			for(int i = 0; i < BG.length; i ++) {
				File f = new File("img/bg" + i + ".png");
				try {
					BG[i] = ImageIO.read(f);
				} catch (IOException e) {
					logger.error("Failed to load bg:" + f.getPath(), e);
				}
			}
			logger.info("Load background image completed.");
		}
		
		return BG;
	}
	
	public CommonPanel getCurrentPanel() {
		return pnlCurrent;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		int idx = getCurrentPanel() == null ?  BG_HOME : getCurrentPanel().getBackgroundIdx();
		g.drawImage(getBg()[idx], 0, 0, null);		
	}

	@Override
	public void sessionOpened(final CpWebSocketServEndpoint cpEp, boolean firstTime) {
		logger.info("Session opened:" + cpEp.getCpNo() + ", firstTime:" + firstTime);
		
		CpModel cp = CtUtil.getCp(cpEp.getCpNo());
		if(cp == null) {
			cp = new CpModel(cpEp.getCpNo());
			CtUtil.getCt().addCp(cp);
			
			//CK @ 20191204, notify LMS
			LmsServEvent e = CtClient.CUR_INST.newLmsEvent(LmsServEventType.AddCpToZone, cp.getCpNo());
			CtClient.CUR_INST.triggerLmsEvent(e);
		}
		cp.setConnected(true);
		cp.setConnectedDttm(cpEp.getConnectedDttm());
		cp.setDisconnectedDttm(null);
		cpEp.setCp(cp);
		
		//CK @ 20191204, notify LMS
		LmsServEvent e = CtClient.CUR_INST.newLmsEvent(LmsServEventType.CpConnected, cp.getCpNo());
		e.addParm(LmsCons.PARM_FIRST_TIME, firstTime);
		CtClient.CUR_INST.triggerLmsEvent(e);
		
		
		CtUtil.saveCurrentCt();
		
		List<CpPanel> cpList = pnlCpList.getCpList();
		//rearrange CP order based on CP No
		int c = Math.min(ct.getCpList().size(), 10);
		for(int i = 0; i < c; i ++) {
			cpList.get(i).setCp(ct.getCpList().get(i));
		}
		
		for(int i = c; i < 10; i ++) {
			cpList.get(i).setCp(null);
		}
		
		CpPanel pnlCp = getCpPanel(cpEp);
		if(pnlCp != null) {
			pnlCp.setCpEp(cpEp);
			
			//handle CP with status charging
			TranModel tm = pnlCp.getCp().getTran();
			if(tm != null && CtUtil.isTranStatusCharging(tm)) {
				new StopChargingThread(pnlCp).start();
			}
		}
		
		updateUi(this);
	}

	@Override
	public void sessionClosed(CpWebSocketServEndpoint cpEp) {
		logger.info("Session closed:" + cpEp.getCpNo());
		CpModel cp = CtUtil.getCp(cpEp.getCpNo());
		if(cp != null) {
			cp.setConnected(false);
			cp.setDisconnectedDttm(new Date());
		}
		
		CpPanel pnlCp = getCpPanel(cpEp);
		if(pnlCp != null) {
			logger.info("Set ws to null");
			pnlCp.setCpEp(null);
		}
		
		updateUi(pnlCp);
		pnlCp.setCpStatus(ChargePointStatus.Unavailable);
		CtUtil.saveCurrentCt();
		
		//CK @ 20191204, notify LMS
		LmsServEvent e = CtClient.CUR_INST.newLmsEvent(LmsServEventType.CpDisconnected, cp.getCpNo());
		CtClient.CUR_INST.triggerLmsEvent(e);
	}

	@Override
	public void messageReceived(CpWebSocketServEndpoint cpEp, String msg) {
		logger.info("Received CP:" + cpEp.getCpNo() + ", msg:" + msg);
		
		updateUi(getCpPanel(cpEp));
	}
	
	public CpPanel getCpPanel(CpWebSocketServEndpoint cpEp) {
		if(cpEp.getCp() == null) {
			return getCpPanel(cpEp.getCpNo());
		}
		else {
			return getCpPanel(cpEp.getCp());
		}
	}
	
	public CpPanel getCpPanel(String cpNo) {
		for(CpPanel p : pnlCpList.getCpList()) {
			if(p.getCp() != null && p.getCp().getCpNo().equals(cpNo)) {
				return p;
			}
		}
		
		return null;
	}
	
	public CpPanel getCpPanel(CpModel cp) {
		for(CpPanel p : pnlCpList.getCpList()) {
			if(p.getCp() == cp) {
				return p;
			}
		}
		
		return null;
	}
	
	public static void updateUi(final JComponent comp) {
		if(comp == null) {
			return;
		}
		
		if (!SwingUtilities.isEventDispatchThread()) {
			logger.info("Update out of UI thread: " + comp.getClass());
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					comp.repaint();
				}
			});
			return;
		} else {
			comp.repaint();
		}
	}
	
	
	private void initUI() {
		setLayout(new BorderLayout(0, 0));
		Insets i = config.SCREEN_INSET;
		setBorder(BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right));
		
		JPanel pnlNorth = new CommonPanel(this);
		add(pnlNorth, BorderLayout.NORTH);
		pnlNorth.setPreferredSize(new Dimension(config.getCtWidth(), config.getNorthHeight()));

		JPanel tmp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		tmp.setPreferredSize(new Dimension(config.getCtWidth(), 50));
		tmp.setOpaque(false);
		//UiUtil.debugUi(tmp);
		lblTime = new JLabel();
		lblTime.setPreferredSize(new Dimension(165, 50));
		lblTime.setBorder(BorderFactory.createEmptyBorder(12, 1, 1, 1));
		lblTime.setFont(new Font(LangUtil.FONT_EN, Font.PLAIN, 32));
		tmp.add(lblTime);
		pnlNorth.add(tmp);
		
		lblTitle = new I18nLabel();
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		LangUtil.setFont(lblTitle, Font.BOLD, 64);
		lblTitle.setPreferredSize(new Dimension(config.getCtWidth() - 100, 80));
		//lblTitle.setForeground(new Color(239, 89, 161)); //HHL
		lblTitle.setForeground(new Color(24, 97, 160));		//CST
		pnlNorth.add(lblTitle);
		
		/*JPanel pnlSouth = new CommonPanel();
		add(pnlSouth, BorderLayout.SOUTH);
		pnlSouth.setPreferredSize(new Dimension(config.getCtWidth(), config.getSouthHeight()));*/
		
		JPanel pnlWest = new CommonPanel(this);
		add(pnlWest, BorderLayout.WEST);
		pnlWest.setPreferredSize(new Dimension(config.getWestWidth(), config.getCtHeight()));
		pnlWest.setLayout(null);
		
		//Home button
		lblHome = new I18nButtonLabel("", 0, "img/home.png");
		Rectangle r = new Rectangle(36, 188, 64, 64);
		lblHome.setBounds(r);
		pnlWest.add(lblHome);
		lblHome.addMouseListener(this);
		
		//Lang button
		lblLang = new I18nButtonLabel("lang", 0, "img/lang.png");
		LangUtil.setFont(lblLang, Font.PLAIN, 20);
		r.y += 80;
		lblLang.setBounds(r);
		pnlWest.add(lblLang);
		lblLang.addMouseListener(this);
		
		//Help button
		lblHelp = new I18nButtonLabel("", 0, "img/help.png");
		r.y += 90;
		lblHelp.setBounds(r);
		pnlWest.add(lblHelp);
		lblHelp.addMouseListener(this);
		
		Dimension dimMainArea = new Dimension(config.getCtWidth() - config.getWestWidth(), config.getCtHeight() - config.getNorthHeight());
		pnlCard = new JPanel();
		add(pnlCard, BorderLayout.CENTER);
		pnlCard.setPreferredSize(dimMainArea);
		pnlCard.setOpaque(false);
		cardLayout = new CardLayout();
		pnlCard.setLayout(cardLayout);
		//CP List
		pnlCpList = new CpSelectionPanel(this);
		addCard(pnlCpList, dimMainArea);
		
		//Help 
		pnlHelp = new HelpPanel(this);
		addCard(pnlHelp, dimMainArea);
		
		//Error 
		pnlError = new ErrorPanel(this);
		addCard(pnlError, dimMainArea);
		
		//1 - Select Time
		step1SelectTime = new Step1SelectTime(this);
		addCard(step1SelectTime, dimMainArea);
		
		//2a - Receipt not available
		step2aReceiptNotAvailable = new Step2aReceiptNotAvailable(this);
		addCard(step2aReceiptNotAvailable, dimMainArea);
		
		//2 - Process Payment
		step2ProcPayment = new Step2ProcessPayment(this);
		addCard(step2ProcPayment, dimMainArea);		
		
		//3 - Print Receipt
		step3PrintReceipt = new Step3PrintReceipt(this);
		addCard(step3PrintReceipt, dimMainArea);
		
		//4 - Get Receipt
		step4GetReceipt = new Step4TakeReceipt(this);
		addCard(step4GetReceipt, dimMainArea);
		
		//5 - Charging Receipt
		step5ChargingRecord = new Step5ChargingRecord(this);
		addCard(step5ChargingRecord, dimMainArea);
		
		//6 - Stop Charging
		step6StopCharging = new Step6StopCharging(this);
		addCard(step6StopCharging, dimMainArea);
		
		//7 - Tap Card to Stop Charging
		step7StopChargingTapCard = new Step7StopChargingTapCard(this);
		addCard(step7StopChargingTapCard, dimMainArea);
		
		//8 - Unplug Charging Cable
		step8UnplugCable = new Step8UnplugCable(this);
		addCard(step8UnplugCable, dimMainArea);
		
		
		//Postpaid
		//1 - Select payment
		postStep1SelectPayment = new PostStep1SelectPayment(this);
		addCard(postStep1SelectPayment, dimMainArea);
		
		//2 - Proc payment
		postStep2ProcPayment = new PostStep2ProcessPayment(this);
		addCard(postStep2ProcPayment, dimMainArea);
		
		//3 - Charging record
		postStep3ChargingRecord = new PostStep3ChargingRecord(this);
		addCard(postStep3ChargingRecord, dimMainArea);
		
		//4 - Stop charging
		postStep4StopCharging = new PostStep4StopCharging(this);
		addCard(postStep4StopCharging, dimMainArea);		
		
		//5 - Stop charging Tap Card
		postStep5StopChargingTapCard = new PostStep5StopChargingTapCard(this);
		addCard(postStep5StopChargingTapCard, dimMainArea);
		
		//5 - Print receipt
		postStep6PrintnReceipt = new PostStep6ShowReceipt(this);
		addCard(postStep6PrintnReceipt, dimMainArea);
		
		
		showCard(pnlCpList);
		
		//start timer
		Timer timer = new Timer(1000, new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				lblTime.setText(DF.format(new Date()));
			}
		});
		timer.start();
	}
	
	private void addCard(CommonPanel pnl, Dimension dim) {
		pnl.setPreferredSize(dim);
		pnlCard.add(pnl, pnl.getCardName());
	}
	
	public void goToHome() {
		if(this.unlockThread == null) {
			logger.info("Go to home called");
			showCard(pnlCpList);
		}
		else {
			logger.info("Go to home ignored, locking now");
		}
	}
	
	public void goHomeCountDown(JComponent requestComponent) {
		countDownThead = new GoHomeCountDownThread(this, requestComponent);
		countDownThead.start();
	}
	
	public void goHomeCountDownExtend(long tm) {
		if(countDownThead != null) {
			countDownThead.checkAndExtendTimeout(tm);
		}
		else {
			logger.info("Count down thread not started, no need to extend!");
		}
	}
	
	public void goHomeLock(long delayMs) {
		unlockThread = new GoHomeUnlockThread(this, delayMs);
		unlockThread.start();
		
		//also extends go home count down for delayMs + 2000
		goHomeCountDownExtend(delayMs + 2000);
	}
	
	public void goHomeUnlock() {
		logger.info("goHomeUnlock()");
		this.unlockThread = null;
	}
	
	private void displayPanel(CommonPanel pnl) {
		try {
			logger.debug("displayPanel:" + pnl);
			pnl.onDisplay(getPnlSelectedCp());
			showCard(pnl);
		} catch (Exception e) {
			logger.error("Failed to display panel:" + pnl, e);
		}
	}
	
	public void goToPostStep1SelectPayment(CpPanel pnl) {
		pnlSelectedCp = pnl;
		logger.debug("step1 " + pnl.getCp().getCpNo() + " select payment");
		displayPanel(postStep1SelectPayment);
	}
	
	public void goToPostStep2ProcessPayment() {
		logger.debug("step2 " + pnlSelectedCp.getCp().getCpNo() + " process payment");	
		displayPanel(postStep2ProcPayment);
	}
	
	
	public void goToPostStep3ChargingRecord() {
		logger.debug("step3 " + pnlSelectedCp.getCp().getCpNo() + " charging record");
		displayPanel(postStep3ChargingRecord);
	}
	
	public void goToPostStep4StopCharging(CpPanel pnlCp) {
		pnlSelectedCp = pnlCp;
		logger.debug("step4 " + pnlSelectedCp.getCp().getCpNo() + " stop charging");
		displayPanel(postStep4StopCharging);
	}
	
	public void goToPostStep5StopChargingTapCard() {
		logger.debug("step5 " + pnlSelectedCp.getCp().getCpNo() + " stop charging tap card");
		displayPanel(postStep5StopChargingTapCard);
	}
	
	
	public void goToPostStep6ShowReceipt() {
		logger.debug("step6 " + pnlSelectedCp.getCp().getCpNo() + " print receipt");
		displayPanel(postStep6PrintnReceipt);
	}
	
	
	public void goToStep1SelectTime(CpPanel pnl) {
		pnlSelectedCp = pnl;
		logger.debug("step1 " + pnl.getCp().getCpNo() + " select time");
		displayPanel(step1SelectTime);
	}
	
	public void goToStep2aReceiptNotAvailable() {
		logger.debug("step2a receipt not available");	
		displayPanel(step2aReceiptNotAvailable);
	}
	
	public void goToStep2ProcessPayment() {
		logger.debug("step2 " + pnlSelectedCp.getCp().getCpNo() + " process payment");
		displayPanel(step2ProcPayment);
	}
	
	public void goToStep3PrintReceipt() {
		logger.debug("step3 " + pnlSelectedCp.getCp().getCpNo() + " print receipt");
		displayPanel(step3PrintReceipt);
	}
	
	public void goToStep4GetReceipt() {
		logger.debug("step4 " + pnlSelectedCp.getCp().getCpNo() + " get receipt");	
		displayPanel(step4GetReceipt);
	}
	
	public void goToStep5ChargingRecord() {
		logger.debug("step5 " + pnlSelectedCp.getCp().getCpNo() + " charging record");
		displayPanel(step5ChargingRecord);
	}
	
	public void goToStep6StopCharging(CpPanel pnl) {
		pnlSelectedCp = pnl;
		logger.debug("step6 " + pnl.getCp().getCpNo() + " stop charging");
		displayPanel(step6StopCharging);
	}
	
	public void goToStep7StopChargingTapCard() {
		logger.debug("step7 " + getPnlSelectedCp().getCp().getCpNo() + " stop charging (tap card)");
		displayPanel(step7StopChargingTapCard);
	}
	
	public void goToStep8UnplugCable() {
		logger.debug("step8 " + getPnlSelectedCp().getCp().getCpNo() + " unplug cable");
		displayPanel(step8UnplugCable);
	}
	
	public void showErrorMessageGeneral(String errorCode, Throwable e) {
		String cpNo = getPnlSelectedCp() == null ? null : getPnlSelectedCp().getCp().getCpNo();
		showErrorMessageGeneral(errorCode,  cpNo, e);
	}
	
	public void showErrorMessageGeneral(String errorCode, String cpNo, Throwable e) {
		String uuid = UUID.randomUUID().toString();
		String code = StringUtil.isEmpty(errorCode) ? "9000" : errorCode;
		String refNo = CtUtil.getCt().getCtId() + "-" + code + 
				"-" + DF_REF_NO.format(new Date()) + "-" + uuid.substring(0, 8);
		showErrorMessage("ERR" + code, refNo);
		goHomeCountDown(pnlError);
		CtWebSocketClient.uploadAlert(uuid, refNo, cpNo, e);
	}
	
	public void showErrorMessage(String msgCode) {
		showErrorMessage(msgCode, null);
	}
	
	public void showErrorMessage(String msgCode, String parm) {		
		logger.info("Error:" + msgCode + ", parm:" + parm);
		
		if(msgCode.startsWith("ERR100001") || msgCode.startsWith("ERR100005") || msgCode.equals("ERROctopusOthers")){
			if(pnlCurrent != pnlError) {
				pnlLastOctInitCommError = pnlCurrent;
				logger.info("pnlLastOctInitCommError set to:" + pnlCurrent.getClass());
			}
		}
		/*else {
			logger.info("pnlLastOctInitCommError set to null");
			pnlLastOctInitCommError = null;
		}*/
		
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				pnlError.getLblMsg().setMsgCode(msgCode);
				if(!StringUtil.isEmpty(parm)) {
					pnlError.getLblMsg().setParms(parm);
				}
				pnlError.onDisplay(null);	//CK @ 20180311, go back to home
				showCard(pnlError);
			}
		});		
	}
	
	public boolean isShowingErrorMessage() {
		return pnlError.isShowing();
	}
	
	private void changeLang() {
		logger.debug("Change lang");
		if(LangUtil.isEnglish()) {
			LangUtil.setChinses();
		}
		else {
			LangUtil.setEnglish();
		}
		
		langChanged(this);	
		logger.debug("Lang changed to:" + (LangUtil.isEnglish() ? "EN" : "ZH"));
	}
	
	private void langChanged(Container c) {
		for(Component comp : c.getComponents()) {
			if(comp instanceof Container) {
				langChanged((Container)comp);
			}
			
			if(comp instanceof I18nSupport) {
				((I18nSupport)comp).langChanged();
			}
		}		
	}
	
	public void goToHelp() {
		if(this.unlockThread == null) {
			logger.info("Go to help called");
			pnlHelp.onDisplay(null);
			showCard(pnlHelp);
		}
		else {
			logger.info("Go to help ignored, locking now");
		}
	}
	
	public void showCard(CommonPanel pnl) {
		logger.info("Show card:" + pnl.getCardName());
		pnlCurrent = pnl;
		cardLayout.show(pnlCard, pnl.getCardName());
		
		lblTitle.setMsgCode(pnl.getTitleMsgKey());

		repaint();
	}
	
	public boolean isCurrentDisplayingPanel(CommonPanel pnl) {
		return pnlCurrent == pnl;
	}
	
	public boolean requestCpConnectionCheck() {
		boolean replyOnTime = false;
		CpPanel pnl = pnlSelectedCp;
		if(pnlSelectedCp == null) {
			return false;
		}
		
		CpModel cp = pnl.getCp();
		logger.info("requestCpConnectionCheck:" + cp.getCpNo() + " started");
		long checkDelay = CtUtil.getConfig().getCheckCpHeartbeatResponseTimeoutMs();
		if(pnl != null && pnl.getCpEp() != null) {
			try {
				CpWebSocket.requestHeartbeat(pnl.getCpEp());
				long timeout = System.currentTimeMillis() + checkDelay;
				while(timeout >= System.currentTimeMillis()) {
					if(pnl != pnlSelectedCp) {
						replyOnTime = false;
						break;
					}
					
					if(cp.getLastReceivedDttm() != null && (System.currentTimeMillis() - cp.getLastReceivedDttm().getTime() <= checkDelay)) {
						replyOnTime = true;
						break;
					}
					Thread.sleep(20);
				}
			} catch(Exception e) {
				logger.error("Failed to check CP connection", e);
				replyOnTime = false;
			}
			
		}
		
		logger.info("requestCpConnectionCheck:" + cp.getCpNo() + " completed, res:" + replyOnTime);
		
		return replyOnTime;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(config.getCtWidth(), config.getCtHeight());
	}


	@Override
	public void mouseClicked(MouseEvent e) {
	}


	@Override
	public void mousePressed(MouseEvent e) {
		logger.info("mousePressed:" + e);
		Object src = e.getSource();
		
		if(src instanceof CpPanel) {
			CpPanel pnlCp = (CpPanel) e.getSource();
			CpModel cp = pnlCp.getCp();
			
			if(cp == null) {
				logger.debug("CP is null");
				return;
			}
			
			logger.info("Pnl enable:" + pnlCp.isEnabled() + ", CP:" + cp.getCpNo() + ", enabled:" + cp.isEnabled() + ", connected:" + cp.isConnected() + 
					", status:" + cp.getStatus() + ", cpEp is null:" + (pnlCp.getCpEp() == null) + ", tran:" + cp.getTran());
			if(pnlCp.isCpEnabled() && pnlCp.isCpConnected()) {
				if(cp.getStatus() == ChargePointStatus.Preparing) {
					//prepaid
					if(CtUtil.isModePrepaid()) {
						goToStep1SelectTime(pnlCp);
					}
					//postpaid
					else {
						goToPostStep1SelectPayment(pnlCp);
					}
				}
				else if(CtUtil.isCpStatusCharging(cp.getStatus())) {
					//prepaid
					if(CtUtil.isModePrepaid(pnlCp.getCp().getTran())) {
						goToStep6StopCharging(pnlCp);
					}
					//postpaid
					else {
						goToPostStep4StopCharging(pnlCp);
					}
				}
				else {
					logger.warn("Unexcepted status:" + cp.getStatus() + ", CP:" + cp.getCpNo());
				}
			}
			else {
				logger.warn("CP is disabled or disconnected:" + cp.getCpNo());
			}
		}
		else if(src == lblHome){
			goToHome();
		}
		else if(src == lblLang) {
			changeLang();
		}
		else if(src == lblHelp) {
			goToHelp();
		}
	}


	@Override
	public void mouseReleased(MouseEvent e) {
	}


	@Override
	public void mouseEntered(MouseEvent e) {
		
	}


	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	public CpPanel getPnlSelectedCp() {
		return pnlSelectedCp;
	}

	public void setPnlSelectedCp(CpPanel pnlSelectedCp) {
		this.pnlSelectedCp = pnlSelectedCp;
	}

	public GoHomeCountDownThread getCountDownThead() {
		return countDownThead;
	}

	public GoHomeUnlockThread getUnlockThread() {
		return unlockThread;
	}

	public PayMethod getPayMethod() {
		return payMethod;
	}

	public void setPayMethod(PayMethod payMethod) {
		this.payMethod = payMethod;
	}

	public CpSelectionPanel getPnlCpList() {
		return pnlCpList;
	}

	public void setPnlCpList(CpSelectionPanel pnlCpList) {
		this.pnlCpList = pnlCpList;
	}

	@Override
	public void setReaderId(String id) {
		logger.info("Octopus reader ID:" + id);
	}
	
	@Override
	public void eventReceived(OctEventData data) {
		ct.setOctopusStatusDttm(data.getEventDttm());
		
		if(data.getEventType() == OctEventType.StatusChange) {
			boolean available = String.valueOf(OctStatus.Available).equals(data.getRemark());
			logger.info("Octopus status changed:" + data.getRemark());
			logger.info("pnlLastOctInitCommError != null:" + (pnlLastOctInitCommError != null ) + 
					(pnlLastOctInitCommError == null ? "" : ",panel:" + pnlLastOctInitCommError.getClass()) +
					", available:" + available + 
					", pnlError.isShowing():" + pnlError.isShowing());
			//2018-05-04, hide Octopus error after resume normal
			try {
				if(pnlLastOctInitCommError != null && available && pnlError.isShowing()) {
					logger.info("Octopus resume normal, hide the error panel now!");
					SwingUtilities.invokeLater(new Runnable() {			
						@Override
						public void run() {
							showCard(pnlLastOctInitCommError);
							pnlLastOctInitCommError = null;
						}
					});	
				}
				else {
					if(available) {
						pnlLastOctInitCommError = null;
					}
				}
			} catch (Exception e) {
				logger.error("Failed to procee error panel!", e);
			}
			
			
			ct.setOctopusStatus(data.getRemark());			
			CtWebSocketClient.updateCt();
			CtUtil.saveCurrentCt();			
		}
		
		CtWebSocketClient.uploadOctEvent(data);
	}

	@Override
	public void statusChanged(String status, String state) {
		String s = CtModel.DEVICE_STATUS_UNKNOWN;
		if(WpCons.STATUS_NOT_YET_LOGGED_IN.equals(status)) {
			s = CtModel.DEVICE_STATUS_NOT_AVAILABLE;
		}
		else if(WpCons.STATUS_LOGGED_IN.equals(status)) {
			s = CtModel.DEVICE_STATUS_AVAILABLE;
		}
		
		ct.setContactlessDeviceStatus(s);
		ct.setContactlessDeviceStatusDttm(new Date());
		CtWebSocketClient.updateCt();
		CtUtil.saveCurrentCt();
	}

	@Override
	public void labelDetected(String portName, String text) {
		logger.info("Recevied QR:" + text + ", port:" + portName);
		if(StringUtil.isEmpty(text)) {
			logger.info("Ignored empty text");
			return;
		}
		this.qr = text.trim();
		this.qrDttm = System.currentTimeMillis();		
	}

	@Override
	public void exceptionOccured(String portName, Exception e) {
		logger.error("Exceiption occurred, port:" + portName, e);
	}

	public String getQr() {
		return qr;
	}

	public void setQr(String qr) {
		this.qr = qr;
	}

	public long getQrDttm() {
		return qrDttm;
	}

	public void setQrDttm(long qrDttm) {
		this.qrDttm = qrDttm;
	}

	public static boolean isSameQrThread(long ttm) {
		return QR_THREAD_MS == ttm;
	}
}
