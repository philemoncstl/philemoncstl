package hk.com.evpay.ct;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.log4j.Logger;


import hk.com.cstl.evcs.model.CpModel;

public class CpSelectionPanel extends CommonPanel{
	private static Logger logger = Logger.getLogger(CpSelectionPanel.class);
	private List<CpPanel> cpList;
	private List<CpModel> cpModelList;
	private int lastCount = -1;
	public CpSelectionPanel(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		ct.getCpList().sort(Comparator.comparing(CpModel::getCpNo));
		cpModelList = ct.getCpList();
		cpList = new ArrayList<>();
		CpPanel temp = null;
		for(int i = 0; i < cpModelList.size(); i++) {
			temp = new CpPanel(pnlCtrl);
			temp.addMouseListener(pnlCtrl);
			logger.info("cpNo: " + cpModelList.get(i).getCpNo());
			cpList.add(temp);
			
		}
		setLayout(new FlowLayout(FlowLayout.LEFT, config.getCpHgap(), config.getCpVgap()));
		updateDisplayCpList(0);
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
	
	public void updateDisplayCpList(int count) {
		if(lastCount == count) {
			return;
		}
		lastCount = count;
		removeAll();
		for(int i = count * 10; i < ((count * 10 + 10) <= cpModelList.size() ? count * 10 + 10 : cpModelList.size()) ; i ++) {
			CpPanel temp = cpList.get(i);
			temp.setCp(cpModelList.get(i));
			add(temp);
		}
		updateUI();
	}
	
	
	
}
