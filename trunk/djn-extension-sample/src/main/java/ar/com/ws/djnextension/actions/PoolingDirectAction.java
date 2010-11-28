/**
 * 
 */
package ar.com.ws.djnextension.actions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.softwarementors.extjs.djn.config.annotations.DirectPollMethod;

import ar.com.ws.djn.extension.annotation.SpringDirectAction;

/**
 * @author Mauro Monti
 *
 */
@SpringDirectAction
public class PoolingDirectAction {

	/**
	 * @param pParameters
	 * @return
	 */
	@DirectPollMethod(event="message")
	public String handleMessagePoll(final Map<String,String> pParameters) {
		final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		final String now = sdf.format(new Date());
		
		return now; 
	}

	
}
