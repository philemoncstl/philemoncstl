package hk.com.evpay.ct.tool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JButton;

import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.Step3PrintReceipt;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

import hk.com.evpay.ct.util.iUC285Util;

public class ContactlessSettlementDialog extends JDialog{
	private Logger logger = Logger.getLogger(ContactlessSettlementDialog.class);
	
	public static void main(String[] args) throws IOException {
		ContactlessSettlementDialog dialog = new ContactlessSettlementDialog(null, false);
		
		dialog.setSize(980, 700);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		dialog.setAlwaysOnTop(true);
	}

	public ContactlessSettlementDialog(JFrame owner, boolean modal) {
		super(owner, modal);
		init();
	}

	private void init() {
		setUndecorated(true);
		setLayout(new BorderLayout());		
		
		//Center Panel
		JPanel pnl 	= new JPanel();
		pnl.setOpaque(false);
		pnl.setPreferredSize(new Dimension(960, 600));
		add(pnl, BorderLayout.CENTER);
		UiUtil.debugUi(pnl);

		JTextArea responseResult = new JTextArea();
		responseResult.setSize(600, 1000);
		responseResult.setLineWrap(true);
		
		JButton edcButton = new JButton("All Acquires");
		edcButton.addActionListener(new ActionListener() {
	      	public void actionPerformed(ActionEvent e) {
	      		responseResult.setText("Loading");
	      		JSONObject response = iUC285Util.doSettlement("EDC");
	      		responseResult.setText(response != null ? response.toString() : "No Response");
	      	}
    	});
		pnl.add(edcButton,BorderLayout.CENTER);
		
		JButton vmjButton = new JButton(" VMJ or VM Acquirer");
		vmjButton.addActionListener(new ActionListener() {
	      	public void actionPerformed(ActionEvent e) {
	      		responseResult.setText("Loading");
	      		JSONObject response = iUC285Util.doSettlement("VMJ");
	      		responseResult.setText(response != null ? response.toString() : "No Response");
	      	}
    	});
		pnl.add(vmjButton,BorderLayout.CENTER);
		
		JButton aeButton = new JButton("Amex Acquirer");
		aeButton.addActionListener(new ActionListener() {
	      	public void actionPerformed(ActionEvent e) {
	      		responseResult.setText("Loading");
	      		JSONObject response = iUC285Util.doSettlement("AE");
	      		responseResult.setText(response != null ? response.toString() : "No Response");
	      	}
    	});
		pnl.add(aeButton,BorderLayout.CENTER);
		
		JButton cupButton = new JButton("CUP Acquirer");
		cupButton.addActionListener(new ActionListener() {
	      	public void actionPerformed(ActionEvent e) {
      			responseResult.setText("Loading");
	      		JSONObject response = iUC285Util.doSettlement("CUP");
	      		responseResult.setText(response != null ? response.toString() : "No Response");
	      	}
    	});
		pnl.add(cupButton,BorderLayout.CENTER);
        pnl.add(responseResult); 
        
		//Sourth panel		
		pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));	
		pnl.setPreferredSize(new Dimension(950, 100));
		pnl.setOpaque(false);
		add(pnl, BorderLayout.SOUTH);
		
		JLabel lblClose = OctopusEnquiryDialog.createButton("close", "img/btn_no.png", 80, 582);
		lblClose.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				logger.debug("GUI - close button pressed");
				ContactlessSettlementDialog.this.dispose();
			}
		});
		pnl.add(lblClose);
		UiUtil.debugUi(pnl);
	}
}