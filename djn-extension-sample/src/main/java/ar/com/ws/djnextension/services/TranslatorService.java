/**
 * 
 */
package ar.com.ws.djnextension.services;

import com.google.api.translate.Language;

/**
 * @author Mauro Monti
 *
 */
public interface TranslatorService {

	/**
	 * @param pText
	 * @param pFromLang
	 * @param pToLang
	 * @return
	 */
	String translate(String pText, Language pFromLang, Language pToLang);
	
}
