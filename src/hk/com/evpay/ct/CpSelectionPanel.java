package hk.com.evpay.ct;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class CpSelectionPanel extends CommonPanel{
	private List<CpPanel> cpList;	

	public CpSelectionPanel(CtrlPanel pnlCtrl) {
		super(pnlCtrl);
		
		cpList = new ArrayList<>();
		
		setLayout(new FlowLayout(FlowLayout.LEFT, config.getCpHgap(), config.getCpVgap()));

		CpPanel temp = null;
		for(int i = 0; i < 10; i ++) {
			temp = new CpPanel(pnlCtrl);
			temp.addMouseListener(pnlCtrl);
			cpList.add(temp);
			
			if(ct.getCpList().size() > i) {
				temp.setCp(ct.getCpList().get(i));
			}
			add(temp);
		}
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
	
	
	
}
