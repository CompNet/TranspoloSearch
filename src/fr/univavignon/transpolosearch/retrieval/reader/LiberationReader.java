package fr.univavignon.transpolosearch.retrieval.reader;

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
 * from the french newspaper Libération (as of 05/05/2015),
 * and gives access to the raw and linked texts, as well
 * as other metadata (authors, publishing date, etc.).
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings("unused")
public class LiberationReader extends ArticleReader
{
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text allowing to detect wikipedia URL */
	public static final String DOMAIN = "www.liberation.fr";

	@Override
	public String getDomain()
	{	return DOMAIN;
	}

	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/** Format used to parse the dates */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	/** Text displayed for limited access content */
	private final static String CONTENT_LIMITED_ACCESS = "Article réservé aux abonnés";
	/** Text displayed for "related articles" links */
	private final static String CONTENT_RELATED_ARTICLES = "Lire aussi";
	
	/** Id of the element containing the article content in the Wikipedia page */
	private final static String ID_ARTICLE_BODY = "article-body";

	/** Class of the author names */
	private final static String CLASS_AUTHOR = "author";
	/** Class of the article description panel */
	private final static String CLASS_DESCRIPTION = "description";
	/** Class of the article information panel */
	private final static String CLASS_INFO = "info";
	/** Class of footnotes */
	private final static String CLASS_FOOTNOTE = "note";
	
	@Override
	public Article processUrl(URL url, ArticleLanguage language) throws ReaderException
	{	Article result = null;
		String name = getName(url);
		
		try
		{	// get the page
			String address = url.toString();
			logger.log("Retrieving page "+address);
			long startTime = System.currentTimeMillis();
			Document document  = retrieveSourceCode(name,url);
			if(document==null)
			{	logger.log("ERROR: Could not retrieve the document at URL "+url);
				throw new ReaderException("Could not retrieve the document at URL "+url);
			}
					
			// get its title
			Element titleElt = document.getElementsByTag(HtmlNames.ELT_TITLE).first();
			String title = titleElt.text();
			title = removeGtst(title);
			logger.log("Get title: "+title);
			
			// check if the access is restricted
			Elements limitedElts = document.getElementsContainingText(CONTENT_LIMITED_ACCESS);
			if(!limitedElts.isEmpty())
				logger.log("WARNING: The access to this article is limited, only the beginning is available.");
			
			// get the article element
			logger.log("Get the main element of the document");
			Elements articleElts = document.getElementsByTag(HtmlNames.ELT_ARTICLE);
			Element articleElt = articleElts.first();
			if(articleElts.size()==0)
				throw new IllegalArgumentException("No <article> element found in the Web page");
			else if(articleElts.size()>1)
				logger.log("WARNING: There are more than 1 <article> elements, which is unusual. Let's focus on the first.");
			Element headerElt = articleElt.getElementsByTag(HtmlNames.ELT_HEADER).first();
			Element infoElt = headerElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_INFO).first();
			
			// retrieve the dates
			Elements timeElts = infoElt.getElementsByTag(HtmlNames.ELT_TIME);
			Element publishingElt = timeElts.first();
			Date publishingDate = HtmlTools.getDateFromTimeElt(publishingElt,DATE_FORMAT);
			logger.log("Found the publishing date: "+publishingDate);
			Date modificationDate = null;
			if(timeElts.size()>1)
			{	Element modificationElt = timeElts.last();
				modificationDate = HtmlTools.getDateFromTimeElt(modificationElt,DATE_FORMAT);
				logger.log("Found a last modification date: "+modificationDate);
			}
			else
				logger.log("Did not find any last modification date");
			
			// retrieve the authors
			List<String> authors = null;
			Elements authorElts = infoElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_AUTHOR);
			if(authorElts.isEmpty())
				logger.log("WARNING: could not find any author, which is unusual");
			else
			{	logger.log("List of the authors found for this article:");
				logger.increaseOffset();
				authors = new ArrayList<String>();
				for(Element authorElt: authorElts)
				{	String authorName = authorElt.text();
					authorName = removeGtst(authorName);
					logger.log(authorName);
					authors.add(authorName);
				}
				logger.decreaseOffset();
			}
			
			// get raw and linked texts
			logger.log("Get raw and linked texts");
			StringBuilder rawStr = new StringBuilder();
			StringBuilder linkedStr = new StringBuilder();

			// get the description
			Element descriptionElt = headerElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_DESCRIPTION).first();
			Element h2Elt = descriptionElt.getElementsByTag(HtmlNames.ELT_H2).first();
			String text = h2Elt.text() + "\n";
			text = removeGtst(text);
			rawStr.append(text);
			linkedStr.append(text);

			// processing each element in the body
			Element bodyElt = articleElt.getElementById(ID_ARTICLE_BODY);
			Elements contentElts = bodyElt.children();
			Iterator<Element> it = contentElts.iterator();
			Element contentElt;
			do
				contentElt = it.next();
			while(!contentElt.tagName().equalsIgnoreCase(HtmlNames.ELT_DIV));
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
//			rawText = cleanText(rawText);
//			rawText = ArticleCleaning.replaceChars(rawText);
			result.setRawText(rawText);
			logger.log("Length of the raw text: "+rawText.length()+" chars.");
			String linkedText = linkedStr.toString();
//			linkedText = cleanText(linkedText);
//			linkedText = ArticleCleaning.replaceChars(linkedText);
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

	/////////////////////////////////////////////////////////////////
	// ELEMENTS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void processParagraphElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	// we ignore the "related article" links
		String str = element.text();
		String eltClass = element.attr(HtmlNames.ATT_CLASS);
		if(!str.startsWith(CONTENT_RELATED_ARTICLES) && !eltClass.equalsIgnoreCase(CLASS_FOOTNOTE))
		{	// possibly add a new line character first (if the last one is not already a newline)
			if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)!='\n')
			{	rawStr.append("\n");
				linkedStr.append("\n");
			}
			
			// recursive processing
			processAnyElement(element,rawStr,linkedStr);
			
			// possibly add a new line character (if the last one is not already a newline)
			if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)!='\n')
			{	rawStr.append("\n");
				linkedStr.append("\n");
			}
		}
	}

}