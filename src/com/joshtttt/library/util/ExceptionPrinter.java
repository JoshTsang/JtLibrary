package com.joshtttt.library.util;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Class ExceptionPrinter just contains method <i>getStackTrace(Exception e)</i>
 * for getting a String reporting the stack-trace and description of Exception
 * <i>e</i>.
 */
public class ExceptionPrinter {
	/**
	 * Gets the stack-trace and description of Exception <i>e</i>.
	 * 
	 * @return It returns a String with the stack-trace and description of the
	 *         Exception.
	 */
	public static String getStackTraceOf(Exception e) { // return e.toString();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(err));
		return err.toString();
	}
}
