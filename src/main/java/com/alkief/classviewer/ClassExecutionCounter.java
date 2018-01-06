package com.alkief.classviewer;

import com.sun.jdi.ReferenceType;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Field;


// Store counts for the execution of all fields and methods of a given class
public class ClassExecutionCounter {
	Class storedClass;
	Map<String, Integer> methodCounts = new HashMap<String, Integer>();
	Map<String, Integer> fieldCounts = new HashMap<String, Integer>();

	String sourceName = ""; // Used to locate class corresponding to given method / field via breakpoint

	public ClassExecutionCounter (Class c) {
		storedClass = c;

		// Create method map entires
		for (Method m : storedClass.getDeclaredMethods()) {
			methodCounts.put(m.getName(), new Integer(0));
		}

		// Create field map entries
		for (Field f : storedClass.getDeclaredFields()) {
			fieldCounts.put(f.getName(), new Integer(0));
		}
	}

	public void incrementMethodCount (String methodName) {
		Integer prevCount = methodCounts.get(methodName);
		if (prevCount != null)
			methodCounts.put(methodName, prevCount + 1);
	}

	public void incrementFieldCount (String fieldName) {
		Integer prevCount = fieldCounts.get(fieldName);
		if (prevCount != null)
			fieldCounts.put(fieldName, prevCount + 1);
	}

	public String getClassSourceName () {
		return sourceName;
	}

	public int getFieldCount (String fieldName) {
		return fieldCounts.get(fieldName);
	}

	public int getMethodCount (String methodName) {
		return methodCounts.get(methodName);
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName (String s) {
		sourceName = s;
	}

	public Class getStoredClass () {
		return storedClass;
	}
}