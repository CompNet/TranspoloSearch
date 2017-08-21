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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;

/**
 * Collection of search results returned by a collection of social
 * search engines, with additional info resulting from their
 * subsequent processing.
 * 
 * @author Vincent Labatut
 */
public class SocialSearchResults extends AbstractSearchResults<SocialSearchResult>
{
	/////////////////////////////////////////////////////////////////
	// RESULTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Adds the specified posts to this list of results.
	 * 
	 * @param post
	 * 		The social media posts to add to this collection of results.
	 */
	public void addResult(SocialSearchResult post)
	{	String resultId = post.id + "@" + post.source;
		results.put(resultId, post);
		engineNames.add(post.source);
	}
	
	/////////////////////////////////////////////////////////////////
	// ARTICLE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Records all the articles corresponding to the retrieved posts.
	 * 
	 * @param keywords
	 * 		Keywords of the current search.
	 * 
	 * @throws IOException
	 * 		Problem while recording the articles. 
	 */
	public void buildArticles(String keywords) throws IOException
	{	logger.log("Recording the post as articles");
		logger.increaseOffset();
			int total = 0;
			for(SocialSearchResult result: results.values())
			{	if(result.status==null)
				{	total++;
					result.buildArticle(keywords);
				}
			}
		
		logger.decreaseOffset();
		logger.log("Article recording complete ("+total+")");
	}

	/////////////////////////////////////////////////////////////////
	// EXPORT		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportEvents(String keywords) throws UnsupportedEncodingException, FileNotFoundException
	{	String filePath = FileNames.getSocialSearchFolder(keywords) + File.separator + FileNames.FI_EVENT_TABLE;
		logger.log("Recording the events as a CVS file: "+filePath);
		logger.decreaseOffset();
			PrintWriter pw = FileTools.openTextFileWrite(filePath, "UTF-8");
			
			// write header
			List<String> cols = Arrays.asList(COL_PAGE_TITLE, COL_PAGE_URL, COL_PUB_DATE, COL_SOCIAL_ENGINE,
					COL_PAGE_STATUS, COL_EVENT_RANK, COL_EVENT_DATES,
					COL_EVENT_LOCATIONS, COL_EVENT_PERSONS, COL_EVENT_ORGANIZATIONS, COL_EVENT_FUNCTIONS,
					COL_EVENT_PRODUCTIONS, COL_EVENT_MEETINGS, COL_COMMENTS
			);
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
			for(SocialSearchResult result: results.values())
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
