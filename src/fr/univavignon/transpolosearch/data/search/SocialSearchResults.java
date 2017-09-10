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
import java.util.ArrayList;
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
	 * @param includeComments 
	 * 		Whether ({@code true}) or not ({@code false}) to include comments 
	 * 		in the proper article (or just the main post).
	 * 
	 * @throws IOException
	 * 		Problem while recording the articles. 
	 */
	public void buildArticles(boolean includeComments) throws IOException
	{	logger.log("Recording the post as articles");
		logger.increaseOffset();
			int total = 0;
			for(SocialSearchResult result: results.values())
			{	if(result.status==null)
				{	total++;
					result.buildArticle(includeComments);
				}
			}
		
		logger.decreaseOffset();
		logger.log("Article recording complete ("+total+")");
	}

	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportEvents(boolean bySentence, boolean byCluster) throws UnsupportedEncodingException, FileNotFoundException
	{	String fileName;
		if(bySentence)
			if(byCluster)
				fileName = FileNames.FI_CLUSTER_TABLE_SENTENCE;
			else
				fileName = FileNames.FI_EVENT_TABLE_SENTENCE;
		else
			if(byCluster)
				fileName = FileNames.FI_CLUSTER_TABLE_ARTICLE;
			else
				fileName = FileNames.FI_EVENT_TABLE_ARTICLE;
		String filePath = FileNames.FO_SOCIAL_SEARCH_RESULTS + File.separator + fileName;
		logger.log("Recording the events as a CVS file: "+filePath);
		logger.decreaseOffset();
			PrintWriter pw = FileTools.openTextFileWrite(filePath, "UTF-8");
			
			// write header
			List<String> startCols = Arrays.asList(COL_COMMENTS);
			List<String> endCols = Arrays.asList(COL_TITLE, COL_URL, COL_LENGTH, COL_PUB_DATE, 
					COL_AUTHORS, COL_SOCIAL_ENGINE, COL_STATUS, COL_EVENT_RANK, COL_EVENT_DATES,
					COL_EVENT_LOCATIONS, COL_EVENT_PERSONS, COL_EVENT_ORGANIZATIONS, COL_EVENT_FUNCTIONS,
					COL_EVENT_PRODUCTIONS, COL_EVENT_MEETINGS
			);
			List<String> cols = new ArrayList<String>();
			cols.addAll(startCols);
			if(byCluster)
				cols.add(COL_CLUSTER);
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
			int total = 0;
			if(byCluster)
			{	logger.log("Treat each cluster separately");
				for(int i=0;i<mapClustRes.size();i++)
				{	List<SocialSearchResult> res = mapClustRes.get(i);
					List<Integer> evt = mapClustEvt.get(i);
					for(int j=0;j<res.size();j++)
					{	// setup the line
						SocialSearchResult r = res.get(j);
						List<Map<String,String>> lines = r.exportEvents();
						int idx = evt.get(j);
						Map<String,String> line = lines.get(idx);
						line.put(COL_CLUSTER, Integer.toString(i+1));
						// write the line
						it = cols.iterator();
						while(it.hasNext())
						{	String col = it.next();
							String val = line.get(col);
							if(val!=null)
								pw.print(val);
							if(it.hasNext())
								pw.print(",");
						}
						pw.println();
					}
				total = i;
				}
			}
			else
			{	logger.log("Treat each article separately");
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
			}
			
			pw.close();
		logger.decreaseOffset();
		if(byCluster)
			logger.log("Wrote "+total+" event clusters");
		else
			logger.log("Wrote "+total+" events");
	}
}
