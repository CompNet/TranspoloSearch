package fr.univ_avignon.transpolosearch.retrieval.reader;

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
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import fr.univ_avignon.transpolosearch.data.article.Article;
import fr.univ_avignon.transpolosearch.data.article.ArticleLanguage;
import fr.univ_avignon.transpolosearch.retrieval.reader.ArticleReader;
import fr.univ_avignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univ_avignon.transpolosearch.tools.file.FileNames;
import fr.univ_avignon.transpolosearch.tools.file.FileTools;
import fr.univ_avignon.transpolosearch.tools.xml.HtmlNames;

/**
 * From a specified URL, this class retrieves a Wikipedia page,
 * and gives access to the raw and linked texts.
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings("unused")
public class GenericReader extends ArticleReader
{
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String getDomain()
	{	return "Generic reader";
	}

	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
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
			Element titleElt = document.getElementsByTag(HtmlNames.ELT_TITLE).get(0);
			String title = titleElt.text();
			title = removeGtst(title);
			logger.log("Get title: "+title);
			
			// identify the content element
			logger.log("Get the main element of the document");
			Element bodyElt = document.getElementsByTag(HtmlNames.ELT_BODY).get(0);
			Element contentElt = getContentElement(bodyElt);

			// get raw and linked texts
			logger.log("Get raw and linked texts");
			StringBuilder rawStr = new StringBuilder();
			StringBuilder linkedStr = new StringBuilder();
			
			// processing each element in the content part
			processAnyElement(contentElt, rawStr, linkedStr);
			
			// create article object
			result = new Article(name);
			result.setTitle(title);
			result.setUrl(url);
			result.initRetrievalDate();
			result.setLanguage(language);
			
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
		catch(ClientProtocolException e)
		{	e.printStackTrace();
		} 
		catch(ParseException e)
		{	e.printStackTrace();
		}
		catch(IOException e)
		{	e.printStackTrace();
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// CONTENT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Minimal proportion of the document (text-wise) we want to access */
	private static final float MIN_CONTENT_RATIO = 0.5f;
	/** List of tags which are not likely to contain the content element */
	private static final List<String> STOP_TAGS = Arrays.asList(
			HtmlNames.ELT_APPLET,HtmlNames.ELT_AREA,HtmlNames.ELT_ASIDE,HtmlNames.ELT_AUDIO,
			HtmlNames.ELT_BASE,HtmlNames.ELT_BASEFONT,HtmlNames.ELT_BDI,HtmlNames.ELT_BDO,
			HtmlNames.ELT_BUTTON,HtmlNames.ELT_CANVAS,HtmlNames.ELT_CODE,HtmlNames.ELT_CONTENT,
			HtmlNames.ELT_DATA,HtmlNames.ELT_DATALIST,HtmlNames.ELT_DECORATOR,HtmlNames.ELT_DEL,
			HtmlNames.ELT_DIALOG,HtmlNames.ELT_DIR,HtmlNames.ELT_ELEMENT,HtmlNames.ELT_EMBED,
			HtmlNames.ELT_FIELDSET,HtmlNames.ELT_FONT,HtmlNames.ELT_FORM,HtmlNames.ELT_FRAME,
			HtmlNames.ELT_FRAMESET,HtmlNames.ELT_IFRAME,HtmlNames.ELT_IMAGE,HtmlNames.ELT_INPUT,
			HtmlNames.ELT_ISINDEX,HtmlNames.ELT_KBD,HtmlNames.ELT_KEYGEN,HtmlNames.ELT_LABEL,
			HtmlNames.ELT_LEGEND,HtmlNames.ELT_LINK,HtmlNames.ELT_LISTING,HtmlNames.ELT_MAP,
			HtmlNames.ELT_MENU,HtmlNames.ELT_MENUITEM,HtmlNames.ELT_METER,HtmlNames.ELT_NAV,
			HtmlNames.ELT_OBJECT,HtmlNames.ELT_OPTGROUP,HtmlNames.ELT_OPTION,HtmlNames.ELT_OUTPUT,
			HtmlNames.ELT_PARAM,HtmlNames.ELT_PROGRESS,HtmlNames.ELT_RP,HtmlNames.ELT_RT,
			HtmlNames.ELT_RTC,HtmlNames.ELT_RUBY,HtmlNames.ELT_S,HtmlNames.ELT_SAMP,
			HtmlNames.ELT_SCRIPT,HtmlNames.ELT_SHADOW,HtmlNames.ELT_SELECT,HtmlNames.ELT_SOURCE,
			HtmlNames.ELT_STRIKE,HtmlNames.ELT_STYLE,HtmlNames.ELT_SUB,HtmlNames.ELT_SUP,
			HtmlNames.ELT_TEMPLATE,HtmlNames.ELT_TEXTAREA,HtmlNames.ELT_TRACK,HtmlNames.ELT_VIDEO,
			HtmlNames.ELT_WBR,HtmlNames.ELT_XMP
		);
	
	/**
	 * Identifies which part of the specified element may be the main element, containing the most
	 * relevant content.
	 * <br/>
	 * The basic rule is the following. First, we try to locate an article element. If it exists,
	 * we go on with it. If there are several of them, we issue a warning and use the first one.
	 * Otherwise, we use the root parameter. We perform a breadth-first search and go deeper as 
	 * long as we get shortest elements (in terms of text content) while they still represent more 
	 * than {@link #MIN_CONTENT_RATIO} percent of their parent. We keep the parent whose children 
	 * do not respect this rule. In other words, we keep the largest node whose content is split 
	 * (roughly) evenly among its children. 
	 * 
	 * @param root
	 * 		Root element.
	 * @return
	 * 		The elected subelement.
	 */
	private Element getContentElement(Element root)
	{	Element result = null;
		String totalText = root.text();
		float totalLength = totalText.length();
		logger.increaseOffset();
		logger.log("Total text length: "+totalLength+" characters");
		
		// presence of an <article> element in the page
		Elements articleElts = root.getElementsByTag(HtmlNames.ELT_ARTICLE);
		if(!articleElts.isEmpty())
		{	logger.log("Found an <article> element in this Web page >> using it as the main content element");
			root = articleElts.first();
			if(articleElts.size()>1)
				logger.log("WARNING: found several <article> elements in this Web page");
		}
		
		// now, use text size 
		{	//logger.log("No <article> element in this Web page >> using text size");
			
			// set up data structures
			Map<Element,Float> sizes = new HashMap<Element, Float>();
			sizes.put(root, totalLength);
			Queue<Element> queue = new LinkedList<Element>();
			queue.offer(root);
			
			do
			{	Element element = queue.poll();
				String name = element.tagName();
if(element.attr("id").equals("post-21996"))
	System.out.println();
				Elements children = element.children();
				float size = sizes.get(element);
				boolean candidate = false;
				
				// if the node is in the stop list, it's not a candidate
				if(STOP_TAGS.contains(name))
					candidate = false;
				
				// otherwise, if the node has no children, it's a candidate
				else if(children.isEmpty())
					candidate = true;
				
				// if it has one or more child
				else
				{	// a list is probably not a candidate (TODO other tags?)
					candidate = !name.equals(HtmlNames.ELT_UL) && !name.equals(HtmlNames.ELT_OL);
					// it is a candidate only if none child contains the majority of its text
					for(Element child: children)
					{	String text = child.text();
						float sz = text.length();
						sizes.put(child,sz);
						candidate = candidate && sz/size<MIN_CONTENT_RATIO;
					}
					// if not a candidate, put its children in the queue
					if(!candidate)
					{	for(Element child: children)
							queue.offer(child);
					}
				}
				
				// if we found a candidate
				if(candidate)
				{	// it's the new best candidate only if it's larger than the previous best candidate
					if(result!=null)
					{	float size0 = sizes.get(result);
						if(size>size0)
							result = element;
					}
					else
						result = element;
				}
			}
			while(!queue.isEmpty());
		}
		
		String text = result.text();
		float size = text.length();
		logger.log("Selected element: "+size+" characters ("+size/totalLength*100+"%)");
		logger.decreaseOffset();
		return result;
	}
//	private Element getContentElement0(Element root)
//	{	Element result = root;
//		String totalText = root.text();
//		float totalLength = totalText.length();
//		float currentLength = totalLength;
//		logger.increaseOffset();
//		logger.log("Total text length: "+totalLength+" characters");
//		
//		// presence of an <article> element in the page
//		Elements articleElts = root.getElementsByTag(HtmlNames.ELT_ARTICLE);
//		if(!articleElts.isEmpty())
//		{	logger.log("Found an <article> element in this Web page >> using it as the main content element");
//			result = articleElts.first();
//			if(articleElts.size()>1)
//				logger.log("WARNING: found several <article> elements in this Web page");
//		}
//		
//		// no <article> element: use text size 
//		else
//		{	logger.log("No <article> element in this Web page >> looking for the largest text chunk");
//			
//			// set up queue
//			Elements children = root.children();
//			Queue<Element> queue = new LinkedList<Element>(children);
//			
//			while(!queue.isEmpty())
//			{	Element element = queue.poll();
//				
//				// update current result
//				String text = element.text();
//				float length = text.length();
//				if(length<currentLength && (length/currentLength)>=MIN_CONTENT_RATIO)
//				{	// update result
//					result = element;
//					currentLength = length;
//					
//					// update queue
//					children = root.children();
//					for(Element child: children)
//						queue.offer(child);
//				}
//			}
//		}
//		
//		logger.log("Selected element: "+currentLength+" characters ("+currentLength/totalLength*100+"%)");
//		logger.decreaseOffset();
//		return result;
//	}
}
