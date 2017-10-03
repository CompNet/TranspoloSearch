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
public class SocialSearchResults extends AbstractSpecificSearchResults<SocialSearchResult>
{	
	/////////////////////////////////////////////////////////////////
	// RESULTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Adds the specified posts to this list of results.
	 * 
	 * @param post
	 * 		The social media posts to add to this collection of results.
	 * @param engineName
	 * 		Engine returning the post.
	 */
	public void addResult(SocialSearchResult post, String engineName)
	{	String resultId = post.id + "@" + post.source;
		SocialSearchResult result = results.get(resultId);
		if(result==null)
			results.put(resultId, post);
		engineNames.add(engineName);
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
	// CSV			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public void exportResults(String fileName) throws UnsupportedEncodingException, FileNotFoundException
	{	logger.log("Recording all the social search results in a file"+fileName);
		logger.increaseOffset();
		
		// create folder
		File folder = new File(FileNames.FO_SOCIAL_SEARCH_RESULTS);
		folder.mkdirs();
		String filePath = folder + File.separator + fileName;
		logger.log("Recording in CSV file \""+filePath+"\"");
		
		// setup colon names
		List<String> cols = Arrays.asList(COL_NOTES, COL_TITLE, COL_STATUS, COL_PUB_DATE, 
				COL_LIKES, COL_SHARES, COL_COMMENTS,
				COL_AUTHORS, COL_ORIGINAL, COL_ENGINE, COL_LENGTH, COL_ARTICLE_CLUSTER,
				COL_ENT_DATES, COL_ENT_LOCATIONS, COL_ENT_PERSONS, COL_ENT_ORGANIZATIONS, COL_ENT_FUNCTIONS, COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
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
			pw.println();
		}
		
		// write data and close file
		for(SocialSearchResult result: results.values())
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
	public void exportEvents(boolean bySentence, boolean byCluster, String filePrefix) throws UnsupportedEncodingException, FileNotFoundException
	{	String fileName = filePrefix;
		if(bySentence)
			if(byCluster)
				fileName = fileName + FileNames.FI_EVENT_CLUSTERS_BYSENTENCE;
			else
				fileName = fileName + FileNames.FI_EVENT_LIST_BYSENTENCE;
		else
			if(byCluster)
				fileName = fileName + FileNames.FI_EVENT_CLUSTERS_BYARTICLE;
			else
				fileName = fileName + FileNames.FI_EVENT_LIST_BYARTICLE;
		String filePath = FileNames.FO_SOCIAL_SEARCH_RESULTS + File.separator + fileName;
		logger.log("Recording the events as a CVS file: "+filePath);
		logger.decreaseOffset();
			
			// setup colon names
			List<String> startCols = Arrays.asList(COL_NOTES);
			List<String> endCols = Arrays.asList(COL_TITLE, COL_URL, COL_LENGTH, COL_PUB_DATE, COL_LIKES, COL_SHARES, COL_COMMENTS, 
					COL_AUTHORS, COL_SOCIAL_ENGINE, COL_STATUS, COL_ARTICLE_CLUSTER, COL_EVENT_RANK, COL_ENT_DATES,
					COL_ENT_LOCATIONS, COL_ENT_PERSONS, COL_ENT_ORGANIZATIONS, COL_ENT_FUNCTIONS,
					COL_ENT_PRODUCTIONS, COL_ENT_MEETINGS
			);
			List<String> cols = new ArrayList<String>();
			cols.addAll(startCols);
			if(byCluster)
				cols.add(COL_EVENT_CLUSTER);
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
								pw.print("\""+val+"\"");
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
