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
public class WebSearchResults extends AbstractSearchResults<WebSearchResult>
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
		
if(url.contains("cookies"))
	System.out.print("");
		
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
	/**
	 * Records all result URLs in a single CSV file.
	 * 
	 * @param fileName
	 * 		Name of the created file.  
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while opening the CSV file.
	 * @throws FileNotFoundException
	 * 		Problem while opening the CSV file.
	 */
	public void exportAsCsv(String fileName) throws UnsupportedEncodingException, FileNotFoundException
	{	logger.log("Recording all the Web search results in a file"+fileName);
		logger.increaseOffset();
		
		// create folder
		File folder = new File(FileNames.FO_WEB_SEARCH_RESULTS);
		folder.mkdirs();
		String cacheFilePath = folder + File.separator + fileName;
		logger.log("Recording in CSV file \""+cacheFilePath+"\"");
		
		// open file and write header
		PrintWriter pw = FileTools.openTextFileWrite(cacheFilePath,"UTF-8");
		{	String line = "\""+COL_TITLE+"\",\""+COL_URL+"\",\""+COL_STATUS+"\",\""+COL_LENGTH+"\"";
			for(String engineName: engineNames)
				line = line + "," + engineName;
			pw.println(line);
		}
		
		// write data and close file
		for(WebSearchResult result: results.values())
		{	// title
			String line = "\"";
			String title = null;
			if(result.article!=null)
				title = result.article.getTitle();
			if(title!=null)
				line = line + title;
			// url
			line = line + "\",\"" + result.url + "\",";
			// status
			if(result.status!=null)
				line = line + result.status;
			line = line + ",";
			// length
			Integer length = null;
			if(result.article!=null)
				length = result.article.getRawText().length();
			if(length!=null)
				line = line + length;
			// ranks
			Map<String,String> ranks = result.ranks;
			for(String engineName: engineNames)
			{	line = line + ",";
				String rank = ranks.get(engineName);
				if(rank!=null)
					line = line + "\"" + rank + "\"";
			}
			pw.println(line);
		}
		pw.close();

		logger.decreaseOffset();
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportEvents(boolean bySentence) throws UnsupportedEncodingException, FileNotFoundException
	{	String fileName;
		if(bySentence)
			fileName = FileNames.FI_EVENT_TABLE_SENTENCE;
		else
			fileName = FileNames.FI_EVENT_TABLE_ARTICLE;
		String filePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + fileName;
		logger.log("Recording the events as a CVS file: "+filePath);
		logger.decreaseOffset();
			PrintWriter pw = FileTools.openTextFileWrite(filePath, "UTF-8");
			
			// write header
			List<String> startCols = Arrays.asList(COL_COMMENTS, COL_TITLE, COL_URL, COL_LENGTH, 
					COL_PUB_DATE, COL_AUTHORS);
			List<String> endCols = Arrays.asList(COL_STATUS, COL_EVENT_RANK, COL_EVENT_DATES,
					COL_EVENT_LOCATIONS, COL_EVENT_PERSONS, COL_EVENT_ORGANIZATIONS, COL_EVENT_FUNCTIONS,
					COL_EVENT_PRODUCTIONS, COL_EVENT_MEETINGS
			);
			List<String> cols = new ArrayList<String>();
			cols.addAll(startCols);
			for(String engineName: engineNames)
				cols.add(COL_RANK+engineName); 
			cols.addAll(endCols);
			Iterator<String> it = cols.iterator();
			while(it.hasNext())
			{	String col = it.next();
				pw.print(col);
				if(it.hasNext())
					pw.print(",");
			}
			pw.println();
			
			// write data
			logger.log("Treat each article separately");
			int total = 0;
			for(WebSearchResult result: results.values())
			{	List<Map<String,String>> lines = result.exportEvents();
				for(Map<String,String> line: lines)
				{	it = cols.iterator();
					while(it.hasNext())
					{	String col = it.next();
						String val = line.get(col);
						if(val!=null)
							pw.print(val);
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
}
