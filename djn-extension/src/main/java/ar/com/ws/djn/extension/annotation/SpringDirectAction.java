/**
 * 
 */
package ar.com.ws.djn.extension.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * @author Mauro Monti
 *
 */
@Component
@Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value={java.lang.annotation.ElementType.TYPE})
public abstract @interface SpringDirectAction {

	/**
	 * @return
	 */
	public abstract java.lang.String[] action() default {""};
	
}
