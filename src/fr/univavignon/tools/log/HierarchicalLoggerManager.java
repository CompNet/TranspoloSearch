package fr.univavignon.tools.log;

/*
 * CommonTools
 * Copyright 2010-19 Vincent Labatut
 * 
 * This file is part of CommonTools.
 * 
 * CommonTools is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 2 of the License, or (at your option) any later version.
 * 
 * CommonTools is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with CommonTools. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fr.univavignon.tools.file.FileNames;

/**
 * General manager to handle all
 * the created {@link HierarchicalLogger} objects.
 * 
 * @version 1.3 
 * @author Vincent Labatut
 */
public class HierarchicalLoggerManager
{	
    /////////////////////////////////////////////////////////////////
	// RETRIEVE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of already created hierarchical loggers */
	private static final Map<String,HierarchicalLogger> loggers = new HashMap<String, HierarchicalLogger>();
	
	/**
	 * Fetches or creates a new 
	 * <i>anonymous</i> logger (note only
	 * one such logger can exist at once).
	 * 
	 * @return
	 * 		The anonymous logger.
	 */
	public static synchronized HierarchicalLogger getHierarchicalLogger()
	{	checkFolder();
		HierarchicalLogger result = getHierarchicalLogger(null);
		return result;
	}

	/**
	 * Retrieves an existing hierarchical logger
	 * from its name, or creates a new one
	 * if it does not exist.
	 * 
	 * @param name
	 * 		Name of the requested logger.
	 * @return
	 * 		The retrieved or created logger.
	 */
	public static synchronized HierarchicalLogger getHierarchicalLogger(String name)
	{	checkFolder();
		HierarchicalLogger result = loggers.get(name);
		if(result==null)
		{	result = new HierarchicalLogger(name);
			loggers.put(name,result);
		}
		
		return result;
	}

    /////////////////////////////////////////////////////////////////
	// FOLDER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks if the log folder exists,
	 * otherwise: creates it.
	 */
	private static void checkFolder()
	{	File folder = new File(FileNames.FO_LOG);
		if(!folder.exists())
			folder.mkdir();
	}
	
    /////////////////////////////////////////////////////////////////
	// CLOSE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Closes all existing loggers.
	 */
	public static synchronized void closeLoggers()
	{	for(HierarchicalLogger logger: loggers.values())
			logger.close();
	}
}
