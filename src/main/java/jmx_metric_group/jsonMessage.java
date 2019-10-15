package jmx_metric_group;

import java.util.Date;
import java.util.List;
import java.io.Serializable;


public class jsonMessage {
  
	
	private String topic;
	private String partition;
	private String consumer_group;
	private String current_offset;
    private String _host;
	private String receiveddate;
	private String beanName;
	private String attribute;
	private String attributeValue;
	
	private long log_end_offset;
	private long lag;
	
	private double total_lag;
	
	
	public jsonMessage() {
		
	}
	
	
	// String Getters a -z
	
	public String getCurrentOffset() {
		return current_offset;
	}
	
	public String getConsumerGroup() {
		return consumer_group;
	}
	
	public String getHost() {
		return _host;
	}
		
	public String getPartition() {
		return partition;
	}
	
	public String getReceivedDate() {
		return receiveddate;
	}

	public String getTopic() {
		return topic;
	}
	
	public String getAttribute() {
		return attribute;
	}
	
	public String getAttributeValue() {
		return attributeValue;
	}
	
	// Long Getters a -z
	

	public long getLag() {
		return lag;
	}
	
	public long getLogEndOffset() {
		return log_end_offset;
	}
	
	public double getAverageLag(){
		return total_lag;
	}

	
	// String Setters a -z
	
	public void setCurentOffset(String _current_offset) {
		this.current_offset = _current_offset;
	}
	
	public void setConsumerGroup(String _consumer_group) {
		this.consumer_group = _consumer_group;
	}
	
	public void setHost(String host) {
		this._host = host;
	}
		
	public void setPartition(String _partition) {
		this.partition = _partition;
	}
	
	public void setReceivedDate(String received_date) {
		this.receiveddate= received_date;
	}

	public void setTopic(String _topic) {
		this.topic = _topic;
	}
	
	public void setBeanName(String _beanName) {
		this.beanName = _beanName;
	}
	
	public void setAttribute(String _attribute) {
		this.attribute = _attribute;
	}
	
	public void setAttributeValue(String _attributeValue) {
		this.attributeValue = _attributeValue;
	}
	
	// Long Setters a -z
	

	public void setLag(long _lag) {
		this.lag = _lag;
	}
	
	public void setLogEndOffset(long _log_end_offset) {
		this.log_end_offset = _log_end_offset;
	}
	
	public void setAverageLag(double _totalLag){
		this.total_lag = _totalLag;
	}
	
	public String toString() {
		String s = "{\"host\":" + "\"" + getHost()  + "\"," + "\n";
		s+= "\"topic\":" + "\"" + getTopic() + "\"," + "\n";
		s+= "\"consumer_group\":" + "\"" + getConsumerGroup()  + "\"," + "\n";
		s+= "\"partition\":" + getPartition()   + "," + "\n";
		s+= "\"current_offset\":" + getCurrentOffset()  + "," + "\n";
		s+= "\"log_end_offset\":" + getLogEndOffset()  + "," + "\n";
		s+= "\"lag\":" + getLag()  + "," + "\n";
		s+= "\"average_lag\":" +  getAverageLag()  +","  + "\n";
		s+= "\"received_date\":" + "\"" + getReceivedDate()  + "\"}"  + "\n";
		
		return s;
		
	}
}
