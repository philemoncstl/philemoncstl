package hk.com.evpay.ct.test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class TestFont {
	public static void main(String[] args) {
		UiUtil.printFontList();
		
		JFrame f = new JFrame("Test Font");
		f.setLayout(new BorderLayout());
		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnl.setPreferredSize(new Dimension(1280, 2000));
		pnl.setMaximumSize(new Dimension(1280, 2000));
		f.add(new JScrollPane(pnl, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.CENTER);
		
		JLabel lbl = null;
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		LangUtil.setChinses();
		lbl = new JLabel("******你您的卡******" + LangUtil.getFontName());
		lbl.setPreferredSize(new Dimension(1200, 25));
		LangUtil.setFont(lbl, Font.PLAIN, 20);
		pnl.add(lbl);
	    for ( int i = 0; i < fonts.length; i++ ){
	    	lbl = new JLabel("你您的卡:" + fonts[i]);
	    	lbl.setFont(new Font(fonts[i], Font.PLAIN, 20));
	    	lbl.setPreferredSize(new Dimension(600, 25));
	    	pnl.add(lbl);
	    }
	    
		f.pack();
		f.setVisible(true);
	}
}
