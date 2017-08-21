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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.event.Event;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.time.Period;
import fr.univavignon.transpolosearch.tools.time.TimeFormatting;

/**
 * Represents one result of a social search engine and some info 
 * regarding how it was subsequently processed.
 * 
 * @author Vincent Labatut
 */
public class SocialSearchResult extends AbstractSearchResult
{
	/**
	 * Initializes the social search result.
	 * 
	 * @param id
	 * 		Unique id of this post. 
	 * @param author
	 * 		Author of this post. 
	 * @param date 
	 * 		Date of publication of this post.
	 * @param source
	 * 		Name of the social media publishing this post.
	 * @param content 
	 * 		Textual content of this post.
	 */
	public SocialSearchResult(String id, String author, Date date, String source, String content)
	{	this.id = id;
		this.author = author;
		this.date = date;
		this.source = source;
		this.content = content;
	}
	
	/////////////////////////////////////////////////////////////////
	// ID			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Unique ID of the post */
	public String id;
	
	/////////////////////////////////////////////////////////////////
	// CONTENT		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Textual content of this post */
	public String content;
	
	/////////////////////////////////////////////////////////////////
	// AUTHOR		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of the author of this post */
	public String author;
	
	/////////////////////////////////////////////////////////////////
	// DATE			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Publication date of this post */
	public Date date;
	
	/////////////////////////////////////////////////////////////////
	// SOURCE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of the social media on which this post has been published */
	public String source;
	
	/////////////////////////////////////////////////////////////////
	// RANK			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Rank of the result according to the source */
	public int rank = -1;
	
	/////////////////////////////////////////////////////////////////
	// COMMENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Posts commenting this post */ 
	public List<SocialSearchResult> comments = new ArrayList<SocialSearchResult>();

	/////////////////////////////////////////////////////////////////
	// URL			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Direct URL of the post */ 
	public URL url;

	/////////////////////////////////////////////////////////////////
	// ARTICLE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Convert this post under the form of a proper {@code Article}. 
	 * The comments are added as paragraphs after the actual post content.
	 * 
	 * @throws IOException 
	 * 		Problem while recording the article.
	 */
	public void buildArticle() throws IOException
	{	String corpusFolder = FileNames.FO_SOCIAL_SEARCH_RESULTS + File.separator + source;
		article = new Article(id, corpusFolder);

		// content
		String text = content;
		for(SocialSearchResult com: comments)
			text = text + "\n\n" + com.content;
		article.setRawText(text);
		article.setLinkedText(text);
		article.cleanContent();
		
		// metadata
		article.setTitle(id);
		article.setLanguage(ArticleLanguage.FR);	// TODO we suppose the language is French, to be generalized later
		article.setPublishingDate(date);
		Calendar cal = Calendar.getInstance();
		Date currentDate = cal.getTime();
		article.setRetrievalDate(currentDate);
		article.setUrl(url);
		
		// record to file
		article.write();
	}
	
	/////////////////////////////////////////////////////////////////
	// FILE			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Date format used to write/read comments in text files */
	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	/** 
	 * Write this post (and its comments) to 
	 * a text file.
	 * 
	 * @param writer
	 * 		Writer on the currently open text file.
	 */
	public void writeAsText(PrintWriter writer)
	{	writer.println(id);
		writer.println(author);
		writer.println(DATE_FORMAT.format(date));
		writer.println(source);
		writer.println(content);
		if(url==null)
			writer.println("N/A");
		else
			writer.println(url.toString());
		writer.println(comments.size());
		for(SocialSearchResult comment: comments)
			comment.writeAsText(writer);
	}
	
	/**
	 * Retrieve a post from a text file, as well as its comments
	 * (if any).
	 * 
	 * @param scanner
	 * 		Scanner on the previously opened text file.
	 * @return
	 * 		An object representing the post read from the text file.
	 */
	public static SocialSearchResult readFromText(Scanner scanner)
	{	SocialSearchResult result = null;
	
		try
		{	// init the post
			String id = scanner.nextLine().trim();
			String author = scanner.nextLine().trim();
			String dateStr = scanner.nextLine().trim();
			Date date = DATE_FORMAT.parse(dateStr);
			String source = scanner.nextLine().trim();
			String content = scanner.nextLine().trim();
			result = new SocialSearchResult(id, author, date, source, content);
			String urlStr = scanner.nextLine().trim();
			if(!urlStr.equals("N/A"))
			{	URL url = new URL(urlStr);
				result.url = url;
			}
			
			// add its comments
			String nbrStr = scanner.nextLine().trim();
			int nbr = Integer.parseInt(nbrStr);
			for(int i=0;i<nbr;i++)
			{	SocialSearchResult comment = readFromText(scanner);
				result.comments.add(comment);
			}
		}
		catch(ParseException e)
		{	e.printStackTrace();
		} 
		catch(MalformedURLException e) 
		{	e.printStackTrace();
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected int extractEvents(boolean bySentence, int nbr)
	{	int result = extractEvents(bySentence,nbr,true);
		return result;
	}
	
	/**
	 * Records the results of the social search as a CSV file.
	 * 
	 * @return
	 * 		Map representing the events associated to this social
	 * 		search result (can be empty). 
	 */
	@Override
	protected List<Map<String,String>> exportEvents()
	{	List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		
		int rank = 0;
		for(Event event: events)
		{	Map<String,String> map = new HashMap<String,String>();
			result.add(map);
			rank++;

			// general stuff
			map.put(AbstractSearchResults.COL_PAGE_TITLE,"\""+article.getTitle()+"\"");
			if(article.getUrl()!=null)
				map.put(AbstractSearchResults.COL_PAGE_URL,"\""+article.getUrl().toString()+"\"");
			map.put(AbstractSearchResults.COL_LENGTH,"\""+article.getRawText().length()+"\"");
			map.put(AbstractSearchResults.COL_PAGE_STATUS,status);
			map.put(AbstractSearchResults.COL_COMMENTS,"");
			
			// publication date
			java.util.Date pubDate = article.getPublishingDate();
			if(pubDate!=null)
			{	String pubDateStr = TimeFormatting.formatDate(pubDate);
				map.put(AbstractSearchResults.COL_PUB_DATE,pubDateStr);
			}
			
			// search engine
			if(source!=null)
				map.put(AbstractSearchResults.COL_SOCIAL_ENGINE,source);
			
			if(event!=null)
			{	map.put(AbstractSearchResults.COL_EVENT_RANK,Integer.toString(rank));
				
				// dates
				Period period = event.getPeriod();
				String periodStr = period.toString();
				periodStr = periodStr.replaceAll("[\\n\\r]", " ");
				map.put(AbstractSearchResults.COL_EVENT_DATES,periodStr);
				
				// locations
				{	String locations = "\"";
					Collection<String> locs = event.getLocations();
					Iterator<String> itLoc = locs.iterator();
					while(itLoc.hasNext())
					{	String loc = itLoc.next();
						loc = loc.replaceAll("[\\n\\r]", " ");
						locations = locations + loc;
						if(itLoc.hasNext())
							locations = locations + ", ";
					}
					locations = locations + "\"";
					map.put(AbstractSearchResults.COL_EVENT_LOCATIONS,locations);
				}
				
				// persons
				{	String persons = "\"";
					Collection<String> perss = event.getPersons();
					Iterator<String> itPers = perss.iterator();
					while(itPers.hasNext())
					{	String pers = itPers.next();
						pers = pers.replaceAll("[\\n\\r]", " ");
						persons = persons + pers;
						if(itPers.hasNext())
							persons = persons + ", ";
					}
					persons = persons + "\"";
					map.put(AbstractSearchResults.COL_EVENT_PERSONS,persons);
				}
				
				// organizations
				{	String organizations = "\"";
					Collection<String> orgs = event.getOrganizations();
					Iterator<String> itOrg = orgs.iterator();
					while(itOrg.hasNext())
					{	String org = itOrg.next();
						org = org.replaceAll("[\\n\\r]", " ");
						organizations = organizations + org;
						if(itOrg.hasNext())
							organizations = organizations + ", ";
					}
					organizations = organizations + "\"";
					map.put(AbstractSearchResults.COL_EVENT_ORGANIZATIONS,organizations);
				}
				
				// functions
				{	String functions = "\"";
					Collection<String> funs = event.getFunctions();
					Iterator<String> itFun = funs.iterator();
					while(itFun.hasNext())
					{	String fun = itFun.next();
						fun = fun.replaceAll("[\\n\\r]", " ");
						functions = functions + fun;
						if(itFun.hasNext())
							functions = functions + ", ";
					}
					functions = functions + "\"";
					map.put(AbstractSearchResults.COL_EVENT_FUNCTIONS,functions);
				}
				
				// productions
				{	String productions = "\"";
					Collection<String> prods = event.getProductions();
					Iterator<String> itProd = prods.iterator();
					while(itProd.hasNext())
					{	String prod = itProd.next();
						prod = prod.replaceAll("[\\n\\r]", " ");
						productions = productions + prod;
						if(itProd.hasNext())
							productions = productions + ", ";
					}
					productions = productions + "\"";
					map.put(AbstractSearchResults.COL_EVENT_PRODUCTIONS,productions);
				}
				
				// meetings
				{	String meetings = "\"";
					Collection<String> meets = event.getMeetings();
					Iterator<String> itMeet = meets.iterator();
					while(itMeet.hasNext())
					{	String meet = itMeet.next();
						meet = meet.replaceAll("[\\n\\r]", " ");
						meetings = meetings + meet;
						if(itMeet.hasNext())
							meetings = meetings + ", ";
					}
					meetings = meetings + "\"";
					map.put(AbstractSearchResults.COL_EVENT_MEETINGS,meetings);
				}
			}
		}
		
		return result;
	}
}
