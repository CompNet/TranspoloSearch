package fr.univavignon.transpolosearch.processing.internal.modelless.subee;

/*
 * TranspoloSearch
 * Copyright 2015-18 Vincent Labatut
 * 
 * This file is part of TranspoloSearch.
 * 
 * TranspoloSearch is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 2 of the License, or (at your option) any later version.
 * 
 * TranspoloSearch is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TranspoloSearch. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.client.ClientProtocolException;

import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.tools.freebase.FbTypeTools;

import fr.univavignon.tools.log.HierarchicalLogger;
import fr.univavignon.tools.log.HierarchicalLoggerManager;

/**
 * This class contains methods used for the maintenance of
 * the maps and list used by our tool Subee. 
 * 
 * @author Vincent Labatut
 */
public class SubeeTools
{	
	/**
	 * Launches the maintenance of the Subee files.
	 * 
	 * @param args
	 * 		None needed.
	 * 
	 * @throws Exception 
	 * 		Problem while loading/retrieving the FB types.
	 */
	public static void main(String[] args) throws Exception
	{	// maintenance
//		updateUnknownTypes();
	
		// tests
		testTypeConverter();
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGING			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	protected static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();

	/////////////////////////////////////////////////////////////////
	// TESTS				/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests the feature allowing to automatically
	 * convert a FB type to a {@link EntityType} value.
	 * 
	 * @throws Exception
	 * 		Something went wrong...
	 */
	private static void testTypeConverter() throws Exception
	{	logger.setName("Test-TypeConverter");
		logger.log("Start converting types");
		logger.increaseOffset();
		
		String title = "World_War_II";
		
		// retrieve all types
		List<String> types = FbTypeTools.getAllTypes(title);
		logger.log("Types retrieved for "+title+":");
		logger.increaseOffset();
		logger.log(types);
		logger.decreaseOffset();
		
		// retrieve only notable type
//		String type = FbTypeTools.getNotableType(title);
//		List<String> types = new ArrayList<String>();
//		types.add(type);
//		logger.log("Notable type for "+title+": "+type);
		
		// build Subee and loads the necessary files
		logger.log("Load the Subee files");
		SubeeDelegateRecognizer DelegateRecognizer = new SubeeDelegateRecognizer(null,true, true, true, true, true);
		DelegateRecognizer.prepareRecognizer();
		
		// convert types
		EntityType type = DelegateRecognizer.retrieveMentionType(types);
		logger.log("Corresponding entity type: "+type);
		
		logger.log("Type conversion complete");
		logger.decreaseOffset();
	}

	/////////////////////////////////////////////////////////////////
	// TYPE MAPS	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Updates the file containing unknown types so that it contains
	 * all types retrieved from Freebase not already present in the
	 * existing type files.
	 * 
	 * @throws ProcessorException 
	 * 		Problem while loading/retrieving the FB types.
	 * @throws org.json.simple.parser.ParseException 
	 * 		Problem while loading/retrieving the FB types.
	 * @throws IOException 
	 * 		Problem while loading/retrieving the FB types.
	 * @throws ClientProtocolException 
	 * 		Problem while loading/retrieving the FB types.
	 */
	public static void updateUnknownTypes() throws ProcessorException, ClientProtocolException, IOException, org.json.simple.parser.ParseException
	{	logger.setName("Updating-Subee-List");
		logger.log("Updating Subee unknown FB types list");
		
		// build Subee and loads the necessary files
		logger.log("Load the Subee files");
		SubeeDelegateRecognizer subeeDelegateRecognizer = new SubeeDelegateRecognizer(null, true, true, true, true, true);
		subeeDelegateRecognizer.prepareRecognizer();
		
		// retrieve the loaded lists
		logger.log("Get the existing type lists from these files");
		Set<String> knownTypes = new TreeSet<String>();
		knownTypes.addAll(SubeeDelegateRecognizer.TYPE_MAP.keySet());
		knownTypes.addAll(SubeeDelegateRecognizer.UNKNOWN_TYPES);
		
		// retrieve the last types from Freebase
		logger.log("Get the last type from Freebase");
		Set<String> fbTypes = FbTypeTools.retrieveDomainTypes();
		
		// retain only the unknown ones
		logger.log("Udpdate the 'unknown' file");
		fbTypes.removeAll(knownTypes);
		
		// append them to the 'unknown' file
		subeeDelegateRecognizer.updateUnknownTypes("-NOT A TYPE^^---------------------");
		for(String type: fbTypes)
			subeeDelegateRecognizer.updateUnknownTypes(type);
	}
}
