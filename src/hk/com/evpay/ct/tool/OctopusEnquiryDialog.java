package hk.com.evpay.ct.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import com.ckzone.octopus.OctReturn;
import com.ckzone.octopus.Poll;
import com.ckzone.octopus.util.OctUtil;
import com.ckzone.util.StringUtil;

import hk.com.evpay.ct.CommonPanel;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class OctopusEnquiryDialog extends JDialog{
	private Logger logger = Logger.getLogger(OctopusEnquiryDialog.class);
	
	private I18nLabel lblMsg = null;
	private JPanel pnlMsg;
	
	private JLabel lblResult = null;
	private JPanel pnlResult;
	
	private long closeTimeMs = -1;
	
	public static void main(String[] args) throws IOException {
		OctopusEnquiryDialog dialog = new OctopusEnquiryDialog(null, false);
		
		dialog.setSize(920, 500);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		dialog.setAlwaysOnTop(true);
	}
	
	public OctopusEnquiryDialog(JFrame owner, boolean modal) {
		super(owner, modal);
		init();
	}

	private void init() {
		setUndecorated(true);
		JPanel c = new JPanel(new BorderLayout());
		
		c.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 3));
		c.setBackground(Color.WHITE);
		setContentPane(c);
		
		
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
		pnl.setOpaque(false);
		c.add(pnl, BorderLayout.CENTER);
		
		//lblMsg = StatusInfoPanel.createBlkMsgLabel220("presentOctopusForEnquiry");
		lblMsg = new I18nLabel("presentOctopusForEnquiry", "");
		LangUtil.setFont(lblMsg, Font.PLAIN, 30);
		lblMsg.setPreferredSize(new Dimension(875, 220));
		lblMsg.setHorizontalAlignment(SwingConstants.CENTER);
		pnlMsg = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 100));
		pnlMsg.setOpaque(false);
		pnlMsg.add(lblMsg);
		pnl.add(pnlMsg);
		
		lblResult = new JLabel();
		lblResult.setPreferredSize(new Dimension(860, 400));
		pnlResult = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
		pnlResult.setOpaque(false);
		pnlResult.add(lblResult);
		pnl.add(pnlResult);
		
		UiUtil.debugUi(pnl);
		UiUtil.debugUi(lblMsg);
		UiUtil.debugUi(lblResult);
		
		pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));		
		pnl.setOpaque(false);
		c.add(pnl, BorderLayout.SOUTH);
		
		JLabel lblClose = createButton("close", "img/btn_no.png", 180, 682);
		lblClose.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				logger.debug("GUI - close button pressed");
				OctopusEnquiryDialog.this.setVisible(false);
			}
		});
		pnl.add(lblClose, BorderLayout.WEST);
		UiUtil.debugUi(pnl);
		
		//Screen Capture
		pnl = (JPanel)this.getContentPane();
		pnl.getInputMap().put(KeyStroke.getKeyStroke("F10"), "CapScreen");
		pnl.getActionMap().put("CapScreen", new CapScreenAction());
	}
	
	public static I18nButtonLabel createButton(String msgCode, String iconPath, int x, int y) {
		I18nButtonLabel btn = new I18nButtonLabel(msgCode, CommonPanel.BTN_LEFT_PADDING, iconPath);
		LangUtil.setFont(btn, Font.PLAIN, 30);
		btn.setForeground(Color.WHITE);
		return btn;
	}
	
	private void showErrorMessageOctopus(int returnCode) {
		showErrorMessageOctopus("ERR" + returnCode);
	}
	
	
	private void showErrorMessageOctopus(String errorCode) {
		lblMsg.setMsgCode(errorCode);
	}
	
	private void handleOctopusError(int returnCode) {
		switch (returnCode) {
		case 100019:	//Card is blocked
		case 100021:	//The last add value date is greater than 1000 days
		case 100024:	//This card not accepted
		case 100035:	//Card recover error
			showErrorMessageOctopus(returnCode);
			//OctUtil.playFailTone();
			break;
		case 100016:	//Card read error
		case 100017:	//Card write error
		case 100020:	//Card is not found after Poll (deduct/add)
		case 100034:	//Card authentication error	
			showErrorMessageOctopus(returnCode);
			break;
		case 100022:	//Incomplete transaction.
		case 100025:	//Incomplete transaction.
			showErrorMessageOctopus(returnCode);
			break;
		case 100048:	//Insufficient fund
			showErrorMessageOctopus(returnCode);
			break;
		case 100049:	//Remaining value exceeds limit
			showErrorMessageOctopus(returnCode);
			break;
			
			
		case 100032:	//No card is detected. //Do Nothing
			break;			
		case 100023:	//Transaction Log full
			OctUtil.xFile();
			break;
		case 100050:	//Quota exceeded, SP to contact Octopus representative for assistance
			break;
		case 100051:	//Invalid Controller ID
			showErrorMessageOctopus(returnCode);
			break;
		case 100066: 	// System time error
			OctUtil.syncTime();
			break;
		default:
			showErrorMessageOctopus("ERROctopusOthers");
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			logger.error(e);
		}
		
		if(returnCode == 100001 || returnCode == 100005) {
			logger.info("handleOctopusError:" + returnCode);
			OctUtil.initComm(0, 0, 0);
		}
	}
	
	public void startPollCard() {
		closeTimeMs = System.currentTimeMillis() + CtUtil.getConfig().getGoHomeDelaySec() * 1000;
		
		logger.info("start poll card");
		LangUtil.setFont(lblResult, Font.PLAIN, 24);
		pnlResult.setVisible(false);
		pnlMsg.setVisible(true);
		
		new Thread() {
			@Override
			public void run() {
				logger.info("thread started");
				long timeout = System.currentTimeMillis() + CtUtil.getConfig().getOctopusPollTimeoutSec() * 1000;
				//long timeout = Long.MAX_VALUE;	//TODO
				int count = 0;
				OctReturn rwlReturn = null;
				boolean success = false;
				
				do {
					if(OctUtil.isOnline()) {
						logger.debug("Polling:" + (++count));
						rwlReturn = OctUtil.poll((char)2, (char)10);
						if (rwlReturn.getReturnCode() > 100000) {
							handleOctopusError(rwlReturn.getReturnCode());
						} 
						else {
							success = true;
							Poll pollData = (Poll) rwlReturn.getReturnData();
							pollData.processData();
							
							logger.info("Card no:" + pollData.getCardId() + ", idm:" + pollData.getIdm() + ", Acc Balance:" + CtUtil.getAmountStr(rwlReturn.getReturnCode()));				
	
							//lblMsg.setMsgCode("Acc Bal:" + CtUtil.getAmountStr(rwlReturn.getReturnCode()));
							
							showTransHist(pollData, rwlReturn.getReturnCode());
							
							//display at lease 10 seconds
							closeTimeMs = Math.max(closeTimeMs, System.currentTimeMillis() + 10000);
							
							break;
						}
					}
					else {
						showErrorMessageOctopus("ERROctopusOthers");
						logger.info("Octopus offline, retry after 1000 ms");
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
					}
				} while (timeout >= System.currentTimeMillis() && lblMsg.isShowing());
				
				if(!success) {
					OctopusEnquiryDialog.this.setVisible(false);
				}
			}
		}.start();
		
		new Thread() {
			public void run() {
				logger.info("Close thread - started");
				while(System.currentTimeMillis() < closeTimeMs) {
					if(OctopusEnquiryDialog.this.isShowing()) {
						try {
							Thread.sleep(200);
						}catch(Exception e) {
							logger.error(e);
						}
					}
					else {
						break;
					}
				}
				
				if(OctopusEnquiryDialog.this.isShowing()) {
					logger.info("Close thread - close dialog now");
					OctopusEnquiryDialog.this.setVisible(false);
				}
				logger.info("Close thread - ended");
			};
		}.start();
	}
	
	private void showTransHist(Poll pd, int rv) {
		
		StringBuffer html = new StringBuffer();
		html.append("<html>");
		html.append("<table style=\"width: 600px;\" cellpadding=\"2\" cellspacing=\"2\">");
		html.append("<tr><td colspan=\"4\">" + LangUtil.getMsg("octopusNo") + ": " + pd.getCardId() + "</td></tr>");
		if(!pd.isSmartOctopus()) {
			html.append("<tr><td colspan=\"4\">" + LangUtil.getMsg("octopusRemainingValue") + ": $" + CtUtil.getAmountStr(rv) + "</td></tr>");
		}
		html.append("<tr><td colspan=\"4\">&nbsp;</td></tr>");
		html.append("<tr><td>" + LangUtil.getMsg("number") + "</td><td>" + LangUtil.getMsg("transactionDatetime") 
				+ "</td><td style=\"text-align:right;\">" + LangUtil.getMsg("amount") + "</td><td style=\"text-align:right;\">" + LangUtil.getMsg("deviceId") + "</td></tr>");
		
		String tran = pd.getTransactions();
		if(!StringUtil.isEmpty(tran)) {
			String[] trans = tran.split(",");
			logger.debug("Trans details, length:" + trans.length + ", dtl:" + tran);
			
			//show last 4 trans only
			//[Card Log n] = <SP Type>,<Transaction Amt>,<Transaction Time >,<Machine ID>,<Service Info>
			int noOfTrans = trans.length / 5;
			noOfTrans = Math.min(noOfTrans, 4);
			
			if(noOfTrans > 0) {
				String tranDttm;
				String amount;
				String devId;
				for(int i = 0; i < noOfTrans; i ++) {
					amount = OctUtil.getTransctionAmount(trans[1 + (i * 5)]);					
					
					tranDttm = OctUtil.getTransctionDateTime(trans[2 + (i * 5)]);
					try {
						devId= Integer.toHexString(Integer.parseInt(trans[3 + (i * 5)])).toUpperCase();
					} catch (Exception e) {
						devId = trans[3 + (i * 5)];
						logger.error("Failed to decode device ID!", e);
					}
					
					String readerId = OctUtil.DEV_ID;
					boolean sameDev = readerId != null && readerId.indexOf(devId) != -1;
					html.append("<tr><td>" + (i + 1) + (sameDev ? " #" : "") + "&nbsp;&nbsp;</td><td>" + 
							tranDttm + "&nbsp;&nbsp;</td><td style=\"text-align:right;\">" + 
							amount + "</td><td style=\"text-align:right;\">" + 
							devId + "</td></tr>");
				}
			}
		}		
		
		html.append("</table>");
		html.append("</html>");
		
		logger.debug(html.toString());
		
		pnlMsg.setVisible(false);
		lblResult.setText(html.toString());
		pnlResult.setVisible(true);
		
		new Thread() {
			public void run() {
				try {
					long timeout = CtUtil.getConfig().getGoHomeDelaySec() * 1000;
					logger.debug("Close after " + timeout + " sec");
					sleep(timeout * 1000);
					OctopusEnquiryDialog.this.dispose();
				} catch (Exception e) {
					logger.error("Failed to close enquiry screen.", e);
				}
			}
		}.start();
	}

	public I18nLabel getLblMsg() {
		return lblMsg;
	}

	public void setLblMsg(I18nLabel lblMsg) {
		this.lblMsg = lblMsg;
	}
}
