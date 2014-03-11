package org.sakaiproject.feedback.tool.entityproviders;

public class Attachment {
	
	public String mimeType = "";
	
	public transient byte[] data;
	
	public String name = "";
	
	public Attachment(String name,String contentType, byte[] bs) {

		this.name = name;
		mimeType = contentType;
		
		if(name.endsWith(".doc")) mimeType = "application/msword";
		data = bs;
	}
	
	public Attachment() {} 
}
