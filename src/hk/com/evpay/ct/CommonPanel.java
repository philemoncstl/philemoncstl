package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import hk.com.cstl.evcs.ct.PayMethodModel;
import hk.com.cstl.evcs.model.CtModel;
import hk.com.evpay.ct.i18n.I18nButtonLabel;
import hk.com.evpay.ct.i18n.I18nLabel;
import hk.com.evpay.ct.i18n.PayButton;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class CommonPanel extends JPanel{
	private static final Logger logger = Logger.getLogger(CommonPanel.class);
	
	public static final int BTN_WIDTH = 240;
	public static final int BTN_HEIGHT = 81;
	public static final int BTN_LEFT_PADDING = 44;
	
	protected CtModel ct;
	protected CtConfig config;
	
	protected CtrlPanel pnlCtrl;
	
	public CommonPanel(CtrlPanel pnlCtrl) {
		super();
		ct = CtUtil.getCt();
		config = CtUtil.getConfig();
		this.pnlCtrl = pnlCtrl;
		
		UiUtil.debugUi(this);
		
		setOpaque(false);
	}
	
	public I18nLabel createLabel(String msgCode, String text, int x, int y, int w, int h) {
		I18nLabel lbl = new I18nLabel(msgCode, text);
		lbl.setBounds(calcBoundsLabel(x, y, w, h));
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		lbl.setVerticalAlignment(SwingConstants.CENTER);
		return lbl;
	}
	
	public Rectangle calcBoundsLabel(int x, int y, int w, int h) {
		return new Rectangle(x - config.getWestWidth() - config.SCREEN_INSET.left, 
				y - config.getNorthHeight() - config.SCREEN_INSET.top, w, h);
	}
	
	public I18nButtonLabel createButton(String msgCode, String iconPath, int x, int y) {
		return createButton(msgCode, iconPath, x, y, Color.WHITE, BTN_LEFT_PADDING);
	}
	
	public I18nButtonLabel createButton(String msgCode, String iconPath, int x, int y, Color foreground, int bthLeftPadding) {
		I18nButtonLabel btn = new I18nButtonLabel(msgCode, bthLeftPadding, iconPath);
		LangUtil.setFont(btn, Font.PLAIN, 30);
		btn.setForeground(foreground);
		btn.setBounds(calcBoundsButton(x, y));
		return btn;
	}
	
	public I18nButtonLabel createButton(String iconPath, int x, int y, int w, int h) {
		return createButton("", iconPath, x, y, w, h);
	}
	
	public I18nButtonLabel createButton(String msgCode, String iconPath, int x, int y, int w, int h) {
		I18nButtonLabel btn = new I18nButtonLabel(msgCode, 0, iconPath, w, h);
		btn.setBounds(calcBoundsLabel(x, y, w, h));
		return btn;
	}
	
	public Rectangle calcBoundsButton(int x, int y) {
		return new Rectangle(x - config.getWestWidth() - config.SCREEN_INSET.left, 
				y - config.getNorthHeight() - config.SCREEN_INSET.top, BTN_WIDTH, BTN_HEIGHT);
	}
	
	public int getBackgroundIdx() {
		return CtrlPanel.BG_HOME;
	}
	
	public String getTitleMsgKey() {
		return "";
	}
	
	public String getCardName() {
		return this.getClass().getSimpleName();
	}
	
	public CtModel getCt() {
		return ct;
	}
	public void setCt(CtModel ct) {
		this.ct = ct;
	}
	public CtConfig getConfig() {
		return config;
	}
	public void setConfig(CtConfig config) {
		this.config = config;
	}
	
	public void onDisplay(CpPanel cp) {
		logger.info(getClass().getSimpleName() + " onDisplay, cp:" + (cp == null ? "" : cp.getCp().getCpNo()));
		pnlCtrl.goHomeCountDown(this);
	}
	
	
	public PayButton[] getPayButtons() {
//		int x = 1150;
//		int y = 682;
//		
//		List<PayMethodModel> list = CtUtil.getPayConfig().getEnabledMethods();
//		PayButton[] btns = new PayButton[list.size()];
//		for(int i = 0; i < list.size(); i ++) {
//			btns[i] = new PayButton(list.get(i));
//			btns[i].setBounds(calcBoundsLabel(x, y, 84, 84));
//			x -= 88;
//		}
		
		int x = 182;
		int y = 450;
		
		List<PayMethodModel> list = CtUtil.getPayConfig().getEnabledMethods();
		PayButton[] btns = new PayButton[list.size()];
		for(int i = 0; i < list.size(); i ++) {
			btns[i] = new PayButton(list.get(i));
			btns[i].setBounds(calcBoundsLabel(x, y, 312, 179));
			x += 320;
		}
		
		return btns;
	}
}
