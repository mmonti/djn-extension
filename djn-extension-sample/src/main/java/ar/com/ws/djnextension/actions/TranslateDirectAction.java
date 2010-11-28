/**
 * 
 */
package ar.com.ws.djnextension.actions;

import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.springframework.beans.factory.annotation.Autowired;

import ar.com.ws.djn.extension.annotation.SpringDirectAction;
import ar.com.ws.djnextension.response.Response;
import ar.com.ws.djnextension.services.TranslatorService;

import com.google.api.translate.Language;
import com.softwarementors.extjs.djn.config.annotations.DirectFormPostMethod;

/**
 * @author Mauro Monti
 *
 */
@SpringDirectAction
public class TranslateDirectAction {

	/**
	 * 
	 */
	private static final String TRANSLATE_TEXT = "translateText";
	
	/**
	 * 
	 */
	@Autowired 
	private TranslatorService translatorService;

	/**
	 * @param pParameters
	 * @param pFileInputParameters
	 * @return
	 */
	@DirectFormPostMethod 
	public Response translate(Map<String, String> pParameters, Map<String, FileItem> pFileInputParameters) {
		final String queryParam = pParameters.get(TranslateDirectAction.TRANSLATE_TEXT);
		final Response response = new Response(translatorService.translate(queryParam, Language.ENGLISH, Language.SPANISH));
		
		return response;
	}

}