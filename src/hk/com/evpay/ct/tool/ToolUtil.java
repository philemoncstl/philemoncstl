package hk.com.evpay.ct.tool;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;

import hk.com.evpay.ct.PostStep5StopChargingTapCard;

public class ToolUtil {
	private static final Logger logger = Logger.getLogger(ToolUtil.class);
	public static void initTool(JComponent comp) {
		logger.info("comp: " + comp.getInputMap());
		//Octopus Enquiry
		comp.getInputMap().put(KeyStroke.getKeyStroke("F1"), "OctEnquiry");
		comp.getActionMap().put("OctEnquiry", new OctopusEnquiryAction());
		
		
		//Reprint Receipt
		comp.getInputMap().put(KeyStroke.getKeyStroke("F2"), "ReprintReceipt");
		comp.getActionMap().put("ReprintReceipt", new ReprintReceiptAction());
		
		//Contactless Settlement
		comp.getInputMap().put(KeyStroke.getKeyStroke("F3"), "ContactlessSettlement");
		comp.getActionMap().put("ContactlessSettlement", new ContactlessSettlementAction());
		
		//Test Receipt
		comp.getInputMap().put(KeyStroke.getKeyStroke("F9"), "TestReceipt");
		comp.getActionMap().put("TestReceipt", new PrintTestReceipt());
				
		//Screen Capture
		comp.getInputMap().put(KeyStroke.getKeyStroke("F10"), "CapScreen");
		comp.getActionMap().put("CapScreen", new CapScreenAction());

		
	}
}
