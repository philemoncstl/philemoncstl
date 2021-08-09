package hk.com.evpay.ct;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import hk.com.cstl.evcs.model.CpModel;

public class CpSelectionPanel extends CommonPanel{
	private static Logger logger = Logger.getLogger(CpSelectionPanel.class);
	private List<CpPanel> cpList;
	private List<CpModel> cpModelList;

	public CpSelectionPanel(CtrlPanel pnlCtrl, int count) {
		super(pnlCtrl);
		ct.getCpList().sort(Comparator.comparing(CpModel::getCpNo));
		cpModelList = ct.getCpList();
		cpList = new ArrayList<>();
		setLayout(new FlowLayout(FlowLayout.LEFT, config.getCpHgap(), config.getCpVgap()));
		updateCpList(count);
//		CpPanel temp = null;
//		for(int i = count * 10; i < ((count * 10 + 10) <= cpModelList.size() ? count * 10 + 10 : cpModelList.size()) ; i ++) {
//			temp = new CpPanel(pnlCtrl);
//			temp.addMouseListener(pnlCtrl);
//			logger.info("cpNo: " + cpModelList.get(i).getCpNo());
//			cpList.add(temp);
//			temp.setCp(ct.getCpList().get(i));
//			add(temp);
//		}
	}
	
	@Override
	public int getBackgroundIdx() {
		return CtrlPanel.BG_WITH_TITLE;
	}
	
	@Override
	public String getTitleMsgKey() {
		return "selectCp";
	}

	public List<CpPanel> getCpList() {
		return cpList;
	}

	public void setCpList(List<CpPanel> cpList) {
		this.cpList = cpList;
	}
	
	public void updateCpList(int count) {
		removeAll();
		CpPanel temp = null;
		for(int i = count * 10; i < ((count * 10 + 10) <= cpModelList.size() ? count * 10 + 10 : cpModelList.size()) ; i ++) {
			temp = new CpPanel(pnlCtrl);
			temp.addMouseListener(pnlCtrl);
			logger.info("cpNo: " + cpModelList.get(i).getCpNo());
			cpList.add(temp);
			temp.setCp(cpModelList.get(i));
			add(temp);
		}
		updateUI();
	}
	
	
	
}
