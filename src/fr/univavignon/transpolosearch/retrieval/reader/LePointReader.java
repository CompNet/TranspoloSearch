package fr.univavignon.transpolosearch.retrieval.reader;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.retrieval.reader.ArticleReader;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;
import fr.univavignon.transpolosearch.tools.html.HtmlNames;
import fr.univavignon.transpolosearch.tools.html.HtmlTools;

/**
 * From a specified URL, this class retrieves a page
 * from the french newspaper Le Point (as of 17/08/2017),
 * and gives access to the raw and linked texts, as well
 * as other metadata (authors, publishing date, etc.).
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings("unused")
public class LePointReader extends ArticleReader
{	
	/**
	 * Method defined only for a quick test.
	 * 
	 * @param args
	 * 		Not used.
	 * 
	 * @throws Exception
	 * 		Whatever exception. 
	 */
	public static void main(String[] args) throws Exception
	{	
//		URL url = new URL("http://www.lepoint.fr/monde/attentat-de-barcelone-les-vehicules-lances-sur-la-foule-un-phenomene-devenu-habituel-en-europe-17-08-2017-2150646_24.php");
		URL url = new URL("http://www.lepoint.fr/invites-du-point/laetitia-strauch-bonart/strauch-bonart-cet-etrange-m-corbyn-17-08-2017-2150595_3096.php");
		
		ArticleReader reader = new LePointReader();
		Article article = reader.processUrl(url, ArticleLanguage.FR);
		System.out.println(article);
		article.write();
	}
	
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text allowing to detect the domain */
	public static final String DOMAIN = "www.lepoint.fr";

	@Override
	public String getDomain()
	{	return DOMAIN;
	}

	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/** Format used to parse the dates */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",Locale.FRENCH);
	/** String prefix used to specify the modification date in the Web page */
	private static final String UPDT_PREFIX = "MAJ : ";
	
	/** Class of the author names */
	private final static String CLASS_AUTHORS = "art-source";
	/** Class of the article description panel */
	private final static String CLASS_DESCRIPTION = "art-chapeau";
	/** Class of the article body */
	private final static String CLASS_ARTICLE_BODY = "art-text";
	/** Class of the article title */
	private final static String CLASS_TITLE = "art-titre";
	/** Class of the dates */
	private final static String CLASS_DATES = "art-date-infos";
	/** Class of the restricted access */
	private final static String CLASS_RESTRICTED = "freemium-tronque";
	
	@Override
	public Article processUrl(URL url, ArticleLanguage language) throws ReaderException
	{	Article result = null;
		String name = getName(url);
		
		try
		{	// init variables
			String title = "";
			StringBuilder rawStr = new StringBuilder();
			StringBuilder linkedStr = new StringBuilder();
			Date publishingDate = null;
			Date modificationDate = null;
			List<String> authors = new ArrayList<String>();
			
			// get the page
			String address = url.toString();
			logger.log("Retrieving page "+address);
			long startTime = System.currentTimeMillis();
			Document document  = retrieveSourceCode(name,url);
			if(document==null)
			{	logger.log("ERROR: Could not retrieve the document at URL "+url);
				throw new ReaderException("Could not retrieve the document at URL "+url);
			}
			
			// get the article element
			logger.log("Get the main element of the document");
			Elements articleElts = document.getElementsByTag(HtmlNames.ELT_ARTICLE);
			if(articleElts.size()==0)
				throw new IllegalArgumentException("No <article> element found in the Web page");
			Element articleElt = articleElts.first();
			Element headerElt = articleElt.getElementsByTag(HtmlNames.ELT_HEADER).first();
			
			// check if the access is restricted
			String classStr = articleElt.attr(HtmlNames.ATT_CLASS);
			if(classStr!=null && classStr.equalsIgnoreCase(CLASS_RESTRICTED))
				logger.log("WARNING: The access to this article is limited, only the beginning is available.");
	
			// get the title
			Element titleElt = headerElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_TITLE).first();
			title = titleElt.text(); 
			title = removeGtst(title).trim();
			logger.log("Get title: \""+title+"\"");
	
			// retrieve the dates
			Element datesElt = headerElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_DATES).first();
			Elements timeElts = datesElt.getElementsByTag(HtmlNames.ELT_TIME);
			if(!timeElts.isEmpty())
			{	Element timeElt = timeElts.get(0);
				publishingDate = HtmlTools.getDateFromTimeElt(timeElt,DATE_FORMAT);
				logger.log("Found the publishing date: "+publishingDate);
				if(timeElts.size()>1)
				{	timeElt = timeElts.get(1);
					modificationDate = HtmlTools.getDateFromTimeElt(timeElt,DATE_FORMAT);
					logger.log("Found the last modification date: "+modificationDate);
				}
				else
					logger.log("Did not find any last modification date");
			}
			else
				logger.log("Did not find any publication date");
			
			// retrieve the authors
			Element authorElt = headerElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_AUTHORS).first();
			String authorName = authorElt.text();
			authorName = removeGtst(authorName);
			authors.add(authorName);
	
			// get the description
			Element descriptionElt = headerElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_DESCRIPTION).first();
			if(descriptionElt==null)
				logger.log("Could not find any article presenstation");
			else
				processAnyElement(descriptionElt, rawStr, linkedStr);
	
			// processing the article main content
			Element contentElt = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_ARTICLE_BODY).first();
			processAnyElement(contentElt, rawStr, linkedStr);
			
			// create and init article object
			result = new Article(name);
			result.setTitle(title);
			result.setUrl(url);
			result.initRetrievalDate();
			result.setLanguage(language);
			result.setPublishingDate(publishingDate);
			if(modificationDate!=null)
				result.setModificationDate(modificationDate);
			if(authors!=null)
				result.addAuthors(authors);
			
			// clean text
			String rawText = rawStr.toString();
			result.setRawText(rawText);
			logger.log("Length of the raw text: "+rawText.length()+" chars.");
			String linkedText = linkedStr.toString();
			result.setLinkedText(linkedText);
			logger.log("Length of the linked text: "+linkedText.length()+" chars.");
			
			// get original html source code
			logger.log("Get original HTML source code.");
			String originalPage = document.toString();
			result.setOriginalPage(originalPage);
			logger.log("Length of the original page: "+originalPage.length()+" chars.");

			long endTime = System.currentTimeMillis();
			logger.log("Total duration: "+(endTime-startTime)+" ms.");
		}
		catch (ClientProtocolException e)
		{	e.printStackTrace();
		} 
		catch (ParseException e)
		{	e.printStackTrace();
		}
		catch (IOException e)
		{	e.printStackTrace();
		}
		
		return result;
	}
}
