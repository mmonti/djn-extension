/**
 * 
 */
package ar.com.ws.djnextension.response;

import java.io.Serializable;

/**
 * @author Mauro Monti
 *
 */
public class Response implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2274786953124633855L;
	
	/**
	 * 
	 */
	private boolean success = true;
	
	/**
	 * 
	 */
	private Object data = new Object();

	/**
	 * @param pData
	 */
	public Response(Object pData) {
		this.data = pData;
	}
	
	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Object data) {
		this.data = data;
	}
}