package hk.com.evpay.ct.i18n;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.JLabel;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class I18nLabel extends JLabel implements I18nSupport{
	private static final Logger logger = Logger.getLogger(I18nLabel.class);
	
	protected String msgCode;
	protected String[] parms;
	
	public I18nLabel() {
		UiUtil.debugUi(this);
	}
	
	public I18nLabel(String msgCode, String text) {
		this(msgCode, text, null);
	}
	
	public I18nLabel(String msgCode, String text, String[] parms) {
		this();
		this.msgCode = msgCode;
		this.parms = parms; 
		
		langChanged();
	}

	@Override
	public void langChanged() {
		LangUtil.setFont(this, this.getFont().getStyle(), this.getFont().getSize());
		updateText();
	}
	
	public void updateText() {
		this.setText(msgCode == null ? super.getText() : 
			(parms == null ? LangUtil.getMsg(msgCode) : String.format(LangUtil.getMsg(msgCode), this.parms)));
		//logger.debug(msgCode + "=" + this.getText());
	}

	public String getMsgCode() {
		return msgCode;
	}

	public void setMsgCode(String msgCode) {
		if(this.msgCode == null || !this.msgCode.equals(msgCode)) {
			this.msgCode = msgCode;
			updateText();
		}
	}

	public String[] getParms() {
		return parms;
	}

	public void setParms(String[] parms) {
		this.parms = parms;
		updateText();
	}
	
	public void setParms(String parm) {
		setParms(new String[]{parm});
	}
	
	public void setParms(String parm1, String parm2) {
		setParms(new String[]{parm1, parm2});
	}
	
	public void setParms(String parm1, String parm2, String parm3) {
		setParms(new String[]{parm1, parm2, parm3});
	}
	
	public void setParms(int parm) {
		setParms(new String[]{String.valueOf(parm)});
	}
	
	public void setParms(int parm1, int parm2) {
		setParms(new String[]{String.valueOf(parm1), String.valueOf(parm2)});
	}
	
	public void setParm(BigDecimal parm) {
		if(parm == null) {
			logger.debug("Null value passed, default to zero.");
			parm = BigDecimal.ZERO;
		}
		setParm(parm, 2, RoundingMode.DOWN);
	}
	
	public void setParm(BigDecimal parm, int digit) {
		setParm(parm, digit, RoundingMode.DOWN);
	}
	
	
	public void setParm(BigDecimal parm, int digit, RoundingMode mode) {
		if(parm != null) {
			setParms(new String[]{String.valueOf(parm.setScale(digit, mode))});
		}
	}
}
