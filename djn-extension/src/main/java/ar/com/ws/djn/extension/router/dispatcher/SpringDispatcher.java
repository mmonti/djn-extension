/**
 * 
 */
package ar.com.ws.djn.extension.router.dispatcher;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.softwarementors.extjs.djn.api.RegisteredAction;
import com.softwarementors.extjs.djn.router.dispatcher.DispatcherBase;

/**
 * @author eM3
 *
 */
@Component
public class SpringDispatcher extends DispatcherBase implements ApplicationContextAware {

	/**
	 * 
	 */
	private ApplicationContext applicationContext;
	
	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.router.dispatcher.DispatcherBase#getActionInstance(com.softwarementors.extjs.djn.api.RegisteredAction)
	 */
	@Override
	protected Object getActionInstance(RegisteredAction pAction) {
		final Object actionInstance = this.applicationContext.getBean(pAction.getActionClass());
		return actionInstance;
	}

	/* (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext pApplicationContext) throws BeansException {
		this.applicationContext = pApplicationContext;
	}

}
