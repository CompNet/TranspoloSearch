package fr.univavignon.transpolosearch.data.search;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import fr.univavignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;

/**
 * Collection of search results returned by a collection of Web
 * search engines, with additional info resulting from their
 * subsequent processing.
 * 
 * @author Vincent Labatut
 */
public class WebSearchResults extends AbstractSpecificSearchResults<WebSearchResult>
{	
	/////////////////////////////////////////////////////////////////
	// RESULTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Adds the specified url to the list of results, as returned at the specified
	 * rank by the specified search engine. If a similar result already exists, it
	 * is simply completed.
	 * 
	 * @param url
	 * 		Result URL.
	 * @param engineName
	 * 		Engine returning the URL.
	 * @param rank
	 * 		Rank of the URL according to the search engine.
	 * @return
	 * 		The created/completed search result object.
	 */
	public WebSearchResult addResult(String url, String engineName, String rank)
	{	String cleanUrl = cleanUrl(url);
		
		WebSearchResult result = results.get(cleanUrl);
		if(result==null)
		{	result = new WebSearchResult(cleanUrl);
			results.put(cleanUrl, result);
		}
		result.addEngine(engineName, rank);
		engineNames.add(engineName);
		return result;
	}
	
	/**
	 * Certain URL returned by certain search engines cannot be processed directly,
	 * but must be corrected first. This methods performs this transformation when
	 * required, and returns the fixed URL.
	 * 
	 * @param url
	 * 		Original URL.
	 * @return
	 * 		Corrected URL.
	 */
	private String cleanUrl(String url)
	{	logger.increaseOffset();
		String result = url;
		
		String sep = "check_cookies?url=%2F";
		int idx = result.indexOf(sep);
		if(idx!=-1)
		{	result = result.substring(0,idx) + result.substring(idx+sep.length());
			result = result.replace("%2F", "/");
			logger.log("Corrected URL: "+result);
		}
		
		logger.decreaseOffset();
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// FILTERING	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Discards the results corresponding to URLs which cannot
	 * be processed.
	 */
	public void filterByUrl()
	{	logger.log("Filtering the retrieved URL to remove those we can't treat");
		logger.increaseOffset();
		
		int count = 0;
		for(WebSearchResult result: results.values())
		{	if(!result.filterUrl())
				count++;
		}
		
		logger.decreaseOffset();
		logger.log("URL filtering complete: "+count+" pages kept");
	}
	
	/////////////////////////////////////////////////////////////////
	// RETRIEVAL	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Retrieve all the articles whose URLs were not previously filtered.
	 * 
	 * @throws IOException
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException
	 * 		Problem while retrieving a Web page.
	 * @throws SAXException
	 * 		Problem while retrieving a Web page.
	 */
	public void retrieveArticles() throws IOException, ParseException, SAXException
	{	logger.log("Starting the article retrieval");
		logger.increaseOffset();
			
			// init
			ArticleRetriever articleRetriever = new ArticleRetriever(true); //TODO cache disabled for debugging
//			articleRetriever.setLanguage(ArticleLanguage.FR);	// we don't need that anymore, since we can now determine the language automatically
			
			int count = 0;
			int total = 0;
			for(WebSearchResult result: results.values())
			{	if(result.status==null)
				{	total++;
					if(result.retrieveArticle(articleRetriever,total))
						count++;
				}
			}
		
		logger.decreaseOffset();
		logger.log("Article retrieval complete: "+count+"/"+total);
	}
	
	/////////////////////////////////////////////////////////////////
	// CSV			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportResults(String fileName) throws UnsupportedEncodingException, FileNotFoundException
	{	logger.log("Recording all the Web search results in file "+fileName);
		logger.increaseOffset();
		
		// create folder
		File folder = new File(FileNames.FO_WEB_SEARCH_RESULTS);
		folder.mkdirs();
		String filePath = folder + File.separator + fileName;
		logger.log("Recording in CSV file \""+filePath+"\"");
		
		// setup colon names
		List<String> startCols = Arrays.asList(
				COL_NOTES, COL_TITLE, COL_URL, COL_LENGTH, 
				COL_PUB_DATE, COL_AUTHORS, COL_STATUS, COL_ARTICLE_CLUSTER
		);
		List<String> endCols = Arrays.asList(
				COL_ENT_DATES, COL_ENT_LOCATIONS, COL_ENT_PERSONS, 
				COL_ENT_ORGANIZATIONS, COL_ENT_FUNCTIONS, COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
		);
		List<String> cols = new ArrayList<String>();
		cols.addAll(startCols);
		for(String engineName: engineNames)
			cols.add(COL_RANK+engineName); 
		cols.addAll(endCols);
		
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
		for(WebSearchResult result: results.values())
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

		logger.decreaseOffset();
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
		String filePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + fileName;
		logger.log("Recording the event list as a CVS file: "+filePath);
		logger.decreaseOffset();
			
			// setup colon names
			List<String> startCols = Arrays.asList(
					COL_NOTES, COL_EVENT_CLUSTER, COL_TITLE, COL_URL, COL_LENGTH, COL_PUB_DATE, COL_AUTHORS,
					COL_STATUS, COL_ARTICLE_CLUSTER
			);
			List<String> endCols = Arrays.asList(
					COL_ENT_DATES, COL_ENT_LOCATIONS, 
					COL_ENT_PERSONS, COL_ENT_ORGANIZATIONS, COL_ENT_FUNCTIONS,
					COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
			);
			List<String> cols = new ArrayList<String>();
			cols.addAll(startCols);
			if(bySentence)
				cols.add(COL_EVENT_RANK);
			for(String engineName: engineNames)
				cols.add(COL_RANK+engineName); 
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
			for(WebSearchResult result: results.values())
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
	public void exportEventClusters(boolean bySentence, String filePrefix) throws UnsupportedEncodingException, FileNotFoundException
	{	String fileName = filePrefix;
		if(bySentence)
			fileName = fileName + FileNames.FI_EVENT_CLUSTERS_BYSENTENCE;
		else
			fileName = fileName + FileNames.FI_EVENT_CLUSTERS_BYARTICLE;
		String filePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + fileName;
		logger.log("Recording the event clusters as a CVS file: "+filePath);
		logger.decreaseOffset();
			
			// setup colon names
			List<String> startCols = Arrays.asList(
					COL_NOTES, COL_TITLE, COL_URL, COL_LENGTH, COL_PUB_DATE, COL_AUTHORS,
					COL_STATUS, COL_ARTICLE_CLUSTER, COL_EVENT_RANK
			);
			List<String> endCols = Arrays.asList(
					COL_EVENT_CLUSTER, COL_ENT_DATES, COL_ENT_LOCATIONS, 
					COL_ENT_PERSONS, COL_ENT_ORGANIZATIONS, COL_ENT_FUNCTIONS,
					COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
			);
			List<String> cols = new ArrayList<String>();
			cols.addAll(startCols);
			for(String engineName: engineNames)
				cols.add(COL_RANK+engineName); 
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
			logger.log("Treat each cluster separately");
			for(int i=0;i<mapClustRes.size();i++)
			{	List<WebSearchResult> res = mapClustRes.get(i);
				List<Integer> evt = mapClustEvt.get(i);
				for(int j=0;j<res.size();j++)
				{	// setup the line
					WebSearchResult r = res.get(j);
					List<Map<String,String>> lines = r.exportEvents();
					int idx = evt.get(j);
					Map<String,String> line = lines.get(idx);
					line.put(COL_EVENT_CLUSTER, Integer.toString(i+1));
					// write the line
					it = cols.iterator();
					while(it.hasNext())
					{	String col = it.next();
						String val = line.get(col);
						if(val!=null)
							pw.print("\""+val+"\"");
						if(it.hasNext())
							pw.print(",");
					}
					pw.println();
				}
			total = i;
			}
			
			pw.close();
		logger.decreaseOffset();
		logger.log("Wrote "+total+" event clusters");
	}
}
