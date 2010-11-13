/**
 * 
 */
package ar.com.ws.djn.extension.servlet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ar.com.ws.djn.extension.router.dispatcher.SpringDispatcher;
import ar.com.ws.djn.extension.scanner.SpringScanner;

import com.softwarementors.extjs.djn.StringUtils;
import com.softwarementors.extjs.djn.Timer;
import com.softwarementors.extjs.djn.api.Registry;
import com.softwarementors.extjs.djn.config.ApiConfiguration;
import com.softwarementors.extjs.djn.config.GlobalConfiguration;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectFormPostMethod;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import com.softwarementors.extjs.djn.config.annotations.DirectPollMethod;
import com.softwarementors.extjs.djn.jscodegen.CodeFileGenerator;
import com.softwarementors.extjs.djn.router.RequestRouter;
import com.softwarementors.extjs.djn.router.RequestType;
import com.softwarementors.extjs.djn.router.dispatcher.Dispatcher;
import com.softwarementors.extjs.djn.router.processor.standard.form.upload.UploadFormPostRequestProcessor;
import com.softwarementors.extjs.djn.scanner.Scanner;
import com.softwarementors.extjs.djn.servlet.DirectJNgineServlet;
import com.softwarementors.extjs.djn.servlet.ServletConfigurationException;
import com.softwarementors.extjs.djn.servlet.ServletRegistryConfigurator;
import com.softwarementors.extjs.djn.servlet.ServletUtils;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Mauro Monti
 *
 */
public class SpringDirectJNgineServlet extends DirectJNgineServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2951903491221684382L;

	/**
	 * 
	 */
	private static final Logger logger = Logger.getLogger(SpringDirectJNgineServlet.class);
	
	/**
	 * 
	 */
	private static final String VALUES_SEPARATOR = ",";
	  
	/**
	 * 
	 */
	private ApplicationContext applicationContext;

	/**
	 * 
	 */
	protected static Map<String, RequestRouter> processors = new HashMap<String, RequestRouter>();
	
	/**
	 * 
	 */
	protected static Map<String, ServletFileUpload> uploaders = new HashMap<String, ServletFileUpload>();
	  
	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.servlet.DirectJNgineServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig pServletConfig) throws ServletException {
		// == Get Spring Container Reference.
		this.applicationContext = WebApplicationContextUtils.getWebApplicationContext(pServletConfig.getServletContext());
		
		super.init(pServletConfig);
	}
	
	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.servlet.DirectJNgineServlet#createDirectJNgineRouter(javax.servlet.ServletConfig)
	 */
	@Override
	protected void createDirectJNgineRouter(ServletConfig pServletConfig) throws ServletException {
		assert pServletConfig != null;

	    final Timer subtaskTimer = new Timer();
	    
	    final GlobalConfiguration globalConfiguration = createGlobalConfiguration(pServletConfig);	
	    final String registryConfiguratorClassName = ServletUtils.getParameter(pServletConfig, REGISTRY_CONFIGURATOR_CLASS, null);   
	    
	    if (logger.isInfoEnabled()) {
	    	String value = registryConfiguratorClassName;
		  	if( value == null) {
				value = "";
			}
  			logger.info("Servlet GLOBAL configuration: " + REGISTRY_CONFIGURATOR_CLASS + "=" + value); 
	    }
	    
	    final Class<? extends ServletRegistryConfigurator> registryConfiguratorClass = getRegistryConfiguratorClass(registryConfiguratorClassName);
	    final List<ApiConfiguration> apiConfigurations = this.createApiConfigurationsFromServletConfigurationApi(pServletConfig);
	    
	    subtaskTimer.stop();
	    subtaskTimer.logDebugTimeInMilliseconds("Djn initialization: Servlet Configuration Load time");
	    subtaskTimer.restart();
	    
	    final Registry registry = new Registry(globalConfiguration);
	    
	    final Scanner scanner = new SpringScanner(registry);
	    scanner.scanAndRegisterApiConfigurations(apiConfigurations);
	    
	    subtaskTimer.stop();
	    subtaskTimer.logDebugTimeInMilliseconds("Djn initialization: Standard Api processing time");

	    if (registryConfiguratorClass != null) {
	    	subtaskTimer.restart();
	    	
	    	performCustomRegistryConfiguration(registryConfiguratorClass, registry, pServletConfig);
	    	
	    	subtaskTimer.stop();
	    	subtaskTimer.logDebugTimeInMilliseconds("Djn initialization: Custom Registry processing time");
	    }
	    
	    subtaskTimer.restart();

	    try {
			CodeFileGenerator.updateSource(registry, globalConfiguration.getCreateSourceFiles());      
			
			subtaskTimer.stop();
			subtaskTimer.logDebugTimeInMilliseconds("Djn initialization: Api Files creation time");
	    
	    } catch (IOException ex) {
			ServletException e = new ServletException("Unable to create DirectJNgine API files", ex);
			logger.fatal( e.getMessage(), e );
			throw e;
	    }

	    subtaskTimer.restart();
	    
	    initializeRouter(globalConfiguration, registry);
	    
	    subtaskTimer.stop();
	    subtaskTimer.logDebugTimeInMilliseconds("Djn initialization: Request Processor initialization time");  
	}

	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.servlet.DirectJNgineServlet#createApiConfigurationsFromServletConfigurationApi(javax.servlet.ServletConfig)
	 */
	protected List<ApiConfiguration> createApiConfigurationsFromServletConfigurationApi(ServletConfig configuration) {
		assert configuration != null;
		
		final List<ApiConfiguration> result = new ArrayList<ApiConfiguration>();    
		final String apisParameter = ServletUtils.getRequiredParameter(configuration, GlobalParameters.APIS_PARAMETER);
		final List<String> apis = StringUtils.getNonBlankValues(apisParameter, VALUES_SEPARATOR);
		
		logger.info("Servlet APIs configuration: " + GlobalParameters.APIS_PARAMETER + "=" + apisParameter); 
		
		for (String api : apis) {
			final ApiConfiguration apiConfiguration = createApiConfigurationFromServletConfigurationApi(configuration, api);
			result.add( apiConfiguration );
		}
		
		if (result.isEmpty()) {
			logger.warn( "No apis specified");
		}
		return result;
	}

	/**
	 * @param configuration
	 * @param api
	 * @return
	 */
	protected ApiConfiguration createApiConfigurationFromServletConfigurationApi(ServletConfig configuration, String api) {
	    assert configuration != null;
	    assert !StringUtils.isEmpty(api);
	    
	    final String apiFile = ServletUtils.getParameter(configuration, api + "." + ApiParameters.API_FILE, api + ApiConfiguration.DEFAULT_API_FILE_SUFFIX);
	    final String fullGeneratedApiFile = getServletContext().getRealPath(apiFile);
	    
	    String apiNamespace = ServletUtils.getParameter(configuration, api + "." + ApiParameters.API_NAMESPACE, "");
	    assert apiNamespace != null;
	    
	    String actionsNamespace = ServletUtils.getParameter(configuration, api + "." + ApiParameters.ACTIONS_NAMESPACE, "");
	
		// == If apiNamespace is empty, try to use actionsNamespace.
	    // == If still empty, use the API name itself.
		if (apiNamespace.equals("")) {
			if (actionsNamespace.equals("")) {
				apiNamespace = ApiConfiguration.DEFAULT_NAMESPACE_PREFIX + api;
				
				if (logger.isDebugEnabled()) {
					logger.debug("Using the api name, prefixed with '" + ApiConfiguration.DEFAULT_NAMESPACE_PREFIX + "' as the value for " + ApiParameters.API_NAMESPACE);          
				}
			} else {
				apiNamespace = actionsNamespace;
				logger.debug("Using " + ApiParameters.ACTIONS_NAMESPACE + " as the value for " + ApiParameters.API_NAMESPACE);                  
			}
		}

		// == Register beans annotated with DirectAction Annotation.
		final Map<String, Object> componentAnnotatedClasses = this.applicationContext.getBeansWithAnnotation(Component.class);
		final Map<String, Object> djnActionAnnotatedClasses = this.applicationContext.getBeansWithAnnotation(DirectAction.class);
		for (String component : componentAnnotatedClasses.keySet()) {
			if (hasDjnAnnotations(componentAnnotatedClasses.get(component))) {
				djnActionAnnotatedClasses.put(component, componentAnnotatedClasses.get(component));
			}
		}
		final String[] beansDefinitions = this.applicationContext.getBeanDefinitionNames();
		for (String beanDefinitionName : beansDefinitions) {
			Object object = this.applicationContext.getBean(beanDefinitionName);
			if (hasDjnAnnotations(object)) {
				if (!djnActionAnnotatedClasses.containsKey(beanDefinitionName)) {
					djnActionAnnotatedClasses.put(beanDefinitionName, object.getClass());
				}
			}
		}
		final List<Class<?>> classes = getClasses(djnActionAnnotatedClasses);
		
		if (logger.isInfoEnabled()) {
			logger.info( "Servlet '" + api + "' Api configuration: " +
			ApiParameters.API_NAMESPACE + "=" + apiNamespace + ", " +
			ApiParameters.ACTIONS_NAMESPACE + "=" + actionsNamespace + ", " +
			ApiParameters.API_FILE + "=" + apiFile + " => Full api file: " + fullGeneratedApiFile + ", " +
			ApiParameters.CLASSES + "=" + djnActionAnnotatedClasses);
		}
	
		if( classes.isEmpty() ) {
			logger.warn( "There are no action classes to register for api '" + api + "'");
		}
		
		// == API Configuration.
	    final ApiConfiguration apiConfiguration = new ApiConfiguration(api, apiFile, fullGeneratedApiFile, apiNamespace, actionsNamespace, classes);
	    
	    return apiConfiguration;
	}
	
	/**
	 * @param object
	 * @return
	 */
	private boolean hasDjnAnnotations(Object object) {
		final Method[] methods = object.getClass().getDeclaredMethods();
		for (int idx = 0; idx < methods.length; idx++) {
			final Method method = methods[idx];
			final Annotation[] annotations = method.getAnnotations();
			
			if (annotations == null) {
				continue;
			}
			
			for (Annotation annotation : annotations) {
				if (annotation.annotationType().equals(DirectMethod.class) || 
					annotation.annotationType().equals(DirectFormPostMethod.class) || 
					annotation.annotationType().equals(DirectPollMethod.class)){
					return true;
				}
			}			
		}
		return false;
	}

	/**
	 * @param globalConfiguration
	 * @param registry
	 */
	private void initializeRouter(GlobalConfiguration globalConfiguration, Registry registry) {
		String servletName = getServletName();
	    uploaders.put(servletName, UploadFormPostRequestProcessor.createFileUploader());
	    processors.put(servletName, createRequestRouter(registry, globalConfiguration));
	}
	
	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.servlet.DirectJNgineServlet#createDispatcher(java.lang.Class)
	 */
	@Override
	protected Dispatcher createDispatcher(Class<? extends Dispatcher> pClass) {
		final AutowireCapableBeanFactory factory = this.applicationContext.getAutowireCapableBeanFactory();
		final SpringDispatcher dispatcherBase = factory.createBean(SpringDispatcher.class);
		
		return dispatcherBase;
	}
	
	/**
	 * @param registryConfiguratorClassName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@NonNull 
	private Class<? extends ServletRegistryConfigurator> getRegistryConfiguratorClass(String pRegistryConfiguratorClassName) {
		if (StringUtils.isEmpty(pRegistryConfiguratorClassName)) {
			return null;
		}
		
		Class<? extends ServletRegistryConfigurator> configuratorClass;
		try {
			configuratorClass = (Class<ServletRegistryConfigurator>) Class.forName(pRegistryConfiguratorClassName);
			if (!ServletRegistryConfigurator.class.isAssignableFrom(configuratorClass)) {
				ServletConfigurationException ex = ServletConfigurationException.forRegistryConfiguratorMustImplementGsonBuilderConfiguratorInterface(pRegistryConfiguratorClassName); 
			    logger.fatal( ex.getMessage(), ex );

			    throw ex;
			}
			return configuratorClass;
		
		} catch( ClassNotFoundException ex ) {
			ServletConfigurationException e = ServletConfigurationException.forClassNotFound(pRegistryConfiguratorClassName, ex); 
			logger.fatal( e.getMessage(), e );
			throw e;
		}
	}
	
	/**
	 * @param request
	 * @param response
	 * @param type
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void processRequest(HttpServletRequest request, HttpServletResponse response, RequestType type) throws IOException {
		final String JSON_CONTENT_TYPE 		 = "application/json";
		final String JAVASCRIPT_CONTENT_TYPE = "text/javascript"; // *YES*, shoul be "application/javascript", but then there is IE, and the fact that this is really cross-browser (sigh!)
		final String HTML_CONTENT_TYPE 		 = "text/html";
   
		RequestRouter processor = getProcessor();
		switch (type) {
		
			case FORM_SIMPLE_POST:
				response.setContentType(JSON_CONTENT_TYPE); 
				processor.processSimpleFormPostRequest( request.getReader(), response.getWriter() );
				break;
				
			case FORM_UPLOAD_POST:
				response.setContentType(HTML_CONTENT_TYPE); // MUST be "text/html" for uploads to work!
				processUploadFormPost(request, response);
				break;
				
			case JSON:
				response.setContentType(JSON_CONTENT_TYPE); 
				processor.processJsonRequest( request.getReader(), response.getWriter() );
				break;
				
			case POLL:
				response.setContentType(JSON_CONTENT_TYPE); 
				processor.processPollRequest( request.getReader(), response.getWriter(), request.getPathInfo() );
				break;
				
			case SOURCE:
				response.setContentType(JAVASCRIPT_CONTENT_TYPE); 
				processor.processSourceRequest( request.getReader(), response.getWriter(), request.getPathInfo());
				break;
				
		}
	}
	
	/* (non-Javadoc)
	 * @see com.softwarementors.extjs.djn.servlet.DirectJNgineServlet#getProcessor()
	 */
	protected RequestRouter getProcessor() {
		assert processors.containsKey(getServletName());
		return processors.get(getServletName());
	}
	
	/**
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void processUploadFormPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		assert request != null;
	    assert response != null;
	    
	    RequestRouter router = getProcessor();
	    
	    UploadFormPostRequestProcessor processor = router.createUploadFromProcessor();
	    try {
	    	router.processUploadFormPostRequest(processor, getFileItems(request), response.getWriter());
	    } catch (FileUploadException e) {
	    	processor.handleFileUploadException(e);
	    }
	}
	
	/**
	 * @param request
	 * @return
	 * @throws FileUploadException
	 */
	@SuppressWarnings("unchecked")
	private List<FileItem> getFileItems(HttpServletRequest request) throws FileUploadException {
		assert request != null;
		
		ServletFileUpload uploader = getUploader();
		return uploader.parseRequest(request);
	}

	/**
	 * @param pClasses
	 * @return
	 */
	private List<Class<?>> getClasses(Map<String, Object> pClasses)  {
		assert pClasses != null;
		
		List<Class<?>> result = new ArrayList<Class<?>>();
		if (pClasses.isEmpty()) {
			return result;
		}
		
		for (String beanKey : pClasses.keySet()) {
			Object beanInstance = this.applicationContext.getBean(beanKey);
			result.add(beanInstance.getClass());
		}
		return result;
	}
	
	/**
	 * @param classes
	 * @return
	 */
	@SuppressWarnings("unused")
	private static List<Class<?>> getClasses(String pClasses)  {
		assert pClasses != null;
		
		List<Class<?>> result = new ArrayList<Class<?>>();
		if (StringUtils.isEmpty(pClasses) ) {
			return result;
		}
		
		List<String> classNames = StringUtils.getNonBlankValues(pClasses, VALUES_SEPARATOR);
		for (String className : classNames) {
			try {
				Class<?> cls = Class.forName( className );
				result.add( cls );
			} catch (ClassNotFoundException ex) {
			    logger.fatal(ex.getMessage(), ex);
			    ServletConfigurationException e = ServletConfigurationException.forClassNotFound(className, ex); 
			    throw e;
			}
		}
		
		return result;
	}
	
	/**
	 * @author eM3
	 *
	 */
	@SuppressWarnings("unused")
	public static class GlobalParameters {
		@NonNull public static final String PROVIDERS_URL = "providersUrl";
		@NonNull public static final String DEBUG = "debug";
		
		@NonNull private static final String APIS_PARAMETER = "apis";
		@NonNull private static final String MINIFY = "minify";
		
		@NonNull public static final String BATCH_REQUESTS_MULTITHREADING_ENABLED = "batchRequestsMultithreadingEnabled";
		@NonNull public static final String BATCH_REQUESTS_MIN_THREADS_POOOL_SIZE = "batchRequestsMinThreadsPoolSize";
		@NonNull public static final String BATCH_REQUESTS_MAX_THREADS_POOOL_SIZE = "batchRequestsMaxThreadsPoolSize";
		@NonNull public static final String BATCH_REQUESTS_THREAD_KEEP_ALIVE_SECONDS = "batchRequestsMaxThreadKeepAliveSeconds";
		@NonNull public static final String BATCH_REQUESTS_MAX_THREADS_PER_REQUEST = "batchRequestsMaxThreadsPerRequest";
		@NonNull public static final String GSON_BUILDER_CONFIGURATOR_CLASS = "gsonBuilderConfiguratorClass";
		@NonNull public static final String DISPATCHER_CLASS = "dispatcherClass";
		@NonNull public static final String JSON_REQUEST_PROCESSOR_THREAD_CLASS = "jsonRequestProcessorThreadClass";
		@NonNull public static final String CONTEXT_PATH = "contextPath";
		@NonNull public static final String CREATE_SOURCE_FILES="createSourceFiles";
	}
	
	/**
	 * @return the applicationContext
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
}
