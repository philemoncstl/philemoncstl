package hk.com.evpay.ct.i18n;

import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class I18nButtonLabel extends I18nLabel{
	private static final Logger logger = Logger.getLogger(I18nButtonLabel.class);
	
	private String padding = "";

	public I18nButtonLabel(String msgCode, int leftPadding, String iconPath) {
		this.msgCode = msgCode;
		this.padding = "padding-left:" + leftPadding + "px;";
		setIcon(new ImageIcon(iconPath));
		setHorizontalTextPosition(SwingConstants.CENTER);
		setVerticalTextPosition(SwingConstants.CENTER);
		setForeground(Color.BLACK);
		updateText();
		
		UiUtil.debugUi(this);
	}
	
	@Override
	public void updateText() {
		String text = "";
		
		try {
			if(this.msgCode == null) {
				text = "";
			}
			else {
				text = LangUtil.getMsg(this.msgCode, true, padding, true);
				if(text.indexOf("%;") == -1 && this.parms != null && this.parms.length > 0) {
					text = String.format(text, this.parms);
				}
			}
		} catch (Exception e) {
			logger.debug("msgCode:" + msgCode + ", parms:" + parms);
			if(this.parms != null && this.parms.length > 0) {
				for(String s : this.parms) {
					logger.debug(s);
				}
			}
			logger.warn("Failed to convert:" + this.msgCode, e);
			text = this.msgCode == null ? "" : LangUtil.getMsg(this.msgCode, true, padding, true);
		}
		
		//logger.debug("Updated text for" + this.msgCode + "=" + text);
		super.setText(text);
	}
}
