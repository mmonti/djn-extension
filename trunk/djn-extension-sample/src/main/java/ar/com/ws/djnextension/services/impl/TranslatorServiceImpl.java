/**
 * 
 */
package ar.com.ws.djnextension.services.impl;

import java.util.Date;

import org.springframework.stereotype.Service;

import ar.com.ws.djnextension.services.TranslatorService;

import com.google.api.GoogleAPI;
import com.google.api.translate.Language;
import com.google.api.translate.Translate;

/**
 * @author Mauro Monti
 *
 */
@Service
public class TranslatorServiceImpl implements TranslatorService {

	/**
	 * 
	 */
	private final String referer = "time=" + new Date().getTime(); 
	
	/* (non-Javadoc)
	 * @see ar.com.ws.tools.pm.services.interfaces.TranslatorService#translate(java.lang.String, com.google.api.translate.Language, com.google.api.translate.Language)
	 */
	@Override
	public String translate(String pText, Language pFromLang, Language pToLang) {
 		GoogleAPI.setHttpReferrer(this.referer);
		String result = null;
    	try {
			result = Translate.execute(pText, Language.ENGLISH, Language.SPANISH);
    	} catch (Exception e) { 
    		
    	}
    	return result;
	}

}
