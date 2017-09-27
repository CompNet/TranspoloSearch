package fr.univavignon.transpolosearch.extraction;

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
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.xml.sax.SAXException;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univavignon.transpolosearch.tools.string.LinkTools;

/**
 * This class handles extracts a collection of Wikipedia articles,
 * based on a starting page, by following the internal links (i.e.
 * links to other articles) until a maximal distance is reached.
 * 
 * @author Vincent Labatut
 */
public class WikipediaExtractor
{	
	/**
	 * Just for testing purposes.
	 * 
	 * @param args
	 * 		Ignored.
	 * 
	 * @throws Exception
	 * 		Whatever problem. 
	 */
	public static void main(String[] args) throws Exception 
	{	logger.setName("WP-extraction");
		
//		WikipediaExtractor we = new WikipediaExtractor(ArticleLanguage.FR);
//		List<Article> articles = we.extractCollection("Recherche_d'information",2);
//		System.out.println(articles);
		
		normFiles();
	}
	
	/**
	 * Moves the file, used just once...
	 * 
	 * @throws Exception
	 * 		Whatever problem. 
	 */
	private static void normFiles() throws Exception
	{	Map<String,String> map = new HashMap<String,String>();
		
		// copy the files
		logger.log("Copy all files in the same folder");
		logger.increaseOffset();
			String rootPath = "out/web_search/_pages";
			File root = new File(rootPath);
			File[] folders = root.listFiles();
			for(int i=0;i<folders.length;i++)
			{	logger.log("Processing file #"+(i+1)+"/"+folders.length+"");
				File folder = folders[i];
				String name = folder.getName();
				
				// get unique ID
	    		UUID id = UUID.randomUUID();
	    		map.put(id.toString(), name);
	    		
	    		// copy file
				String oldPath = folder.getPath() + File.separator + "raw" + FileNames.EX_TEXT;
				File oldFile = new File(oldPath);
				String newPath = rootPath + File.separator + id + FileNames.EX_TEXT; 
				File newFile = new File(newPath);
				FileTools.copy(oldFile, newFile);
			}
		logger.decreaseOffset();
		
		// create mapping file
		String mapPath = rootPath + File.separator + "_conversion" + FileNames.EX_TEXT;
		logger.log("Record the map in file "+mapPath);
		PrintWriter pw = FileTools.openTextFileWrite(mapPath, "UTF-8");
		Set<String> ids = new TreeSet<String>(map.keySet());
		for(String id: ids)
		{	String name = map.get(id);
			name = URLDecoder.decode(name, "UTF-8");
			pw.println(id + "\t" + name);
		}
		pw.close();
		
		logger.log("Done !");
	}
	
	/**
	 * Builds and initializes an extractor object.
	 *
	 * @param language
	 * 		Targeted language for the extracted pages, and
	 * 		also of the targeted Wikipedia version.
	 * 
	 * @throws ProcessorException
	 * 		Problem while initializing the NER tool. 
	 */
	public WikipediaExtractor(ArticleLanguage language) throws ProcessorException
	{	this.language = language;
		articleRetriever = new ArticleRetriever(true);
		articleRetriever.setLanguage(language);
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// PROCESS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Prefix used for WP internal links */
	private static final String INTERNAL_LINK_PREFIX = "/wiki/";
	/** Prefix used for WP full links (beginning of the prefix) */
	private static final String FULL_LINK_PREFIX_START = "http://";
	/** Prefix used for WP full links (end of the prefix) */
	private static final String FULL_LINK_PREFIX_END = ".wikipedia.org/wiki/";
	/** Object used to retrieve the articles */
	private ArticleRetriever articleRetriever = null;
	/** Targeted language */
	private ArticleLanguage language = null;
	
	/**
	 * Extracts a collection of Wikipedia articles, based on the specified seeds.
	 * These are themselves Wikipedia pages, and the extractor will retrieve all
	 * pages they link to, proceeding iteratively until a maximal distance is 
	 * reached (relatively to the seeds).
	 * 
	 * @param seeds
	 * 		List of titles of Wikipedia pages.
	 * @param maxDistance
	 * 		Maximal distance of the retrieved pages, relatively to the seeds.
	 * @return
	 * 		The list of retrieved articles.
	 * 
	 * @throws ReaderException 
	 * 		Problem while retrieving the article. 
	 * @throws IOException 
	 * 		Problem while retrieving the article. 
	 * @throws SAXException 
	 * 		Problem while retrieving the article. 
	 * @throws ParseException 
	 * 		Problem while retrieving the article. 
	 */
	public List<Article> extractCollection(List<String> seeds, int maxDistance) throws ParseException, SAXException, IOException, ReaderException
	{	logger.log("Retrieving the WP articles for the specified "+seeds.size()+" seeds");
		logger.increaseOffset();
		
		// create the data structures
		List<Article> result = new ArrayList<Article>();
		Set<String> processedNames = new TreeSet<String>();
		
		// init the queues
		Queue<String> remainingNames = new LinkedList<String>();
		Queue<Integer> distances = new LinkedList<Integer>(); 
		for(String seed: seeds)
		{	remainingNames.offer(seed);
			distances.offer(0);
		}
		
		// process each page name in the queue until it is empty, or the max distance is reached
		int dist = 0;
		int i = 1;
		while(!remainingNames.isEmpty() && dist<=maxDistance)
		{	logger.log("Processing page #"+i);
			logger.increaseOffset();
			
			// get the page distance to the closest seed
			dist = distances.poll();
			logger.log("Distance: "+dist);
			if(dist<=maxDistance)
			{	// get the page name
				String name = remainingNames.poll();
				logger.log("Name: "+name);
				processedNames.add(name);
				
				// load the corresponding article
				String urlStr = FULL_LINK_PREFIX_START + language.toString().toLowerCase() + FULL_LINK_PREFIX_END + name;
				Article article = articleRetriever.process(urlStr);
				result.add(article);
				
				// extract the new names and put them at the end of the queue
				List<String> mentionedNames = getMentionedNames(article,processedNames);
				logger.log("Mentioned names: "+mentionedNames.size());
				for(String n: mentionedNames)
				{	remainingNames.offer(n);
					distances.offer(dist+1);
				}
			}
			else
				logger.log("Max distance ("+maxDistance+") reached >> process over");

			i++;
			
			logger.decreaseOffset();			
		}
		
		logger.log("Number of articles returned: "+result.size());
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Extracts a collection of Wikipedia articles, based on the specified seed.
	 * This is itself a Wikipedia page, and the extractor will retrieve all
	 * pages it links to, proceeding iteratively until a maximal distance is 
	 * reached (relatively to the seed).
	 * 
	 * @param seed
	 * 		Titles of a Wikipedia page.
	 * @param maxDistance
	 * 		Maximal distance of the retrieved pages, relatively to the seed.
	 * @return
	 * 		The list of retrieved articles.
	 * 
	 * @throws ReaderException 
	 * 		Problem while retrieving the article. 
	 * @throws IOException 
	 * 		Problem while retrieving the article. 
	 * @throws SAXException 
	 * 		Problem while retrieving the article. 
	 * @throws ParseException 
	 * 		Problem while retrieving the article. 
	 */
	public List<Article> extractCollection(String seed, int maxDistance) throws ParseException, SAXException, IOException, ReaderException
	{	List<String> seeds = new ArrayList<String>();
		seeds.add(seed);
		List<Article> result = extractCollection(seeds, maxDistance);
		return result;
	}
	
	/**
	 * Takes an article and return the names of the WP pages that it mentions,
	 * and that have not been processed yet.
	 *  
	 * @param article
	 * 		Article to process.
	 * @param processedNames
	 * 		Current list of previously processed page names.
	 * @return
	 * 		List of new page names mentioned in the specified article.
	 */
	private List<String> getMentionedNames(Article article, Set<String> processedNames)
	{	List<String> result = new LinkedList<String>();
		
		// get the urls of the pages mentioned in the article
		String linkedText = article.getLinkedText();
		List<String> tmp = LinkTools.extractUrls(linkedText);
		Set<String> urls = new TreeSet<String>(tmp);
		
		// process each url
		for(String url: urls)
		{	// get the name for the current url
			if(!url.startsWith(INTERNAL_LINK_PREFIX))
			{	//throw new IllegalArgumentException("Incorrect internal WP URL: "+url);
				logger.log("Found an unusual URL: "+url);
			}
			else
			{	String name = url.substring(INTERNAL_LINK_PREFIX.length());
				// must not have been processed before
				if(!processedNames.contains(name))
					result.add(name);
			}
		}
		return result;
	}
}
