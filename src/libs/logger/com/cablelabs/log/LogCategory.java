package com.cablelabs.log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Hashtable;

/**
 * The base class for application log categories.
 * <p>
 *  Subclasses are required to have these two constructors: <br/>
 *  Subclass() // This will construct the default Application category and MUST only be called from {@link LogCategory#updateAppCategory}<br/>
 *  Subclass(String name) // This will construct any other categories. <br/>
 *  Theses will be used to create the default log categories automatically. <br/> 
 *  All categories are expected to be registered before use. To do this subclasses MUST do one of two things
 *  <ol>
 *  <li> Constructors can call super() or super(name) as appropriate.
 *  </li>
 *  <li> call {@link LogCategory#addToCategoryMap(LogCategory)} with the category once before logging begins.
 *  </li>
 *  </ol>
 *  </p><p>
 *   Any other needed categories can be created as needed by the application but are expected to be sent to {@link LogCategory#addToCategoryMap(LogCategory)}.
 *   <br/>
 *  It is recommended to store these instances as public static final on the subclass for easy access throughout the application. 
 *  </p>
 * 
 * @author rvail
 *
 */
public class LogCategory {

	public static LogCategory ALL;
	public static LogCategory APPLICATION;
	public static LogCategory LOG_MSG;

	private static Hashtable<String, LogCategory> categories;

	static {
		categories = new Hashtable<String, LogCategory>();
	}

	public final String name;
	
	public LogCategory() {
		this.name = getApplicationName();
		addToCategoryMap(this);
	}

	public LogCategory(String name) {
		this.name = name;
		addToCategoryMap(this);
	}

	public String getApplicationName() {
		return "APPLICATION";
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Registers a category for use with the LogAPI.
	 * @param cat the category to register
	 * @throws IllegalArgumentException if the given categories name is already registered.
	 */
	protected static void addToCategoryMap(LogCategory cat) {
		LogCategory existing = categories.get(cat.name);
		if (existing != null) {
			throw new IllegalArgumentException(cat + " is already a known LogCategory.");
		}
		categories.put(cat.name, cat);
	}

	public static LogCategory getCategory(String name) {
		return categories.get(name);
	}

	/**
	 * Creates the default categories with instances of the given class.
	 * 
	 * @param categoryClass
	 */
    public static void updateAppCategories(Class<? extends LogCategory> categoryClass) {
        try {
            APPLICATION = categoryClass.newInstance();
        }
        catch (InstantiationException e) {
            System.err.println("Error creating default application category. Ensure that class " + categoryClass + " has a public nullary/default Constructor.\n"
                    + e.getLocalizedMessage());
            e.printStackTrace(System.err);
        }
        catch (Exception e) {
            System.err.println("Error creating default application category. For class " + categoryClass + "\n"
                    + e.getLocalizedMessage());
            e.printStackTrace(System.err);
        }
        
        
        try {
            Constructor<? extends LogCategory> con = categoryClass.getConstructor(String.class);
            ALL = con.newInstance("ALL");
            LOG_MSG = con.newInstance("LogMsg");
        }
        catch (NoSuchMethodException e) {
            System.err.println("Error creating default LogCategories. Ensure class: " + categoryClass 
                    + " has a public " + categoryClass.getSimpleName() + "(String name)\n" 
                    + e.getLocalizedMessage());
            e.printStackTrace(System.err);
        }
        catch (Exception e) {
            System.err.println("Error creating default LogCategories with class: " + categoryClass + "\n" 
                    + e.getLocalizedMessage());
            e.printStackTrace(System.err);
        }
        
        try {
            Method m = categoryClass.getMethod("updateAppCategoriesFinished");
            m.invoke(null);  
        }
        catch (NoSuchMethodException e) {
            // method is probably not needed so no need to log
        }
        catch (Exception e) {
           System.err.println("Error invoking " + categoryClass.getSimpleName() + "updateAppCategoriesFinished(): " + e.getLocalizedMessage());
        }
     
    }

}
