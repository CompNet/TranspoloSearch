package fr.univavignon.transpolosearch.recognition;

/*
 * TranspoloSearch
 * Copyright 2015-17 Vincent Labatut
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.xml.sax.SAXException;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.entity.Entities;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;

/**
 * Class used to convert the output of NER tool to
 * some format compatible with our own system.
 * <br/>
 * It also allows to read/write our own XML based
 * format.
 * 
 * @author Yasa Akbulut
 * @author Vincent Labatut
 */
public abstract class AbstractConverter
{	
	/**
	 * Builds a new converter.
	 * 
	 * @param recognizerName
	 * 		Name of the associated NER tool.
	 * @param nerFolder
	 * 		Name of the associated NER tool folder.
	 * @param rawFile
	 * 		Name of the raw file (i.e. external format).
	 */
	public AbstractConverter(RecognizerName recognizerName, String nerFolder, String rawFile)
	{	this.recognizerName = recognizerName;
		this.nerFolder = nerFolder;
		this.rawFile = rawFile;
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGING			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	protected static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();

	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**  Name of the associated NER tool */
	protected RecognizerName recognizerName = null;
	
	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of the folder used to store results */
	protected String nerFolder = null;
	
	/////////////////////////////////////////////////////////////////
	// FILES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of the file possibly generated by the NER tool */
	protected String rawFile = null;
	
	/**
	 * Returns the XML file associated to the specified
	 * article.
	 * 
	 * @param article
	 * 		Article to process.
	 * @return
	 * 		A {@code File} object representing the associated XML result file.
	 */
	public File getXmlFile(Article article)
	{	String resultsFolder = article.getFolderPath();
		if(nerFolder!=null)
			resultsFolder = resultsFolder + File.separator + nerFolder;
		String filePath = resultsFolder + File.separator + FileNames.FI_ENTITY_LIST;
		
		File result = new File(filePath);
		return result;
	}
	
	/**
	 * Returns the raw file associated to the specified
	 * article, i.e. the file possibly generated externally
	 * by the NER tool associated to this converter.
	 * 
	 * @param article
	 * 		Article to process.
	 * @return
	 * 		A {@code File} object representing the associated raw result file.
	 */
	public File getRawFile(Article article)
	{	String resultsFolder = article.getFolderPath();
		if(nerFolder!=null)
			resultsFolder = resultsFolder + File.separator + nerFolder;
		String filePath = resultsFolder + File.separator + rawFile;
	
		File result = new File(filePath);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// XML				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Write the XML results obtained for the specified article.
	 * This method is meant for both internal and external tools.
	 * 
	 * @param article
	 * 		Concerned article.
	 * @param entities
	 * 		List of the detected entities.
	 * @throws IOException
	 * 		Problem while writing the file.
	 */
	public void writeXmlResults(Article article, Entities entities) throws IOException
	{	// data file
		File file = getXmlFile(article);
		
		// check folder
		File folder = file.getParentFile();
		if(!folder.exists())
			folder.mkdirs();
		
		entities.writeToXml(file);
	}
	
	/**
	 * Read the XML representation of the results
	 * previously processed by the associated NER
	 * tool, for the specified article.
	 * 
	 * @param article
	 * 		Article to process.
	 * @return
	 * 		The list of entities stored in the file.
	 * 
	 * @throws SAXException
	 * 		Problem while reading the file.
	 * @throws IOException
	 * 		Problem while reading the file.
	 * @throws ParseException 
	 * 		Problem while parsing a date. 
	 */
	public Entities readXmlResults(Article article) throws SAXException, IOException, ParseException
	{	File dataFile = getXmlFile(article);
		
		Entities result = Entities.readFromXml(dataFile);
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// RAW				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tries to delete the file containing the raw results.
	 * Returns a boolean indicating success ({@code true})
	 * or failure ({@code false}).
	 * 
	 * @param article
	 * 		Concerned article.
	 * @return
	 * 		{@code true} iff the file could be deleted.
	 */
	public boolean deleteRawFile(Article article)
	{	boolean result = false;
		File rawFile = getRawFile(article);
		if(rawFile!=null && rawFile.exists() && rawFile.isFile())
			result = rawFile.delete();
		return result;
	}
}
