package hk.com.cstl.evcs.ct;

import java.util.Date;

import hk.com.cstl.evcs.model.CtModel;

public class IucEventLog implements java.io.Serializable {
	private Integer eId;
	private CtModel ctId;
	private String eventType;
	private Date eventDttm;
	private String remark;
	private Date creDttm;
	public Integer geteId() {
		return eId;
	}
	public void seteId(Integer eId) {
		this.eId = eId;
	}

	public CtModel getCtId() {
		return ctId;
	}
	public void setCtId(CtModel ctId) {
		this.ctId = ctId;
	}
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public Date getEventDttm() {
		return eventDttm;
	}
	public void setEventDttm(Date eventDttm) {
		this.eventDttm = eventDttm;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public Date getCreDttm() {
		return creDttm;
	}
	public void setCreDttm(Date creDttm) {
		this.creDttm = creDttm;
	}
		
}
