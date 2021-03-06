package fr.univavignon.transpolosearch.retrieval.reader.journals;

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

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.retrieval.reader.AbstractArticleReader;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.tools.string.StringTools;

import fr.univavignon.tools.web.HtmlNames;
import fr.univavignon.tools.web.HtmlTools;

/**
 * From a specified URL, this class retrieves a page
 * from the French newspaper LeMonde (as of 17/08/2017),
 * and gives access to the raw and linked texts, as well
 * as other metadata (authors, publishing date, etc.).
 * 
 * @author Vincent Labatut
 */
public class LeMondeReader extends AbstractJournalReader
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
//		URL url = new URL("http://www.lemonde.fr/les-decodeurs/article/2017/08/09/navire-antimigrants-c-star-une-mission-inutile-voire-illegale_5170650_4355770.html");
//		URL url = new URL("http://www.lemonde.fr/sport/article/2017/08/12/sur-les-traces-des-jeux-olympiques-de-1900-et-1924_5171678_3242.html");
		URL url = new URL("http://www.lemonde.fr/videos/2.html");
		
		AbstractArticleReader reader = new LeMondeReader();
		Article article = reader.processUrl(url, ArticleLanguage.FR);
		System.out.println(article);
		article.write();
	}
	
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text allowing to detect the domain */
	public static final String DOMAIN = "www.lemonde.fr";
	/** Subdomain with very different article structures */
	public static final List<String> SUB_DOMAINS = Arrays.asList("/video/","/videos/");
	
	@Override
	public String getDomain()
	{	return DOMAIN;
	}
	
	/**
	 * Checks whether the specified URL is compatible
	 * with this reader.
	 * 
	 * @param url
	 * 		URL to check.
	 * @return
	 * 		{@code true} iff this reader can handle the URL.
	 */
	public static boolean checkDomain(String url)
	{	boolean result = url.contains(DOMAIN);
		Iterator<String> it = SUB_DOMAINS.iterator();
		while(result && it.hasNext())
		{	String subDomain = it.next();
			result = !url.contains(subDomain);
		}
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/** Format used to parse the dates */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.FRENCH);
	
	/** Text displayed for limited access content */
	private final static String CONTENT_LIMITED_ACCESS = "L’accès à la totalité de l’article est protégé";
	
	/** Id of the element containing the article content in the Wikipedia page */
	private final static String ID_ARTICLE_BODY = "articleBody";
	
	/** Class of the author names */
	private final static String CLASS_AUTHOR = "auteur";
	/** Itemprop of the article body */
	private final static String ITEMPROP_ARTICLE_BODY = "articleBody";
	/** Class of the article "related articles" links */
	private final static String CLASS_RELATED_ARTICLES = "lire";
	
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
				logger.log("WARNING: found several <article> elements in the same page.");
			
			// retrieve the dates
//			Element signatureElt = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_SIGNATURE).first();
			Elements timeElts = articleElt.getElementsByTag(HtmlNames.ELT_TIME);
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
			Elements authorElts = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_AUTHOR);
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
			
			// processing each element in the article body
			Element bodyElt = articleElt.getElementById(ID_ARTICLE_BODY);
			if(bodyElt==null)
			{	//Elements bodyElts = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_ARTICLE_BODY);
				Elements bodyElts = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_ITEMPROP, ITEMPROP_ARTICLE_BODY);
				bodyElt = bodyElts.first();
				if(bodyElts.size()==0)
					throw new IllegalArgumentException("No article body found in the Web page");
				else if(bodyElts.size()>1)
					logger.log("WARNING: There are more than 1 element for the article body, which is unusual. Let's focus on the first.");
			}
			processAnyElement(bodyElt, rawStr, linkedStr);
			
			// create and init article object
			result = new Article(name);
			result.setTitle(title);
			result.setUrl(url);
			result.initRetrievalDate();
			result.setPublishingDate(publishingDate);
			if(modificationDate!=null)
				result.setModificationDate(modificationDate);
			if(authors!=null)
				result.addAuthors(authors);
			
			// add the title to the content, just in case the entity appears there but not in the article body
			String rawText = rawStr.toString();
			String linkedText = linkedStr.toString();
			if(title!=null && !title.isEmpty())
			{	rawText = title + "\n" + rawText;
				linkedText = title + "\n" + linkedText;
			}
			
			// clean text
//			rawText = cleanText(rawText);
//			rawText = ArticleCleaning.replaceChars(rawText);
			result.setRawText(rawText);
			logger.log("Length of the raw text: "+rawText.length()+" chars.");
//			linkedText = cleanText(linkedText);
//			linkedText = ArticleCleaning.replaceChars(linkedText);
			result.setLinkedText(linkedText);
			logger.log("Length of the linked text: "+linkedText.length()+" chars.");

			// language
			if(language==null)
			{	language = StringTools.detectLanguage(rawText,false);
				logger.log("Detected language: "+language);
			}
			result.setLanguage(language);
			
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
	{	String eltClass = element.attr(HtmlNames.ATT_CLASS);
		// we ignore the inter-paragraph hyperlinks
		if(!eltClass.equalsIgnoreCase(CLASS_RELATED_ARTICLES))
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
