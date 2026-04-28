package WxLogClient.service;

// -----( IS Java Code Template v1.2

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.wm.util.Debug;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.lang.System;
import com.wm.app.b2b.server.*;
import com.wm.util.Table;
import java.text.*;
import com.wm.lang.ns.*;
import com.wm.app.b2b.util.GenUtil;
// --- <<IS-END-IMPORTS>> ---

public final class util

{
	// ---( internal utility methods )---

	final static util _instance = new util();

	static util _newInstance() { return new util(); }

	static util _cast(Object o) { return (util)o; }

	// ---( server methods )---




	public static final void getCallingServiceName (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(getCallingServiceName)>> ---
		// @subtype unknown
		// @sigtype java 3.5
		// [i] field:0:required emptyVal
		IDataCursor cursor = pipeline.getCursor();
		NSService currentSvc = Service.getServiceEntry();
		Stack callStack = InvokeState.getCurrentState().getCallStack();
		int index = callStack.indexOf(currentSvc);
		if (index > 1)
		{
		  IDataUtil.put(cursor, "callingServiceName", ((NSService) callStack.elementAt(index - 2)).toString());
		}
		cursor.destroy();
		// --- <<IS-END>> ---

                
	}



	public static final void mergeDocuments (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(mergeDocuments)>> ---
		// @sigtype java 3.5
		// [i] record:0:required mdc
		// [i] - field:0:required source.service.name
		// [i] - field:0:required destination.service.name
		// [i] - field:0:required event.type
		// [i] - field:0:required transaction.id
		// [i] - field:0:required url.original
		// [i] - field:0:required http.request.method
		// [i] - field:0:required http.response.status_code
		// [i] record:0:required custom
		// [o] record:0:required mdc
		// [o] - field:0:required source.service.name
		// [o] - field:0:required destination.service.name
		// [o] - field:0:required event.type
		// [o] - field:0:required transaction.id
		// [o] - field:0:required url.original
		// [o] - field:0:required http.request.method
		// [o] - field:0:required http.response.status_code
		IDataCursor pipelineCursor = pipeline.getCursor();
		IData mdc = IDataUtil.getIData(pipelineCursor, "mdc");
		IData custom = IDataUtil.getIData(pipelineCursor, "custom");
		
		if (mdc == null)
		{
			mdc = IDataFactory.create();
		}
		
		if (custom != null)
		{
			IDataCursor customCursor = custom.getCursor();
			IDataCursor mdcCursor = mdc.getCursor();
			while (customCursor.next())
			{
				IDataUtil.put(mdcCursor, customCursor.getKey(), customCursor.getValue());
			}
			customCursor.destroy();
			mdcCursor.destroy();
		}
		
		IDataUtil.put(pipelineCursor, "mdc", mdc);
		pipelineCursor.destroy();
		// --- <<IS-END>> ---

                
	}

	// --- <<IS-START-SHARED>> ---
	static class GenCallable implements Callable<IData>
	{
	    IData pipeline;
	    NSName NSServiceName;
	    Session sess;
	    String serviceIdentifier;
	    long time = -1;
	    
	    public GenCallable(IData pipeline, NSName NSServiceName, Session sess, String serviceIdentifier)
	    {
	        this.pipeline = pipeline;
	        this.NSServiceName = NSServiceName;
	        this.sess = sess;
	        this.serviceIdentifier = serviceIdentifier;
	    }
	    
	    // Keep backward compatibility
	    public GenCallable(IData pipeline, NSName NSServiceName, Session sess)
	    {
	        this(pipeline, NSServiceName, sess, NSServiceName.toString());
	    }
	
	    public IData call() throws Exception {
	        long startTime = System.currentTimeMillis();
	        try 
	        {
	            System.out.println("Starting service call: " + serviceIdentifier);
	            
	            ServiceThread servth = Service.doThreadInvoke(NSServiceName, sess, pipeline, time);
	            IData Output = servth.getIData();
	            
	            long executionTime = System.currentTimeMillis() - startTime;
	            System.out.println("Service " + serviceIdentifier + " completed in " + executionTime + "ms");
	            
	            // Add execution metadata to output
	            if (Output == null)
	            {
	                Output = IDataFactory.create();
	            }
	            IDataCursor cursor = Output.getCursor();
	            IDataUtil.put(cursor, "executionTimeMs", String.valueOf(executionTime));
	            IDataUtil.put(cursor, "serviceNamespace", serviceIdentifier);
	            cursor.destroy();
	            
	            return Output;
	        }
	        catch (Exception e)
	        {
	            long executionTime = System.currentTimeMillis() - startTime;
	            System.out.println("Service " + serviceIdentifier + " failed after " + executionTime + "ms: " + e.getMessage());
	            
	            // Return error information instead of throwing
	            IData errorOutput = IDataFactory.create();
	            IDataCursor cursor = errorOutput.getCursor();
	            IDataUtil.put(cursor, "status", "ERROR");
	            IDataUtil.put(cursor, "errorMessage", e.getMessage());
	            IDataUtil.put(cursor, "serviceNamespace", serviceIdentifier);
	            IDataUtil.put(cursor, "executionTimeMs", String.valueOf(executionTime));
	            cursor.destroy();
	            
	            return errorOutput;
	        }
	    }
	}
	
	private static final Values convert(Hashtable hT)
	{
	  // Following statement gets all arrays in this object.
	  boolean nullFlag = false;
	  Object[] hTArray = hT.values().toArray();
	  Enumeration hTEnumeration = hT.keys();
	  Values outbound = new Values();
	
	  for (int i = 0; i < hTArray.length; i++)
	  {
	    String key = (String) hTEnumeration.nextElement();
	    if (hTArray[i] instanceof java.lang.String)
	    {
	      outbound.put(key, (String) hTArray[i]);
	    }
	    else if (hTArray[i] instanceof java.util.Hashtable)
	    {
	      Values internalObject = convert((Hashtable) hTArray[i]);
	      if (internalObject == null)
	      {
	        nullFlag = true;
	        return null;
	      }
	      outbound.put(key, internalObject);
	    }
	    else
	    {
	      System.out.println("Conversion Failure:" + "unsupported type within inbound Hashtable.");
	      return null;
	    }
	  }
	  return outbound;
	}	
		
	// --- <<IS-END-SHARED>> ---
}

