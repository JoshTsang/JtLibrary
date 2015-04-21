package com.joshtttt.library.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class Log
{
	public static final String LOG_DIR = "/ibaby/";
	
    private static final String TAG = "MyLog";
    private static boolean _lock = false;
    // xueqiang wx13263,20130215,������־����
    private static RotatingLog log = null;
    /** Log level. Only logs with a level less or equal to this are written. */
//    public static int debug_level = Log.INFO;
    public static int debug_level = android.util.Log.VERBOSE;
    /**
     * Path for the log folder where log files are written. By default, it is used the "./log" folder. Use ".", to store
     * logs in the current root folder.
     */
    public static String log_path;
    /** The size limit of the log file [kB] */
    public static int max_logsize = 3072; // 3MB modified for DTS2013122705490
    /**
     * The number of rotations of log files. Use '0' for NO rotation, '1' for rotating a single file
     */
    public static int log_rotations = 2;
    /**
     * The rotation period, in MONTHs or DAYs or HOURs or MINUTEs examples: log_rotation_time=3 MONTHS, log_rotations=90
     * DAYS Default value: log_rotation_time=2 MONTHS
     */
    private static String log_rotation_time = "2 MONTHS";
    /** The rotation time scale */
    public static int rotation_scale = RotatingLog.MONTH;
    /** The rotation time value */
    public static int rotation_time = 2;

    public static boolean isService = false;
    
    private static boolean mLogToFile = false;

    public Log()
    {
        this._lock = false;
    }

    private static synchronized RotatingLog getInstance()
    {
        String filename = log_path;
        if (filename == null) {
        	filename = "/dev/null";
        	android.util.Log.w(TAG, "Log path == null");
		} else {
			if (isService) {
				filename = filename + "service.log";
			} else {
				filename = filename + "app.log";
			}
			
		}
        
		if (log == null) {
			log = new RotatingLog(filename, null, debug_level,
					max_logsize * 1024, log_rotations, rotation_scale,
					rotation_time);
		}
        return log;
    }

    //
    public static void e(String tag, String content)
    {
        android.util.Log.e(tag, String.valueOf(content));
        if(_lock || !mLogToFile)
            return;

        if(log == null)
        {
            log = getInstance();
        }
        log.println(tag, String.valueOf(content), android.util.Log.ERROR);
    }

    //
    public static void v(String tag, String content)
    {
        android.util.Log.v(tag, String.valueOf(content));
        if(_lock || !mLogToFile)
            return;
        if(log == null)
        {
            log = getInstance();
        }
        log.println(tag, String.valueOf(content), android.util.Log.VERBOSE);
    }

    //
    public static void i(String tag, String content)
    {
        android.util.Log.i(tag, String.valueOf(content));
        if(_lock || !mLogToFile)
            return;
        if(log == null)
        {
            log = getInstance();
        }
        log.println(tag, String.valueOf(content), android.util.Log.INFO);
    }

    public static void d(String tag, String content)
    {
        android.util.Log.d(tag, String.valueOf(content));
        if(_lock || !mLogToFile)
            return;
        if(log == null)
        {
            log = getInstance();
        }
        log.println(tag, String.valueOf(content), android.util.Log.DEBUG);
    }

    public static void w(String tag, String content)
    {
        android.util.Log.w(tag, String.valueOf(content));
        if(_lock || !mLogToFile)
            return;
        if(log == null)
        {
            log = getInstance();
        }
        log.println(tag, String.valueOf(content), android.util.Log.WARN);
    }

    public static void a(String tag, String content)
    {
        android.util.Log.i(tag, String.valueOf(content));
        if(_lock || !mLogToFile)
            return;
        if(log == null)
        {
            log = getInstance();
        }
        log.println(tag, String.valueOf(content), android.util.Log.ASSERT);
    }
    
    public static void t(String tag, Exception e) {
        e.printStackTrace();
        if(_lock || !mLogToFile)
            return;
        if(log == null)
        {
            log = getInstance();
        }
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        log.println(tag, result.toString(), android.util.Log.ERROR);
    }
    
    public static void setLogToFile(boolean enable) {
    	mLogToFile = enable; 
    	if (!enable)
    		android.util.Log.i(TAG, "****************Log to File disabled******************");
    }
    
    public static void setPath(String path) {
    	log_path = path;
    }
    
    public static void appLaunched(String verName, String verCode) {
    	 if(_lock)
             return;
         if(log == null)
         {
             log = getInstance();
         }
        
         log.println("APP LAUNCHED", 
        		 String.format("\r\n\r\n%s\r\n%s:%s\r\n%s:%s\r\n%s:%s\r\n%s:%s\r\n%s\r\n\r\n", 
        				 "-----------------------APP LAUNCHED-----------------------",
        				 "version name",
        				 verName,
        				 "version code",
        				 verCode,
        				 "product_model",
        				 android.os.Build.MODEL,
        				 "sdk_ver", 
        				 android.os.Build.VERSION.RELEASE,
        				 "-----------------------APP LAUNCHED-----------------------"),
        				 android.util.Log.ASSERT);
    }
}
