package hk.com.cstl.evcs.ct;

import hk.com.cstl.evcs.ocpp.eno.PayMethod;

public class PayMethodModel implements Comparable<PayMethodModel>{
	private PayMethod payMethod;
	private String name;
	private String nameChi;
	private int seq;
	
	private boolean enabled;
	private String iconName;
	
	public PayMethodModel() {
		super();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNameChi() {
		return nameChi;
	}
	public void setNameChi(String nameChi) {
		this.nameChi = nameChi;
	}
	public int getSeq() {
		return seq;
	}
	public void setSeq(int seq) {
		this.seq = seq;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getIconName() {
		return iconName;
	}
	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	public PayMethod getPayMethod() {
		return payMethod;
	}

	public void setPayMethod(PayMethod payMethod) {
		this.payMethod = payMethod;
	}

	@Override
	public int compareTo(PayMethodModel o) {
		return this.getSeq() - o.getSeq();
	}	
}
