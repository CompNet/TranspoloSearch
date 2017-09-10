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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
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
import fr.univavignon.transpolosearch.tools.string.StringTools;

/**
 * From a specified URL, this class retrieves a Wikipedia page,
 * and gives access to the raw and linked texts.
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings("unused")
public class WikipediaReader extends ArticleReader
{
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text allowing to detect the wikipedia domain */
	public static final String DOMAIN = "wikipedia.org";
	
	@Override
	public String getDomain()
	{	return DOMAIN;
	}

	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/** Id of the element containing the article content in the Wikipedia page */
	private final static String ID_CONTENT = "mw-content-text";
	/** Id of the element containing the article title in the Wikipedia page */
	private final static String ID_TITLE = "firstHeading";
	
	/** Class of phonetic transcriptions */
	private final static String CLASS_IPA = "IPA";
	/** Class of WP messages */
	private final static String CLASS_DABLINK = "dablink";
	/** Class of WP edit buttons */
	private final static String CLASS_EDIT = "editsection";
	/** Class of external hyperlinks of the Wikipedia page */
	private final static String CLASS_EXTERNAL = "external";
	/** Class of image hyperlinks */
	private final static String CLASS_IMAGE = "image";
	/** Class of the element containing the infobox of the Wikipedia page */
	private final static String CLASS_INFORMATIONBOX = "infobox";
	/** Class of the element containing some language-related information */
	private final static String CLASS_LANGUAGEICON = "languageicon";
	/** Class of zoom-in buttons */
	private final static String CLASS_MAGNIFY = "magnify";
	/** Class of the element containing some media material (audio, video...) */
	private final static String CLASS_MEDIA = "mediaContainer";
	/** Class of the element containing some metadata (e.g. wikimedia link) */
	private final static String CLASS_METADATA = "metadata";
	/** Class of the element containing navigation boxes */
	private final static String CLASS_NAVIGATIONBOX = "navbox";
	/** Class of the element containing personal data box (?) */
	private final static String CLASS_PERSONDATA = "persondata";
	/** Class of the element containing the list of references */
	private final static String CLASS_REFERENCES = "reflist";
	/** Class of the element containing a related link */
	private final static String CLASS_RELATEDLINK = "rellink";
	/** Class of the element containing the table of content */
	private final static String CLASS_TABLEOFCONTENT = "toc";
	/** Class used for certain pictures */
	private final static String CLASS_THUMB = "thumb";
	/** Class of icones located at the begining of pages */
	private final static String CLASS_TOPICON = "topicon";
	/** Class of the element containing some kind of generated table */
	private final static String CLASS_WIKITABLE = "wikitable";
	
	/** Title of audio links  */
	private final static String TITLE_LISTEN = "Listen";
	/** Disambiguation link */
	private final static String PARAGRAPH_FORTHE = "For the";

	/** List of sections to be ignored */
	private final static List<String> IGNORED_SECTIONS = Arrays.asList(
		"audio books", "audio recordings", /*"awards", "awards and honors",*/
		"bibliography", "books",
		"collections", "collections (selection)",
		"directed",
		"external links",
		"film adaptations", "film and television adaptations", "filmography", "footnotes", "further reading",
		"gallery",
//		"honours",
		"main writings",
		"notes", "nudes",
		"patents", "publications",
		"quotes",
		"references",
		"secondary bibliography", "see also", "selected bibliography", "selected filmography", "selected list of works", "selected works", "self-portraits", "sources", "stage adaptations",
		"texts of songs", "theme exhibitions", "theme exhibitions (selection)",
		"works"
	);

	/**
	 * Retrieve the text located in 
	 * a paragraph (P) HTML element.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 */
	@Override
	protected void processParagraphElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	// possibly add a new line character first
		if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)!='\n')
		{	rawStr.append("\n");
			linkedStr.append("\n");
		}
		
		// recursive processing
		processAnyElement(element,rawStr,linkedStr);
		
		// possibly add a new line character
		if(rawStr.charAt(rawStr.length()-1)!='\n')
		{	rawStr.append("\n");
			linkedStr.append("\n");
		}
	}

	/**
	 * Retrieve the text located in 
	 * a quote (BLOCKQUOTE) HTML element.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 * @return
	 * 		{@code true} iff the element was processed.
	 */
	@Override
	protected boolean processQuoteElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result = true;
		
		// possibly modify the previous characters 
		if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)=='\n')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
		}
		
		// insert quotes
		rawStr.append(" \"");
		linkedStr.append(" \"");
		
		// recursive processing
		int rawIdx = rawStr.length();
		int linkedIdx = linkedStr.length();
		processAnyElement(element,rawStr,linkedStr);

		// possibly remove characters added after quote marks
		while(rawStr.length()>rawIdx && 
			(rawStr.charAt(rawIdx)=='\n' || rawStr.charAt(rawIdx)==' '))
		{	rawStr.deleteCharAt(rawIdx);
			linkedStr.deleteCharAt(linkedIdx);
		}
		
		// possibly modify the ending characters 
		if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)=='\n')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
		}

		// insert quotes
		rawStr.append("\"");
		linkedStr.append("\"");
		
		return result;
	}
	
	/**
	 * Retrieve the text located in 
	 * a span (SPAN) HTML element.
	 * <br/>
	 * We process everything but
	 * the phonetic transcriptions.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 * @return
	 * 		{@code true} iff the element was processed.
	 */
	@Override
	protected boolean processSpanElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(HtmlNames.ATT_CLASS);
		
		if(eltClass==null || 
			// we don't need phonetic transcriptions, and they can mess up NER tools
			(!eltClass.contains(CLASS_IPA)
			// we also ignore WP buttons such as the "edit" links placed in certain section headers
			&& !eltClass.contains(CLASS_EDIT)
			// language indications
			&& !eltClass.contains(CLASS_LANGUAGEICON)))
			
		{	result = true;
			// otherwise, we process what's inside the span tag
			processAnyElement(element,rawStr,linkedStr);
		}
		
		else
			result = false;
		
		return result;
	}
	
	/**
	 * Retrieve the text located in 
	 * a hyperlink (A) HTML element.
	 * <br/>
	 * We ignore all external links,
	 * as well as linked images.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 * @return
	 * 		{@code true} iff the element was processed.
	 */
	@Override
	protected boolean processHyperlinkElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(HtmlNames.ATT_CLASS);
		
//		if(eltClass==null)
		{	result = true;
			
			// simple text
			String str = element.text();
			if(!str.isEmpty())
			{	str = removeGtst(str);
				rawStr.append(str);
			
//if(str.contains("Philadelphia, Pa."))	//debug stuff
//	System.out.print("");
				
				// hyperlink
				String eltTitle = element.attr(HtmlNames.ATT_TITLE);
				if((eltClass==null
						|| (!eltClass.contains(CLASS_IMAGE) && !eltClass.contains(CLASS_EXTERNAL)))
						&& (eltTitle==null	
						|| (!eltTitle.contains(TITLE_LISTEN)))
				)
				{	String href = element.attr(HtmlNames.ATT_HREF);
					String code = "<" + HtmlNames.ELT_A + " " +HtmlNames.ATT_HREF + "=\"" + href + "\">" + str + "</" + HtmlNames.ELT_A + ">";
					linkedStr.append(code);
				}
				else
					linkedStr.append(str);
			}
		}
		
//		else
//			result = false;
		
		return result;
	}
	
	/**
	 * Retrieve the text located in  list (UL or OL) HTML element.
	 * <br/>
	 * We try to linearize the list, in order to make it look like
	 * regular text. This is possible because list are used in a
	 * more "regular" way in Wikipedia than in random Web pages.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 * @param ordered
	 * 		Whether the list is numbered or not.
	 */
	@Override
	protected void processListElement(Element element, StringBuilder rawStr, StringBuilder linkedStr, boolean ordered)
	{	// possibly remove the last new line character
		char c = rawStr.charAt(rawStr.length()-1);
		if(c=='\n')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
		}
		
		// possibly remove preceeding space
		c = rawStr.charAt(rawStr.length()-1);
		if(c==' ')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
		}
		
		// possibly add a column
		c = rawStr.charAt(rawStr.length()-1);
		if(c!='.' && c!=':' && c!=';')
		{	rawStr.append(":");
			linkedStr.append(":");
		}
		
		// process each list element
		int count = 1;
		for(Element listElt: element.getElementsByTag(HtmlNames.ELT_LI))
		{	// add leading space
			rawStr.append(" ");
			linkedStr.append(" ");
			
			// possibly add number
			if(ordered)
			{	rawStr.append(count+") ");
				linkedStr.append(count+") ");
			}
			count++;
			
			// get text and links
			processAnyElement(listElt,rawStr,linkedStr);
			
			// possibly remove the last new line character
			c = rawStr.charAt(rawStr.length()-1);
			if(c=='\n')
			{	rawStr.deleteCharAt(rawStr.length()-1);
				linkedStr.deleteCharAt(linkedStr.length()-1);
			}
			
			// add final separator
			rawStr.append(";");
			linkedStr.append(";");
		}
		
		// possibly remove last separator
		c = rawStr.charAt(rawStr.length()-1);
		if(c==';')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
			c = rawStr.charAt(rawStr.length()-1);
			if(c!='.')
			{	rawStr.append(".");
				linkedStr.append(".");
			}
			rawStr.append("\n");
			linkedStr.append("\n");
		}
	}
	
	/**
	 * Retrieve the text located in a description list (DL) HTML element.
	 * <br/>
	 * We try to linearize the list, in order to make it look like
	 * regular text. This is possible because list are used in a
	 * more "regular" way in Wikipedia than in random Web pages.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 */
	@Override
	protected void processDescriptionListElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	// possibly remove the last new line character
		char c = rawStr.charAt(rawStr.length()-1);
		if(c=='\n')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
		}
		
		// possibly remove preceeding space
		c = rawStr.charAt(rawStr.length()-1);
		if(c==' ')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
		}
		
		// possibly add a column
		c = rawStr.charAt(rawStr.length()-1);
		if(c!='.' && c!=':' && c!=';')
		{	rawStr.append(":");
			linkedStr.append(":");
		}
		
		// process each list element
		Elements elements = element.children();
		Iterator<Element> it = elements.iterator();
		Element tempElt = null;
		if(it.hasNext())
			tempElt = it.next();
		while(tempElt!=null)
		{	// add leading space
			rawStr.append(" ");
			linkedStr.append(" ");
			
			// get term
			String tempName = tempElt.tagName();
			if(tempName.equals(HtmlNames.ELT_DT))
			{	// process term
				processAnyElement(tempElt,rawStr,linkedStr);
				
				// possibly remove the last new line character
				c = rawStr.charAt(rawStr.length()-1);
				if(c=='\n')
				{	rawStr.deleteCharAt(rawStr.length()-1);
					linkedStr.deleteCharAt(linkedStr.length()-1);
				}
				
				// possibly remove preceeding space
				c = rawStr.charAt(rawStr.length()-1);
				if(c==' ')
				{	rawStr.deleteCharAt(rawStr.length()-1);
					linkedStr.deleteCharAt(linkedStr.length()-1);
				}
				
				// possibly add a column and space
				c = rawStr.charAt(rawStr.length()-1);
				if(c!='.' && c!=':' && c!=';')
				{	rawStr.append(": ");
					linkedStr.append(": ");
				}
				
				// go to next element
				if(it.hasNext())
					tempElt = it.next();
				else
					tempElt = null;
			}
			
			// get definition
//			if(tempName.equals(HtmlNames.ELT_DD))
			if(tempElt!=null)
			{	// process term
				processAnyElement(tempElt,rawStr,linkedStr);
				
				// possibly remove the last new line character
				c = rawStr.charAt(rawStr.length()-1);
				if(c=='\n')
				{	rawStr.deleteCharAt(rawStr.length()-1);
					linkedStr.deleteCharAt(linkedStr.length()-1);
				}
				
				// possibly remove preceeding space
				c = rawStr.charAt(rawStr.length()-1);
				if(c==' ')
				{	rawStr.deleteCharAt(rawStr.length()-1);
					linkedStr.deleteCharAt(linkedStr.length()-1);
				}
				
				// possibly add a semi-column
				c = rawStr.charAt(rawStr.length()-1);
				if(c!='.' && c!=':' && c!=';')
				{	rawStr.append(";");
					linkedStr.append(";");
				}
				
				// go to next element
				if(it.hasNext())
					tempElt = it.next();
				else
					tempElt = null;
			}
		}
		
		// possibly remove last separator
		c = rawStr.charAt(rawStr.length()-1);
		if(c==';')
		{	rawStr.deleteCharAt(rawStr.length()-1);
			linkedStr.deleteCharAt(linkedStr.length()-1);
			c = rawStr.charAt(rawStr.length()-1);
			if(c!='.')
			{	rawStr.append(".");
				linkedStr.append(".");
			}
			rawStr.append("\n");
			linkedStr.append("\n");
		}
	}
	
	/**
	 * Retrieve the text located in 
	 * a division (DIV) HTML element.
	 * <br/>
	 * We ignore some of them: table
	 * of content, reference list, related links, etc.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 * @return
	 * 		{@code true} iff the element was processed.
	 */
	@Override
	protected boolean processDivisionElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(HtmlNames.ATT_CLASS);
		
//if(eltClass.contains("thumb"))
//	System.out.print("");
		
		if(eltClass==null || 
			// we ignore infoboxes
			(!eltClass.contains(CLASS_TABLEOFCONTENT)
			// list of bibiliographic references located at the end of the page
			&& !eltClass.contains(CLASS_REFERENCES)
			// WP warning links (disambiguation and such)
			&& !eltClass.contains(CLASS_DABLINK)
			// related links
			&& !eltClass.contains(CLASS_RELATEDLINK)
			// audio or video clip
			&& !eltClass.contains(CLASS_MEDIA)
			// button used to magnify images
			&& !eltClass.contains(CLASS_MAGNIFY)
			// icons located at the top of the page
			&& !eltClass.contains(CLASS_TOPICON)
			))
		{	result = true;
			processAnyElement(element, rawStr, linkedStr);
		}
		
		else
			result = false;
		
		return result;
	}
	
	/**
	 * Retrieve the text located in a table (TABLE) HTML element.
	 * <br/>
	 * We process each cell in the table as a text element. 
	 * Some tables are ignored: infoboxes, wikitables, navboxes,
	 * metadata, persondata, etc. 
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 * @return
	 * 		{@code true} iff the element was processed.
	 */
	@Override
	protected boolean processTableElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(HtmlNames.ATT_CLASS);
		
		if(eltClass==null || 
			// we ignore infoboxes
			(!eltClass.contains(CLASS_INFORMATIONBOX)
			// and wikitables
			&& !eltClass.contains(CLASS_WIKITABLE)
			// navigation boxes
			&& !eltClass.contains(CLASS_NAVIGATIONBOX)
			// navigation boxes, WP warnings (incompleteness, etc.)
			&& !eltClass.contains(CLASS_METADATA)
			// personal data box (?)
			&& !eltClass.contains(CLASS_PERSONDATA)))
			
		{	result = true;
			Element tbodyElt = element.children().get(0);
			
			for(Element rowElt: tbodyElt.children())
			{	for(Element colElt: rowElt.children())
				{	// process cell content
					processAnyElement(colElt, rawStr, linkedStr);
					
					// possibly add final dot and space. 
					if(rawStr.charAt(rawStr.length()-1)!=' ')
					{	if(rawStr.charAt(rawStr.length()-1)=='.')
						{	rawStr.append(" ");
							linkedStr.append(" ");
						}
						else
						{	rawStr.append(". ");
							linkedStr.append(". ");
						}
					}
				}
			}
		}
		
		else
			result = false;
		
		return result;
	}
	
	/**
	 * Extract text and hyperlinks from an element
	 * supposingly containing only text.
	 * 
	 * @param textElement
	 * 		The element to be processed.
	 * @param rawStr
	 * 		The StringBuffer to contain the raw text.
	 * @param linkedStr
	 * 		The StringBuffer to contain the text with hyperlinks.
	 */
	@Override
	protected void processAnyElement(Element textElement, StringBuilder rawStr, StringBuilder linkedStr)
	{	// we process each element contained in the specified text element
		for(Node node: textElement.childNodes())
		{	// element node
			if(node instanceof Element)
			{	Element element = (Element) node;
				String eltName = element.tag().getName();
				
				// section headers: same thing
				if(eltName.equals(HtmlNames.ELT_H2) || eltName.equals(HtmlNames.ELT_H3)
					|| eltName.equals(HtmlNames.ELT_H4) || eltName.equals(HtmlNames.ELT_H5) || eltName.equals(HtmlNames.ELT_H6))
				{	processParagraphElement(element,rawStr,linkedStr);
				}
	
				// paragraphs inside paragraphs are processed recursively
				else if(eltName.equals(HtmlNames.ELT_P))
				{	processParagraphElement(element,rawStr,linkedStr);
				}
				
				// superscripts are to be avoided
				else if(eltName.equals(HtmlNames.ELT_SUP))
				{	// they are either external references or WP inline notes
					// cf. http://en.wikipedia.org/wiki/Template%3ACitation_needed
				}
				
				// small caps are placed before phonetic transcriptions of names, which we avoid
				else if(eltName.equals(HtmlNames.ELT_SMALL))
				{	// we don't need them, and they can mess up NER tools
				}
				
				// we ignore certain types of span (phonetic trancription, WP buttons...) 
				else if(eltName.equals(HtmlNames.ELT_SPAN))
				{	processSpanElement(element,rawStr,linkedStr);
				}
				
				// hyperlinks must be included in the linked string, provided they are not external
				else if(eltName.equals(HtmlNames.ELT_A))
				{	processHyperlinkElement(element,rawStr,linkedStr);
				}
				
				// lists
				else if(eltName.equals(HtmlNames.ELT_UL))
				{	processListElement(element,rawStr,linkedStr,false);
				}
				else if(eltName.equals(HtmlNames.ELT_OL))
				{	processListElement(element,rawStr,linkedStr,true);
				}
				else if(eltName.equals(HtmlNames.ELT_DL))
				{	processDescriptionListElement(element,rawStr,linkedStr);
				}
				
				// list item
				else if(eltName.equals(HtmlNames.ELT_LI))
				{	processAnyElement(element,rawStr,linkedStr);
				}
	
				// divisions are just processed recursively
				else if(eltName.equals(HtmlNames.ELT_DIV))
				{	processDivisionElement(element,rawStr,linkedStr);
				}
				
				// quotes are just processed recursively
				else if(eltName.equals(HtmlNames.ELT_BLOCKQUOTE))
				{	processQuoteElement(element,rawStr,linkedStr);
				}
				// citation
				else if(eltName.equals(HtmlNames.ELT_CITE))
				{	processParagraphElement(element,rawStr,linkedStr);
				}
				
				// other elements are considered as simple text
				else
				{	String text = element.text();
					text = removeGtst(text);
					rawStr.append(text);
					linkedStr.append(text);
				}
			}
			
			// text node
			else if(node instanceof TextNode)
			{	// get the text
				TextNode textNode = (TextNode) node;
				String text = textNode.text();
				// if at the begining of a new line, or already preceeded by a space, remove leading spaces
				while(rawStr.length()>0 
						&& (rawStr.charAt(rawStr.length()-1)=='\n' || rawStr.charAt(rawStr.length()-1)==' ') 
						&& text.startsWith(" "))
					text = text.substring(1);
				// complete string buffers
				text = removeGtst(text);
				rawStr.append(text);
				linkedStr.append(text);
			}
		}
	}
	
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
			Element firstHeadingElt = document.getElementsByAttributeValue(HtmlNames.ATT_ID,ID_TITLE).get(0);
			String title = firstHeadingElt.text();
			title = removeGtst(title);
			title = title.replace("\"","'");
			title = title.replaceAll("[\\s]+"," ");
			logger.log("Get title: "+title);
			
			// get raw and linked texts
			logger.log("Get raw and linked texts.");
			StringBuilder rawStr = new StringBuilder();
			StringBuilder linkedStr = new StringBuilder();
			Element bodyContentElt = document.getElementsByAttributeValue(HtmlNames.ATT_ID,ID_CONTENT).get(0);
			// processing each element in the content part
			boolean ignoringSection = false;
			boolean first = true;
			for(Element element: bodyContentElt.children())
			{	String eltName = element.tag().getName();
				String eltClass = element.attr(HtmlNames.ATT_CLASS);
			
				// section headers
				if(eltName.equals(HtmlNames.ELT_H2))
				{	first = false;
					// get section name
					StringBuilder fakeRaw = new StringBuilder();
					StringBuilder fakeLinked = new StringBuilder();
					processParagraphElement(element,fakeRaw,fakeLinked);
					String str = fakeRaw.toString().trim().toLowerCase(Locale.ENGLISH);
					// check section name
					if(IGNORED_SECTIONS.contains(str))
						ignoringSection = true;
					else
					{	ignoringSection = false;
						rawStr.append("\n-----");
						linkedStr.append("\n-----");
						processParagraphElement(element,rawStr,linkedStr);
					}
				}
			
				else if(!ignoringSection)
				{	// lower sections
					if(eltName.equals(HtmlNames.ELT_H3) || eltName.equals(HtmlNames.ELT_H4) 
						|| eltName.equals(HtmlNames.ELT_H5) || eltName.equals(HtmlNames.ELT_H6))
					{	first = false;
						processParagraphElement(element,rawStr,linkedStr);
					}
					
					// paragraph
					else if(eltName.equals(HtmlNames.ELT_P))
					{	String str = element.text();
						// ignore possible initial disambiguation link
						if(!first || !str.startsWith(PARAGRAPH_FORTHE))	 
						{	first = false;
							processParagraphElement(element,rawStr,linkedStr);
						}
					}
					
					// list
					else if(eltName.equals(HtmlNames.ELT_UL))
					{	first = false;
						processListElement(element,rawStr,linkedStr,false);
					}
					else if(eltName.equals(HtmlNames.ELT_OL))
					{	first = false;
						processListElement(element,rawStr,linkedStr,true);
					}
					else if(eltName.equals(HtmlNames.ELT_DL))
					{	first = false;
						processDescriptionListElement(element,rawStr,linkedStr);
					}
					
					// tables
					else if(eltName.equals(HtmlNames.ELT_TABLE))
					{	first = !processTableElement(element, rawStr, linkedStr);
					}
					
					// divisions
					else if(eltName.equals(HtmlNames.ELT_DIV))
					{	// ignore possible initial picture 
						if(!first || eltClass==null || !eltClass.contains(CLASS_THUMB))
							first = !processDivisionElement(element, rawStr, linkedStr);
					}
				
					// we ignore certain types of span (phonetic trancription, WP buttons...) 
					else if(eltName.equals(HtmlNames.ELT_SPAN))
					{	first = !processSpanElement(element,rawStr,linkedStr);
					}
					
					// hyperlinks must be included in the linked string, provided they are not external
					else if(eltName.equals(HtmlNames.ELT_A))
					{	first = !processHyperlinkElement(element,rawStr,linkedStr);
					}
					
					// quotes are just processed recursively
					else if(eltName.equals(HtmlNames.ELT_BLOCKQUOTE))
					{	first = !processQuoteElement(element,rawStr,linkedStr);
					}
					
					// other tags are ignored
				}
			}
			
			// create article object
			result = new Article(name);
			result.setTitle(title);
			result.setUrl(url);
			result.initRetrievalDate();
			
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
}
