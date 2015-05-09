package fr.univ_avignon.transpolosearch.recognition.internal;

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
 * along with TranspoloSearch. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;

import fr.univ_avignon.transpolosearch.data.article.Article;
import fr.univ_avignon.transpolosearch.data.entity.Entities;
import fr.univ_avignon.transpolosearch.recognition.AbstractConverter;
import fr.univ_avignon.transpolosearch.recognition.ConverterException;
import fr.univ_avignon.transpolosearch.recognition.RecognizerName;
import fr.univ_avignon.transpolosearch.tools.file.FileTools;

/**
 * This class represents a converter for an internal NER tool,
 * i.e. a tool executed programmatically from own system.
 * It is able to convert data from the tool format towards
 * our own objects. 
 * 
 * @param <T>
 * 		Internal representation of the entity list used by the NER tool.
 * 		 
 * @author Vincent Labatut
 */
public abstract class AbstractInternalConverter<T> extends AbstractConverter
{	
	/**
	 * Builds a new internal converter.
	 * 
	 * @param recognizerName
	 * 		Name of the associated NER tool.
	 * @param nerFolder
	 * 		Name of the associated NER tool folder.
	 * @param rawFile
	 * 		Name of the raw file (i.e. external format).
	 */
	public AbstractInternalConverter(RecognizerName recognizerName, String nerFolder, String rawFile)
	{	super(recognizerName,nerFolder,rawFile);
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Convert the specified objects, used internally by the associated NER
	 * tool, into the entity list used internally by our tool.  
	 * 
	 * @param article
	 * 		Original article (might be usefull, in order to get the full text).
	 * @param data
	 * 		Data objects to process.
	 * @return
	 * 		List of entities detected by the associated NER tool.
	 * 
	 * @throws ConverterException
	 * 		Problem while performing the conversion.
	 */
	public abstract Entities convert(Article article, T data) throws ConverterException;

	/////////////////////////////////////////////////////////////////
	// RAW				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Write the raw results obtained for the specified article.
	 * This method is meant for internal tools (those executed
	 * programmatically).
	 * 
	 * @param article
	 * 		Concerned article.
	 * @param results
	 * 		String representation of the NER tool result.		
	 * 
	 * @throws IOException 
	 * 		Problem while recording the file.
	 */
	protected void writeRawResultsStr(Article article, String results) throws IOException
	{	File file = getRawFile(article);
		File folder = file.getParentFile();
		if(!folder.exists())
			folder.mkdirs();
		
		FileTools.writeTextFile(file, results);
	}

	/**
	 * Records the results of the NER task
	 * in a text file, for archiving purposes.
	 * 
	 * @param article
	 * 		Concerned article.
	 * @param intRes
	 * 		Result of the entity detection, represented using the format internal to the NER tool.
	 * 
	 * @throws IOException
	 * 		Problem while writing the file.
	 */
	protected abstract void writeRawResults(Article article, T intRes) throws IOException;
}
