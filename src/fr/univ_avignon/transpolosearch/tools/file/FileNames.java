package fr.univ_avignon.transpolosearch.tools.file;

/*
 * TranspoloSearch
 * Copyright 2015 Vincent Labatut
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
 * along with Nerwip - Named Entity Extraction in Wikipedia Pages.  
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;

/**
 * This class contains various constants
 * related to file and folder names.
 *  
 * @author Vincent Labatut
 */
public class FileNames
{	
	/////////////////////////////////////////////////////////////////
	// FOLDERS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Log folder */
	public final static String FO_LOG = "log";
	/** Output folder */
//	public final static String FO_OUTPUT = "out";	//actual folder
//	public final static String FO_OUTPUT = "C:/Users/Vincent/Documents/Dropbox/Nerwip2/out";
//	public final static String FO_OUTPUT = "D:/Users/Vincent/Documents/Dropbox/NetExtraction/Data2";
//	public final static String FO_OUTPUT = "C:/Temp";
	public final static String FO_OUTPUT = "out";
//	public final static String FO_OUTPUT = "/home/vlabatut/Dropbox/Nerwip2/out";
//	public final static String FO_OUTPUT = "/home/vlabatut/Nerwip2/out/Temp";
	/** Resources folder */
	public final static String FO_RESOURCES = "res";
		/** Folder used to store certain cached files */
		public final static String FO_CACHE = FO_RESOURCES + File.separator + "cache";
		/** Folder used to store various data */
		public final static String FO_MISC = FO_RESOURCES + File.separator + "misc";
		/** Folder containing the XML schemas */
		public final static String FO_SCHEMA = FO_RESOURCES + File.separator + "schemas";
	
	/////////////////////////////////////////////////////////////////
	// FILES		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** XML schema file used to store entities  */
	public final static String FI_ENTITY_SCHEMA = "entities.xsd";
	/** XML schema file used to store keys */
	public final static String FI_KEY_SCHEMA = "keys.xsd";
	/** XML schema file used to record article properties  */
	public final static String FI_PROPERTY_SCHEMA = "properties.xsd";
	/** File containing the properties of the article */
	public final static String FI_PROPERTIES = "properties.xml";
	/** File containing original page */
	public final static String FI_ORIGINAL_PAGE = "original.html";
	/** File containing the raw text */
	public final static String FI_RAW_TEXT = "raw" + FileNames.EX_TXT;
	/** File containing the text with hyperlinks */
	public final static String FI_LINKED_TEXT = "linked" + FileNames.EX_TXT;
	/** File containing the entities estimated by a NER tool, in a normalized format */
	public final static String FI_ENTITY_LIST = "entities.xml";
	/** XML schema file used to store keys */
	public final static String FI_KEY_LIST = "keys.xml";
	/** File containing the entities estimated by a NER tool, in a tool-specific format */
	public final static String FI_OUTPUT_TEXT = "output" + FileNames.EX_TXT;
	
	/////////////////////////////////////////////////////////////////
	// EXTENSIONS	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text file extension */
	public final static String EX_TXT = ".txt";
	/** XML file extension */
	public final static String EX_XML = ".xml";
}
