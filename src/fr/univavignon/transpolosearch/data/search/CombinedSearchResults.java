package fr.univavignon.transpolosearch.data.search;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;

/**
 * Collection of search results returned by a collection of Web
 * and social media search engines, with additional info resulting from their
 * subsequent processing.
 * 
 * @author Vincent Labatut
 */
public class CombinedSearchResults extends AbstractSearchResults<AbstractSearchResult>
{	
	/**
	 * Combines existing web and social search results in a single object.
	 * 
	 * @param webRes
	 * 		Web search results.
	 * @param socRes
	 * 		Social search results.
	 */
	public CombinedSearchResults(WebSearchResults webRes, SocialSearchResults socRes)
	{	super();
		
		// add all results
		results.putAll(webRes.results);
		results.putAll(socRes.results);
		// and search engines
		engineNames.addAll(webRes.engineNames);
		engineNames.addAll(socRes.engineNames);
		// and references
		referenceEvents.putAll(webRes.referenceEvents);
		referenceClusters.putAll(webRes.referenceClusters);
		referenceClusters.putAll(socRes.referenceClusters);
	}
	
	/////////////////////////////////////////////////////////////////
	// CSV			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportResults(String fileName) throws UnsupportedEncodingException, FileNotFoundException
	{	logger.log("Recording all the combined search results in file "+fileName);
		logger.increaseOffset();
		
		// setup filename
		String filePath = FileNames.FO_OUTPUT + File.separator + fileName;
		logger.log("Recording in CSV file \""+filePath+"\"");
		
		// setup colon names
		List<String> cols = Arrays.asList(
				COL_NOTES, COL_TITLE_CONTENT, COL_URL_ID, COL_LENGTH, COL_PUB_DATE, 
				COL_AUTHORS, COL_STATUS, COL_ARTICLE_CLUSTER, COL_SOURCE, 
				COL_ENT_DATES, COL_ENT_LOCATIONS, COL_ENT_PERSONS, COL_ENT_ORGANIZATIONS, 
				COL_ENT_FUNCTIONS, COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
		);
		
		// open file and write header
		PrintWriter pw = FileTools.openTextFileWrite(filePath,"UTF-8");
		{	Iterator<String> it = cols.iterator();
			while(it.hasNext())
			{	String col = it.next();
				pw.print("\""+col+"\"");
				if(it.hasNext())
					pw.print(",");
			}
		}
		pw.println();
		
		// write data and close file
		for(AbstractSearchResult result: results.values())
		{	Map<String,String> map = result.exportResult();
			Iterator<String> it = cols.iterator();
			while(it.hasNext())
			{	String col = it.next();
				String val = map.get(col);
				if(val!=null)
					pw.print("\""+val+"\"");
				if(it.hasNext())
					pw.print(",");
			}
			pw.println();
		}
		pw.close();
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportEvents(boolean bySentence, String filePrefix) throws UnsupportedEncodingException, FileNotFoundException 
	{	String fileName = filePrefix;
		if(bySentence)
			fileName = fileName + FileNames.FI_EVENT_LIST_BYSENTENCE;
		else
			fileName = fileName + FileNames.FI_EVENT_LIST_BYARTICLE;
		String filePath = FileNames.FO_OUTPUT + File.separator + fileName;
		logger.log("Recording the event list as a CVS file: "+filePath);
		logger.decreaseOffset();
			
			// setup colon names
			List<String> startCols = Arrays.asList(
					COL_NOTES, COL_EVENT_CLUSTER, COL_TITLE_CONTENT, COL_URL_ID, COL_LENGTH, 
					COL_PUB_DATE, COL_AUTHORS, COL_STATUS, COL_ARTICLE_CLUSTER, COL_SOURCE
			);
			List<String> endCols = Arrays.asList(
					COL_ENT_DATES, COL_ENT_LOCATIONS, COL_ENT_PERSONS, COL_ENT_ORGANIZATIONS, 
					COL_ENT_FUNCTIONS, COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
			);
			List<String> cols = new ArrayList<String>();
			cols.addAll(startCols);
			if(bySentence)
			{	cols.add(COL_EVENT_RANK);
				cols.add(COL_EVENT_SENTENCE);
			}
			cols.addAll(endCols);
			
			// open file and write header
			PrintWriter pw = FileTools.openTextFileWrite(filePath, "UTF-8");
			Iterator<String> it = cols.iterator();
			while(it.hasNext())
			{	String col = it.next();
				pw.print("\""+col+"\"");
				if(it.hasNext())
					pw.print(",");
			}
			pw.println();
			
			// write data
			int total = 0;
			logger.log("Treat each article separately");
			for(AbstractSearchResult result: results.values())
			{	List<Map<String,String>> lines = result.exportEvents();
				for(Map<String,String> line: lines)
				{	it = cols.iterator();
					while(it.hasNext())
					{	String col = it.next();
						String val = line.get(col);
						if(val!=null)
							pw.print("\""+val+"\"");
						if(it.hasNext())
							pw.print(",");
					}
					pw.println();
					total++;
				}
			}
			
			pw.close();
		logger.decreaseOffset();
		logger.log("Wrote "+total+" events");
	}

	@Override
	public void exportEventClusters(boolean bySentence, String filePrefix, ArticleLanguage language) throws UnsupportedEncodingException, FileNotFoundException 
	{	String fileName = filePrefix;
		if(bySentence)
			fileName = fileName + FileNames.FI_EVENT_CLUSTERS_BYSENTENCE;
		else
			fileName = fileName + FileNames.FI_EVENT_CLUSTERS_BYARTICLE;
		String filePath = FileNames.FO_OUTPUT + File.separator + fileName;
		
		exportEventClusters(filePath, language);
	}
}
