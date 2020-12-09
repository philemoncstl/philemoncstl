package hk.com.evpay.ct.tool;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class ToolUtil {
	public static void initTool(JComponent comp) {
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
