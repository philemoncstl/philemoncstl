package hk.com.evpay.ct.i18n;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JPanel;

import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class FieldPanel extends JPanel{
	public static int FIELD_V_GAP = 5;
	
	public static int FIELD_HEIGHT = 30;
	public static int FEILD_WIDTH = 820;
	public static Dimension DIM = new Dimension(FEILD_WIDTH, FIELD_HEIGHT);
	public static int FONT_SIZE = 22;
	
	public static int LABEL_WIDTH = 500;	//borrow 40 from colon
	public static int COLON_GAP = 4;
	public static int COLON_WIDTH = 60;

	private I18nLabel lbl;
	private I18nLabel val;
	
	public FieldPanel(String lblMsgCode, String valMsgCode) {
		this(lblMsgCode, "", valMsgCode, "");
	}
	
	public FieldPanel(String lblMsgCode, String lblDefaultVal, String valMsgCode, String valDefaultVal) {
		super();
		
		setLayout(null);
		setOpaque(false);
		UiUtil.debugUi(this);
		
		int x = 0;
		//Label
		lbl = new I18nLabel(lblMsgCode, lblDefaultVal);
		add(lbl);
		LangUtil.setFont(lbl, Font.PLAIN, getFieldFontSize());
		lbl.setBounds(x, 0, LABEL_WIDTH, FIELD_HEIGHT);
		
		//Colon
		x = x + LABEL_WIDTH + COLON_GAP;
		JLabel lblColon = new JLabel(":");
		add(lblColon);
		lblColon.setFont(new Font(LangUtil.FONT_EN, Font.BOLD, getFieldFontSize()));
		lblColon.setBounds(x, 0, COLON_WIDTH, FIELD_HEIGHT);
		UiUtil.debugUi(lblColon);
		
		//Value
		x += COLON_WIDTH + COLON_GAP;
		val = new I18nLabel(valMsgCode, valDefaultVal);
		add(val);
		LangUtil.setFont(val, Font.PLAIN, getFieldFontSize());
		val.setBounds(x, 0, FEILD_WIDTH - x, FIELD_HEIGHT);
	}
	
	public Rectangle setFieldLocation(int x, int y, boolean firstField) {
		setBounds(x, (firstField ? y : y + DIM.height + FIELD_V_GAP), DIM.width, DIM.height);
		return getBounds();
	}
	
	public int getFieldFontSize() {
		return FONT_SIZE;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return DIM;
	}

	public I18nLabel getLbl() {
		return lbl;
	}

	public void setLbl(I18nLabel lbl) {
		this.lbl = lbl;
	}

	public I18nLabel getVal() {
		return val;
	}

	public void setVal(I18nLabel val) {
		this.val = val;
	}
}
