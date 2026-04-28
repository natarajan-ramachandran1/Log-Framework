package wx.log;

// -----( IS Java Code Template v1.2

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import com.wm.app.b2b.server.ServerAPI;
import com.wm.lang.ns.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.wm.app.b2b.server.ServerClassLoader;
// --- <<IS-END-IMPORTS>> ---

public final class services

{
	// ---( internal utility methods )---

	final static services _instance = new services();

	static services _newInstance() { return new services(); }

	static services _cast(Object o) { return (services)o; }

	// ---( server methods )---




	public static final void initialize (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(initialize)>> ---
		// @sigtype java 3.5
		//Configure log4j configuration file
		configure();
		
		// Get instance of configuration factory; your options are default ConfigurationFactory, XMLConfigurationFactory,
		// 	YamlConfigurationFactory & JsonConfigurationFactory
		ConfigurationFactory factory =  XmlConfigurationFactory.getInstance();
		
		// Locate the source of this configuration
		ConfigurationSource configurationSource = null;
		try {
			configurationSource = new ConfigurationSource(new FileInputStream(System.getProperty("watt.wx.log.config")));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			com.wm.util.JournalLogger.log(
					  com.wm.util.JournalLogger.INFO,
		              com.wm.util.JournalLogger.FAC_FLOW_SVC,
		              com.wm.util.JournalLogger.ERROR,
		              "WxLog: " + e1.toString());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			com.wm.util.JournalLogger.log(
					  com.wm.util.JournalLogger.INFO,
		              com.wm.util.JournalLogger.FAC_FLOW_SVC,
		              com.wm.util.JournalLogger.ERROR,
		              "WxLog: " + e1.toString());
		}
		
		// Get context instance
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		
		// Get a reference from configuration
		Configuration configuration = factory.getConfiguration(ctx, configurationSource);
		
		// Start logging system
		ctx.start(configuration);
		
		com.wm.util.JournalLogger.log(
				  com.wm.util.JournalLogger.INFO,
		          com.wm.util.JournalLogger.FAC_FLOW_SVC,
		          com.wm.util.JournalLogger.INFO,
		          "WxLog: Logging context Initialized Successfully");
			
		// --- <<IS-END>> ---

                
	}



	public static final void log (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(log)>> ---
		// @sigtype java 3.5
		// [i] field:0:optional logger
		// [i] field:0:optional id
		// [i] field:0:optional text
		// [i] field:0:optional level {"ERROR","WARN","INFO","DEBUG","TRACE"}
		// [i] record:0:optional mdc
		// [i] - field:0:required service.namespace
		// [i] - field:0:required service.name
		// [i] - field:0:required service.environment
		// [i] - field:0:required transaction.id
		// [i] - field:0:required source.service.name
		// [i] - field:0:required destination.service.name
		// [i] - field:0:required url.original
		// [i] - field:0:required http.request.method
		// [i] - field:0:required error.type
		// [i] - field:0:required error.message
		// [i] - field:0:required error.stack_trace
		// [i] - field:0:required http.response.status_code
		// [o] field:0:required rootContextID
			IDataCursor pipelineCursor = pipeline.getCursor();
			String logger      	= IDataUtil.getString(pipelineCursor, "logger");
			IData mdc 			= IDataUtil.getIData(pipelineCursor,  "mdc");
			String messageID    = IDataUtil.getString(pipelineCursor, "id");
			String level	 	= IDataUtil.getString(pipelineCursor, "level");
			String message      = IDataUtil.getString(pipelineCursor, "text");
		
			if ((messageID == null || messageID.isEmpty()) && 
			    (message == null || message.isEmpty())) {
				com.wm.util.JournalLogger.log( com.wm.util.JournalLogger.INFO,
		                com.wm.util.JournalLogger.FAC_FLOW_SVC,
		                com.wm.util.JournalLogger.ERROR,
		                "wx.log.services:log called without either /id, or /text inputs. One is required." );				
				return;
			}
			
			//pipelineCursor.destroy();
			
			// Clear MDC if this thread was recycled
			conditionallyClearMDC();
			
			IDataUtil.put( pipelineCursor, "rootContextID", lastRootContextID.get().toString() );
			pipelineCursor.destroy();
		
			// Default to INFO if no level was provided
			if (level == null || level.isEmpty()) {
				level = DEFAULT_LEVEL;
			}
		
			// Put *temporary* metadata in MDC
			putMDC(mdc);
			
			// Get message catalog resource bundle from package's resource/ dir
			if (messageID != null) {
				ResourceBundle resources = getResourceBundle();
				try {
					message = resources.getString(messageID + ".text");
				} catch (Exception e) {
					com.wm.util.JournalLogger.log( com.wm.util.JournalLogger.INFO,
			                com.wm.util.JournalLogger.FAC_FLOW_SVC,
			                com.wm.util.JournalLogger.ERROR,
			                "WxLog: " + e.toString());				
			        return;
				}
					try {
			 		level = resources.getString(messageID + ".level");
			 	} catch (Exception e) {
			 		level = DEFAULT_LEVEL;
			 	}
			}
		
			try {
				Level.valueOf(level);
			} catch (IllegalArgumentException e) {
				level = DEFAULT_LEVEL;
			}
			
			// Replace any ${variables} in the message with MDC values (if they exist)
			message = substituteVariables(message);
			
			// Log the message
			log(logger, message, Level.valueOf(level));
			
			// Remove the *temporary* metadata from MDC
			removeMDC(mdc);
	
		// --- <<IS-END>> ---

                
	}



	public static final void mdc (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(mdc)>> ---
		// @sigtype java 3.5
		// [i] record:0:optional put
		// [i] field:1:optional get
		// [i] field:1:optional remove
		// [i] object:0:optional clear
		// [o] record:0:optional entries
			
		IDataCursor pipelineCursor = pipeline.getCursor();
		IData put 		= IDataUtil.getIData(pipelineCursor, "put");
		String[] get 	= IDataUtil.getStringArray(pipelineCursor, "get");
		String[] remove	= IDataUtil.getStringArray(pipelineCursor, "remove");
		Boolean clear	= (Boolean)IDataUtil.get(pipelineCursor, "clear");
		
		// Clear MDC if this thread was recycled
		conditionallyClearMDC();
		
		// Put a list of entries into the MDC
		putMDC(put);
		
		// Return a list of entries from the MDC
		IDataUtil.put(pipelineCursor, "entries", getMDC(get));
		
		// Remove a list of entries from the MDC
		removeMDC(remove);
		
		// Clear the entire MDC
		if (clear != null && clear == true) {
			clearMDC();
		}
		
		pipelineCursor.destroy();
		
		// --- <<IS-END>> ---

                
	}

	// --- <<IS-START-SHARED>> ---
	
	// Whether configure() has been run
	private static boolean configured = false; 
	
	// Stores the root context ID in thread local storage
	private static ThreadLocal<String> lastRootContextID = new ThreadLocal<String>(); 
	
	public enum Level { ERROR, WARN, INFO, DEBUG, TRACE };
	public static final String DEFAULT_LEVEL = "INFO";
	
	private static String substituteVariables(String messageTemplate) {
		Pattern varPattern = Pattern.compile("\\$\\{([a-zA-Z0-9_.$]+)\\}");
		Matcher m = varPattern.matcher(messageTemplate);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			//String mdcKey = m.group();
			String mdcKey = m.group(1);
			String mdcVal = ThreadContext.get(mdcKey);
			if (mdcVal != null && !mdcVal.isEmpty()) {
				m.appendReplacement(sb, Matcher.quoteReplacement(mdcVal));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private static ResourceBundle getResourceBundle() {
		// Get (parent) service that made the log statement
		NSService parent = Service.getCallingService();
		if (parent == null) {
			parent = Service.getServiceEntry();
		}
		
		// Get that package class loader of the calling service
		String parentPackage = parent.getPackage().getName();
		ClassLoader loader = ServerClassLoader.getPackageLoader(parentPackage);		
		
		//System.out.println(parentPackage);
		
		// Load message resource bundle from that package's class loader
		// Will search in the following order:
		// - resources/messages_en_US.properties
		// - resources/messages_en.properties
		// - resources/messages.properties
		ResourceBundle messageBundle = ResourceBundle.getBundle("messages", Locale.getDefault(), loader);
		//ResourceBundle messageBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
		
		//System.out.println(messageBundle.toString());
		
		return messageBundle;
	}
	
	
	/**
	 * [Re-]load configuration
	 */	
	private static void configure() {
		
		try {
	
	      // Read extended setting to determine config file
		  String configFileName = System.getProperty("watt.wx.log.config");
	
		  // If none exists, then let's create a default one
		  if (configFileName == null || configFileName.isEmpty()) {
			  configFileName = "packages\\WxLog\\config\\wxlog_default.xml";
			  System.setProperty("watt.wx.log.config", configFileName);
			  com.wm.util.JournalLogger.log(
					  com.wm.util.JournalLogger.INFO,
		              com.wm.util.JournalLogger.FAC_FLOW_SVC,
		              com.wm.util.JournalLogger.INFO,
					  "WxLog: Using default logger configuration");
		  }  
			  
		  File configFile = new File(configFileName);
		  
		  if (!configFile.exists()) {
			  //log error to server log
			  com.wm.util.JournalLogger.log(
					  com.wm.util.JournalLogger.INFO,
		              com.wm.util.JournalLogger.FAC_FLOW_SVC,
		              com.wm.util.JournalLogger.INFO,
					  "WxLog: Can't find " + configFileName + ", using default");
			  configFileName = "packages\\WxLog\\config\\wxlog_default.xml";
			  System.setProperty("watt.wx.log.config", configFileName);
		  }
		  
		  com.wm.util.JournalLogger.log(
				  com.wm.util.JournalLogger.INFO,
	              com.wm.util.JournalLogger.FAC_FLOW_SVC,
	              com.wm.util.JournalLogger.INFO,
	              "WxLog: Loading logger configuration: "+configFileName);
	
		  // Load the configuration
		  System.setProperty("watt.wx.log.config", configFileName);
		  configured = true;
		   
		} catch (Exception e) {
			com.wm.util.JournalLogger.log(
					  com.wm.util.JournalLogger.INFO,
		              com.wm.util.JournalLogger.FAC_FLOW_SVC,
		              com.wm.util.JournalLogger.ERROR,
		              "WxLog: " + e.toString());
	
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * Conditionally clear the MDC if this thread was recycled in the service
	 * thread pool.
	 */
	public static void conditionallyClearMDC() {
		
		// If root context ID has changed
		String rootContextID = ServerAPI.getAuditContext()[0];
		if (!rootContextID.equals(lastRootContextID.get())) { 
			ThreadContext.clearAll();
		}
		
		// Update thread local storage with the initial, or updated root context ID
		lastRootContextID.set(rootContextID);
		
		// Add implicit metadata to MDC
		addImplicitMetadata();
	}
	
	
	/**
	 * Put a list of entries into the MDC
	 */
	private static void putMDC(IData entries) {
		
		if (entries != null) {
			IDataCursor entriesCursor = entries.getCursor();
			while (entriesCursor.next()) {
				String entryKey = entriesCursor.getKey();
				String entryValue = (String)entriesCursor.getValue();
				if (entryKey != null && entryValue != null) {
					ThreadContext.put(entryKey, entryValue);
				}
			}
		}
	}
	
	
	/**
	 * Return a list of entries from the MDC
	 */
	private static IData getMDC(String[] entries) {
	
		IData gotEntries = IDataFactory.create();
		IDataCursor gotEntriesCursor = gotEntries.getCursor();
		if (entries != null && entries.length > 0) {
			for (String entry : entries) {
				IDataUtil.put(gotEntriesCursor, entry, ThreadContext.get(entry));
			}
		}
		return gotEntries;
	}
	
	
	/**
	 * Remove a list of entries from the MDC 
	 * 
	 * @param entries an array of MDC key names
	 */
	private static void removeMDC(String[] entries) {
		
		if (entries != null && entries.length > 0) {
			for (String entry : entries) {
				ThreadContext.remove(entry);
			}
		}
	}
	
	
	/**
	 * Remove a list of entries from the MDC
	 * 
	 * @param entries an document of MDC entries (only the key name is used to remove)
	 */
	private static void removeMDC(IData entries) {
		
		if (entries != null) {
			IDataCursor entriesCursor = entries.getCursor();
			while (entriesCursor.next()) {
				String entryKey = entriesCursor.getKey();
				ThreadContext.remove(entryKey);
			}
		}
	}
	
	
	/**
	 * Clear the entire MDC
	 */
	private static void clearMDC() {
		ThreadContext.clearAll();
	}
	
	
	/**
	 * Adds implicit metadata to MDC.
	 * 
	 * The following fields are added:
	 *   package
	 *   service
	 *   host
	 *   user
	 *   ssnid
	 * 
	 */
	private static void addImplicitMetadata() {	
	
		// Calling service name
		//
		// Note: this is the parent service that invoked the log service.
		// If there is no parent service, then the log service was 
		// invoked directly (eg. via admin gui or designer), so use
		// the log service itself as the calling service
		NSService parent = Service.getCallingService();
		if (parent == null) {
			parent = Service.getServiceEntry();
		}		
		//ThreadContext.put("service", parent.toString());
	
		// Package name
		//ThreadContext.put("package", parent.getPackage().getName());
		
		// Hostname
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostName();
			ThreadContext.put("host.hostname", host);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
		}
		
		// User
		ThreadContext.put("user.id", Service.getUser().toString());
		
		// Session
		//ThreadContext.put("ssnid", Service.getSession().getSessionID());
	
		// Current and Root Context IDs
		String[] contextIDs = ServerAPI.getAuditContext();
		ThreadContext.put("trace.id", contextIDs[0]); //rootContextID
		// length-2 because we want the currentContextID of the service
		// which is invoking the log service (not the context ID of the 
		// log service invocation itself)
		//ThreadContext.put("currentContextID", contextIDs[contextIDs.length-2]);
		
		long pid = ProcessHandle.current().pid();
		ThreadContext.put("process.pid", String.valueOf(pid));
		
	}
	
	
	/**
	 * Log a log message
	 * 
	 * This is the fundamental logging method. All log() calls lead here.
	 * 
	 * @param logger - if null then it determines the current service name
	 * @param message - the log message to log
	 * @param levelString - ERROR, WARN, INFO, DEBUG, or TRACE
	 * @param inheritMDCFromPreviousInvoke - whether or not to inherit the MDC from a recycled thread.
	 */
	public static void log(String loggerName, String message, Level level) {
		
		// If level is empty then default it to INFO
		if (level == null) {
			level = Level.INFO;
		}
		
		// Get service that made the log statement
		NSService parent = Service.getCallingService();
		if (parent == null) {
			parent = Service.getServiceEntry();
		}
		
		// Get default logger name (based on service name) if none
		// was provided
		if (loggerName == null || loggerName.isEmpty()) {
			// Make dot-delimited logger name (eg. package.folder.subfolder.service)
			loggerName = parent.getPackage().getName() + "." + parent.toString().replace(':', '.');
		}
		
		// Catch-all - if for some reason logger is still null, make it "null"
		// Not sure this will ever happen, but if so, setting it to the string
		// "null" will at least ensure the message gets written to a log file
		// rather than just disappearing silently.
		if (loggerName == null || loggerName.isEmpty()) {
			loggerName = "null";
		}
		
		// Get the logger object for this logger name
		Logger logger = LogManager.getLogger(loggerName);
		
		// Log the message	
		try {
			switch(level) {
				case ERROR: logger.error(message); break;
				case WARN:  logger.warn(message); break;
				case INFO:  logger.info(message); break;
				case DEBUG: logger.debug(message); break;
				case TRACE: logger.trace(message); break;
			}
	
		} catch (Exception e) {	
			com.wm.util.JournalLogger.log(4, 90, 1, "WxLog: Could not log message: " + e.toString());
		}
	}
	
	
	/**
	 * Log method to invoke directly via java code.
	 */
	public static void log(String message) {
		logFromJava(null, message, null);
	}
	
	/**
	 * 
	 */
	public static void log(String logger, String message) {
		logFromJava(logger, message, null);
	}
	
	
	/**
	 * Log method to invoke directly via java code.
	 */
	public static void log(String message, org.apache.logging.log4j.Level level) {
		logFromJava(null, message, level); // Logger name not explicitly specified
	}
	
	
	public static void logFromJava(String logger, String message, org.apache.logging.log4j.Level level) {
		
		NSName logService = NSName.create("wx.log.services:log");
		IData input = IDataFactory.create(new String[][] {
				{ "logger", logger },
				{ "text", message },
				{ "level", (level == null ? DEFAULT_LEVEL : level.toString()) }
		});
		
		try {
			Service.doInvoke(logService, input);
		} catch (Exception e) {
			com.wm.util.JournalLogger.log(
					  com.wm.util.JournalLogger.INFO,
		              com.wm.util.JournalLogger.FAC_FLOW_SVC,
		              com.wm.util.JournalLogger.ERROR,
		              "WxLog: " + e.toString());
		}
	}
		
		
	// --- <<IS-END-SHARED>> ---
}

