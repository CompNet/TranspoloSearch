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
 * from the french newspaper Le Figaro (as of 17/08/2017),
 * and gives access to the raw and linked texts, as well
 * as other metadata (authors, publishing date, etc.).
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings("unused")
public class LeFigaroReader extends ArticleReader
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
		URL url = new URL("http://www.lefigaro.fr/international/2017/08/16/01003-20170816ARTFIG00075-violences-a-charlottesville-la-polemique-racontee-en-quatre-episodes.php");
//		URL url = new URL("http://www.lefigaro.fr/sciences/2017/08/17/01008-20170817ARTFIG00132-daniel-zagury-l-homme-qui-se-vaccina-contre-le-sida.php");
		
		ArticleReader reader = new LeFigaroReader();
		Article article = reader.processUrl(url, ArticleLanguage.FR);
		System.out.println(article);
		article.write();
	}
	
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text allowing to detect the domain */
	public static final String DOMAIN = "www.lefigaro.fr";

	@Override
	public String getDomain()
	{	return DOMAIN;
	}

	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/** Format used to parse the dates */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	/** Item prop of the main article */
	private final static String CONTENT_MAIN = "mainContentOfPage";
	/** Item prop of the author name */
	private final static String AUTHOR_NAME = "name";

	/** Class of the author names */
	private final static String CLASS_AUTHOR = "fig-auteur";
	/** Class of the article description panel */
	private final static String CLASS_DESCRIPTION = "fig-chapo";
	/** Class of the article body */
	private final static String CLASS_ARTICLE_BODY = "fig-article-body";
	/** Class of the publication date */
	private final static String CLASS_DATE_PUB = "fig-date-pub";
	/** Class of the modification date */
	private final static String CLASS_DATE_UPDT = "fig-date-maj";
	/** Class of the restricted access */
	private final static String CLASS_PROMO = "fig-article-promo";
	
	/** ID of the restricted access */
	private final static String ID_KLOCKED = "kLocked";
	
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
			List<String> authors = null;
			
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
			Element articleElt = articleElts.first();
			if(articleElts.size()==0)
				throw new IllegalArgumentException("No <article> element found in the Web page");
			else 
			{	String itemPropVal = articleElt.attr(HtmlNames.ATT_ITEMPROP);
				if(!itemPropVal.equals(CONTENT_MAIN))
					logger.log("WARNING: The first article is not the main content of the page, which is not normal.");
			}
			
			// check if the access is restricted
			Elements promoElts = articleElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_PROMO);
			if(!promoElts.isEmpty())
				logger.log("WARNING: The access to this article is limited, only the beginning is available.");
			
			// get the title
			Element titleElt = articleElt.getElementsByTag(HtmlNames.ELT_H1).first();
			List<TextNode> textNodes = titleElt.textNodes();	// we need to ignore "avant-première" and other similar indications
			for(TextNode textNode: textNodes)
				title = title + " " + textNode.text();
			title = removeGtst(title).trim();
			logger.log("Get title: \""+title+"\"");
			
			// retrieve the dates
			Elements pubDateElts = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_DATE_PUB);
			if(!pubDateElts.isEmpty())
			{	Element pubDateElt = pubDateElts.first();
				Element time = pubDateElt.getElementsByTag(HtmlNames.ELT_TIME).first();
				publishingDate = HtmlTools.getDateFromTimeElt(time,DATE_FORMAT);
				logger.log("Found the publishing date: "+publishingDate);
			}
			else
				logger.log("Did not find any publication date");
			Elements updtDateElts = articleElt.getElementsByAttributeValueContaining(HtmlNames.ATT_CLASS, CLASS_DATE_UPDT);
			if(!updtDateElts.isEmpty())
			{	Element updtDateElt = updtDateElts.first();
				Element time = updtDateElt.getElementsByTag(HtmlNames.ELT_TIME).first();
				modificationDate = HtmlTools.getDateFromTimeElt(time,DATE_FORMAT);
				logger.log("Found the last modification date: "+modificationDate);
			}
			else
				logger.log("Did not find any last modification date");
	
			// retrieve the authors
			Elements authorElts = articleElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_AUTHOR);
			if(!authorElts.isEmpty())
			{	Element authorElt = authorElts.first();
				Elements nameElts = authorElt.getElementsByAttributeValue(HtmlNames.ATT_ITEMPROP, AUTHOR_NAME);
				logger.log("List of the authors found for this article:");
					logger.increaseOffset();
					authors = new ArrayList<String>();
					for(Element nameElt: nameElts)
					{	String authorName = nameElt.text();
						authorName = removeGtst(authorName);
						logger.log(authorName);
						authors.add(authorName);
					}
				logger.decreaseOffset();
			}
			else
				logger.log("WARNING: could not find any author, which is unusual");
			
			// get the description
			Element descriptionElt = articleElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_DESCRIPTION).first();
			String text = descriptionElt.text() + "\n";
			text = removeGtst(text);
			rawStr.append(text);
			linkedStr.append(text);
		
			// processing the article main content
			Element contentElt = articleElt.getElementsByAttributeValue(HtmlNames.ATT_CLASS, CLASS_ARTICLE_BODY).first();
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
