package hk.com.evpay.ct.tool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.apache.log4j.Logger;

import com.ckzone.util.DateUtil;

import hk.com.cstl.evcs.model.TranModel;
import hk.com.evpay.ct.Step3PrintReceipt;
import hk.com.evpay.ct.util.CtUtil;
import hk.com.evpay.ct.util.LangUtil;
import hk.com.evpay.ct.util.UiUtil;

public class ReprintReceiptDialog extends JDialog{
	private Logger logger = Logger.getLogger(ReprintReceiptDialog.class);
	
	private TransTableModel tblTranModel;
	private JTable tblTran;
	
	public static void main(String[] args) throws IOException {
		ReprintReceiptDialog dialog = new ReprintReceiptDialog(null, false);
		
		dialog.setSize(980, 700);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		dialog.setAlwaysOnTop(true);
	}

	public ReprintReceiptDialog(JFrame owner, boolean modal) {
		super(owner, modal);
		init();
	}

	private void init() {
		setUndecorated(true);
		setLayout(new BorderLayout());		
		
		//Center Panel
		JPanel pnl = new JPanel();
		pnl.setOpaque(false);
		pnl.setPreferredSize(new Dimension(960, 600));
		add(pnl, BorderLayout.CENTER);
		UiUtil.debugUi(pnl);
		
		tblTranModel = new TransTableModel();
		tblTranModel.setData(TranHistCtrl.getTranHist().getTrans());
		tblTran = new JTable(tblTranModel);
		tblTran.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		tblTran.setRowHeight(40);
		tblTran.setFont(new Font("Arial", Font.PLAIN, 24));
		tblTran.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);		
		tblTranModel.setColumnWidth(tblTran);
		
		//Payment Due (align right)
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		
		for(int i = 0; i < 5; i ++) {
			tblTran.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}
		
		tblTran.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
		
		//Reprint button
		Action reprint = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		        JTable table = (JTable)e.getSource();
		        int row = Integer.valueOf(e.getActionCommand());
		        final TranModel tm = tblTranModel.getData().get(row);
		        logger.debug("Reprint clicked on row:" + row + ", receiptNo:" + tm.getReceiptNo());
		        Step3PrintReceipt.printReceipt(tm, true);
		    }
		};		 
		ButtonColumn buttonColumn = new ButtonColumn(tblTran, reprint, 6);
		buttonColumn.setMnemonic(KeyEvent.VK_D);
		
		JScrollPane scrollPane = new JScrollPane(tblTran);
		scrollPane.setPreferredSize(new Dimension(960, 590));
		tblTran.setFillsViewportHeight(true);
		pnl.add(scrollPane);
		
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
				ReprintReceiptDialog.this.dispose();
			}
		});
		pnl.add(lblClose);
		UiUtil.debugUi(pnl);
	}
}

class TransTableModel extends AbstractTableModel {
	private static final int[] COL_WIDTH = new int[] {45, 140, 150, 150, 250, 100, 100};
	private String[] columnNames;
	private List<TranModel> data;
	
	
	public TransTableModel() {
		columnNames = new String[] {
			"#",
			LangUtil.getMsg("cpNoLabel"),
			LangUtil.getMsg("paymentMethod"),
			LangUtil.getMsg("receiptNo"),
			LangUtil.getMsg("tranDatetime"),
			LangUtil.getMsg("amount"),
			LangUtil.getMsg("reprint")
		};
	}
	
	public void setColumnWidth(JTable table) {
		TableColumnModel tcm = table.getColumnModel();
		for(int i = 0; i < getColumnCount(); i ++) {
			tcm.getColumn(i).setPreferredWidth(COL_WIDTH[i]);
		}
	}

	@Override
	public int getRowCount() {
		return data == null ? 0 : data.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return getColumnNames()[column];
	}
	
	public String[] getColumnNames() {
		return columnNames;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 6;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TranModel tran = data.get(rowIndex);
		if(tran == null) {
			return "";
		}
		
		String val = "";
		switch (columnIndex) {
		case 0:
			val = "" + (rowIndex + 1);
			break;
		case 1:
			val = tran.getCpNo();
			break;
		case 2:
			val = tran.getPayMethodCode();
			break;
		case 3:
			val = tran.getReceiptNo();
			break;
		case 4:
			val = tran.getStartDttm() == null ? "" : DateUtil.formatDateTime(tran.getTranDttm(), true);			
			break;
		case 5:
			val = tran.getAmt() == null ? "0" : CtUtil.bigDecimalToString(tran.getAmt()) + " ";
			break;
		case 6:
			val = LangUtil.getMsg("reprint");
			break;
		default:
			break;
		}
		
		return val;
	}

	public List<TranModel> getData() {
		return data;
	}

	public void setData(List<TranModel> data) {
		this.data = data;
	}
}