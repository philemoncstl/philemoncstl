package hk.com.evpay.ct.util;

import java.awt.Font;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JComponent;

import org.apache.log4j.Logger;

import com.ckzone.util.StringUtil;

public class LangUtil {
	private static final Logger logger = Logger.getLogger(LangUtil.class);
	
	public static boolean LANG_EN = false;
	
	private static ResourceBundle bundleEn = null;
	
	private static ResourceBundle bundleZh = null;
	
	public static String FONT_EN = "Arial";
	
	public static String FONT_ZH = "Noto Sans CJK TC Regular";
	//public static String FONT_ZH = "PMingLiU";
	
	public static void main(String[] args) {
		setEnglish();
		System.out.println(getMsg("key"));
		
		setChinses();
		System.out.println(getMsg("key"));
	}
	
	private static ResourceBundle getBundle() {
		if(bundleEn == null || bundleZh == null) {
			logger.info("Initing i18n messages");
			Locale.setDefault(Locale.ENGLISH);
			bundleEn = ResourceBundle.getBundle("hk.com.evpay.ct.i18n.msg", new XMLResourceBundleControl());
			
			Locale.setDefault(Locale.CHINESE);
			bundleZh = ResourceBundle.getBundle("hk.com.evpay.ct.i18n.msg", new XMLResourceBundleControl());
		}
		
		return LANG_EN ? bundleEn : bundleZh;
	}

	public static void setEnglish() {
		logger.info("Lang set to EN");
		LANG_EN = true;
	}
	
	public static boolean isEnglish() {
		return LANG_EN;
	}
	
	public static void setChinses() {
		logger.info("Lang set to ZH");
		LANG_EN = false;
	}
	
	public static String getFontName() {
		return isEnglish() ? FONT_EN : FONT_ZH;
	}
	
	public static String getMsg(String key) {
		return getMsg(key, true);
	}
	
	public static String htmlText(boolean forceHtmlFormat, String text, String padding) {
		if(forceHtmlFormat || text.indexOf("\n") != -1) {
			text = text.replaceAll("\n", "<br>");
			text = "<html><div style=\"text-align: center;" + (padding == null ? "" : padding) + "\">" + text + "</div></html>";
			//logger.debug("******" + text);
		}
		
		return text;
	}
	
	public static String getMsg(String key, boolean displayErrorWhenNotFound) {
		return getMsg(key, false, null, displayErrorWhenNotFound);
	}
	
	public static String getMsg(String key, boolean forceHtmlFormat, String padding, boolean displayErrorWhenNotFound) {
		if(StringUtil.isEmpty(key)) {
			return "";
		}
		
		//logger.debug("getMsg()" + key);
		try {
			return htmlText(forceHtmlFormat, getBundle().getString(key), padding);	
		} catch (Exception e) {
			if(displayErrorWhenNotFound) {
				logger.error("Failed to get msg:" + key);
			}
			return key;
		}
	}
	
	
	public static void setFont(JComponent comp, int style, int fontSize) {
		comp.setFont(new Font(LangUtil.getFontName(), style, fontSize));
		//comp.setFont(new Font(comp.getFont().getFamily(), style, fontSize));
	}
}
