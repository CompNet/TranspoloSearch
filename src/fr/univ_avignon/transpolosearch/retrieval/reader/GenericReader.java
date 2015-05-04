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
 * along with Nerwip - Named Entity Extraction in Wikipedia Pages.  
 * If not, see <http://www.gnu.org/licenses/>.
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
import fr.univ_avignon.transpolosearch.retrieval.reader.ArticleReader;
import fr.univ_avignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univ_avignon.transpolosearch.tools.file.FileNames;
import fr.univ_avignon.transpolosearch.tools.file.FileTools;
import fr.univ_avignon.transpolosearch.tools.xml.XmlNames;

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
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/**
	 * Retrieve the text located in a paragraph (P) HTML element.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 */
	private void processParagraphElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	// possibly add a new line character first
		if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)!='\n')
		{	rawStr.append("\n");
			linkedStr.append("\n");
		}
		
		// recursive processing
		processTextElement(element,rawStr,linkedStr);
		
		// possibly add a new line character
		if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)!='\n')
		{	rawStr.append("\n");
			linkedStr.append("\n");
		}
	}

	/**
	 * Retrieve the text located in a quote (BLOCKQUOTE) HTML element.
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
	private boolean processQuoteElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
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
		processTextElement(element,rawStr,linkedStr);

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
	 * Retrieve the text located in a span (SPAN) HTML element.
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
	private boolean processSpanElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(XmlNames.ATT_CLASS);
		
//		if(eltClass==null || 
//			// we don't need phonetic transcriptions, and they can mess up NER tools
//			(!eltClass.contains(CLASS_IPA)
//			// we also ignore WP buttons such as the "edit" links placed in certain section headers
//			&& !eltClass.contains(CLASS_EDIT)
//			// language indications
//			&& !eltClass.contains(CLASS_LANGUAGEICON)))
			
		{	result = true;
			// otherwise, we process what's inside the span tag
			processTextElement(element,rawStr,linkedStr);
		}
		
//		else
//			result = false;
		
		return result;
	}
	
	/**
	 * Retrieve the text located in a hyperlink (A) HTML element.
	 * <br/>
	 * We ignore links containing no text.
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
	private boolean processHyperlinkElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(XmlNames.ATT_CLASS);
		
//		if(eltClass==null)
		{	result = true;
			
			// simple text
			String str = element.text();
			if(!str.isEmpty())
			{	rawStr.append(str);
				
				// hyperlink
//				String eltTitle = element.attr(XmlNames.ATT_TITLE);
//				if((eltClass==null
//						|| (!eltClass.contains(CLASS_IMAGE) && !eltClass.contains(CLASS_EXTERNAL)))
//						&& (eltTitle==null	
//						|| (!eltTitle.contains(TITLE_LISTEN)))
//				)
				{	String href = element.attr(XmlNames.ATT_HREF);
					String code = "<" + XmlNames.ELT_A + " " +XmlNames.ATT_HREF + "=\"" + href + "\">" + str + "</" + XmlNames.ELT_A + ">";
					linkedStr.append(code);
				}
//				else
//					linkedStr.append(str);
			}
		}
		
//		else
//			result = false;
		
		return result;
	}
	
	/**
	 * Retrieve the text located in a list (UL or OL) HTML element.
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
	private void processListElement(Element element, StringBuilder rawStr, StringBuilder linkedStr, boolean ordered)
	{	// possibly remove the last new line character
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
			if(c=='\n')
			{	rawStr.deleteCharAt(rawStr.length()-1);
				linkedStr.deleteCharAt(linkedStr.length()-1);
			}
		}
		
		// possibly remove preceeding space
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
			if(c==' ')
			{	rawStr.deleteCharAt(rawStr.length()-1);
				linkedStr.deleteCharAt(linkedStr.length()-1);
			}
		}
		
		// possibly add a column
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
			if(c!='.' && c!=':' && c!=';')
			{	rawStr.append(":");
				linkedStr.append(":");
			}
		}
		
		// process each list element
		int count = 1;
		for(Element listElt: element.getElementsByTag(XmlNames.ELT_LI))
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
			processTextElement(listElt,rawStr,linkedStr);
			
			// possibly remove the last new line character
			if(rawStr.length()>0)
			{	char c = rawStr.charAt(rawStr.length()-1);
				if(c=='\n')
				{	rawStr.deleteCharAt(rawStr.length()-1);
					linkedStr.deleteCharAt(linkedStr.length()-1);
				}
			}
			
			// add final separator
			rawStr.append(";");
			linkedStr.append(";");
		}
		
		// possibly remove last separator
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
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
	}
	
	/**
	 * Retrieve the text located in a description list (DL) HTML element.
	 * 
	 * @param element
	 * 		Element to be processed.
	 * @param rawStr
	 * 		Current raw text string.
	 * @param linkedStr
	 * 		Current text with hyperlinks.
	 */
	private void processDescriptionListElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
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
			if(tempName.equals(XmlNames.ELT_DT))
			{	// process term
				processTextElement(tempElt,rawStr,linkedStr);
				
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
//			if(tempName.equals(XmlNames.ELT_DD))
			if(tempElt!=null)
			{	// process term
				processTextElement(tempElt,rawStr,linkedStr);
				
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
	 * Retrieve the text located in a division (DIV) HTML element.
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
	private boolean processDivisionElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(XmlNames.ATT_CLASS);
		
//if(eltClass.contains("thumb"))
//	System.out.print("");
		
//		if(eltClass==null || 
//			// we ignore infoboxes
//			(!eltClass.contains(CLASS_TABLEOFCONTENT)
//			// list of bibiliographic references located at the end of the page
//			&& !eltClass.contains(CLASS_REFERENCES)
//			// WP warning links (disambiguation and such)
//			&& !eltClass.contains(CLASS_DABLINK)
//			// related links
//			&& !eltClass.contains(CLASS_RELATEDLINK)
//			// audio or video clip
//			&& !eltClass.contains(CLASS_MEDIA)
//			// button used to magnify images
//			&& !eltClass.contains(CLASS_MAGNIFY)
//			// icons located at the top of the page
//			&& !eltClass.contains(CLASS_TOPICON)
//			))
		{	result = true;
			processTextElement(element, rawStr, linkedStr);
		}
		
//		else
//			result = false;
		
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
	private boolean processTableElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result;
		String eltClass = element.attr(XmlNames.ATT_CLASS);
		
//		if(eltClass==null || 
//			// we ignore infoboxes
//			(!eltClass.contains(CLASS_INFORMATIONBOX)
//			// and wikitables
//			&& !eltClass.contains(CLASS_WIKITABLE)
//			// navigation boxes
//			&& !eltClass.contains(CLASS_NAVIGATIONBOX)
//			// navigation boxes, WP warnings (incompleteness, etc.)
//			&& !eltClass.contains(CLASS_METADATA)
//			// personal data box (?)
//			&& !eltClass.contains(CLASS_PERSONDATA)))
			
		{	result = true;
			Element tbodyElt = element.children().get(0);
			
			for(Element rowElt: tbodyElt.children())
			{	for(Element colElt: rowElt.children())
				{	// process cell content
					processTextElement(colElt, rawStr, linkedStr);
					
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
		
//		else
//			result = false;
		
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
	private void processTextElement(Element textElement, StringBuilder rawStr, StringBuilder linkedStr)
	{	// we process each element contained in the specified text element
		for(Node node: textElement.childNodes())
		{	// element node
			if(node instanceof Element)
			{	Element element = (Element) node;
				String eltName = element.tag().getName();
				
				// section headers: same thing
				if(eltName.equals(XmlNames.ELT_H2) || eltName.equals(XmlNames.ELT_H3)
					|| eltName.equals(XmlNames.ELT_H4) || eltName.equals(XmlNames.ELT_H5) || eltName.equals(XmlNames.ELT_H6))
				{	processParagraphElement(element,rawStr,linkedStr);
				}
	
				// paragraphs inside paragraphs are processed recursively
				else if(eltName.equals(XmlNames.ELT_P))
				{	processParagraphElement(element,rawStr,linkedStr);
				}
				
				// superscripts are to be avoided
				else if(eltName.equals(XmlNames.ELT_SUP))
				{	// they are either external references or WP inline notes
					// cf. http://en.wikipedia.org/wiki/Template%3ACitation_needed
				}
				
				// small caps are placed before phonetic transcriptions of names, which we avoid
				else if(eltName.equals(XmlNames.ELT_SMALL))
				{	// we don't need them, and they can mess up NER tools
				}
				
				// we ignore certain types of span (phonetic trancription, WP buttons...) 
				else if(eltName.equals(XmlNames.ELT_SPAN))
				{	processSpanElement(element,rawStr,linkedStr);
				}
				
				// hyperlinks must be included in the linked string, provided they are not external
				else if(eltName.equals(XmlNames.ELT_A))
				{	processHyperlinkElement(element,rawStr,linkedStr);
				}
				
				// lists
				else if(eltName.equals(XmlNames.ELT_UL))
				{	processListElement(element,rawStr,linkedStr,false);
				}
				else if(eltName.equals(XmlNames.ELT_OL))
				{	processListElement(element,rawStr,linkedStr,true);
				}
				else if(eltName.equals(XmlNames.ELT_DL))
				{	processDescriptionListElement(element,rawStr,linkedStr);
				}
				
				// list item
				else if(eltName.equals(XmlNames.ELT_LI))
				{	processTextElement(element,rawStr,linkedStr);
				}
	
				// divisions are just processed recursively
				else if(eltName.equals(XmlNames.ELT_DIV))
				{	processDivisionElement(element,rawStr,linkedStr);
				}
				
				// quotes are just processed recursively
				else if(eltName.equals(XmlNames.ELT_BLOCKQUOTE))
				{	processQuoteElement(element,rawStr,linkedStr);
				}
				// citation
				else if(eltName.equals(XmlNames.ELT_CITE))
				{	processParagraphElement(element,rawStr,linkedStr);
				}
				
				// other elements are considered as simple text
				else
				{	String text = element.text();
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
				rawStr.append(text);
				linkedStr.append(text);
			}
		}
	}
	
	/**
	 * Pulls a text from the specified URL without images, tags, etc.
	 * 
	 * @param url
	 * 		Address of the targetted text.
	 * @return
	 * 		An Article object representing the retrieved text.
	 * 
	 * @throws ReaderException
	 * 		Problem while retrieving the text.
	 */
	@Override
	public Article read(URL url) throws ReaderException
	{	Article result = null;
		String name = getName(url);
		
		try
		{	// get the page
			String address = url.toString();
			logger.log("Retrieving page "+address);
			long startTime = System.currentTimeMillis();
			Document document  = retrieveSourceCode(name,url);
					
			// get its title
			Element titleElt = document.getElementsByTag(XmlNames.ELT_TITLE).get(0);
			String title = titleElt.text();
			logger.log("Get title: "+title);
			
			// identify the content element
			logger.log("Get the main element of the document");
			Element bodyElt = document.getElementsByTag(XmlNames.ELT_BODY).get(0);
			Element contentElt = getContentElement(bodyElt);

			// get raw and linked texts
			logger.log("Get raw and linked texts");
			StringBuilder rawStr = new StringBuilder();
			StringBuilder linkedStr = new StringBuilder();
			
			
			// processing each element in the content part
			for(Element element: contentElt.children())
			{	String eltName = element.tag().getName();
			
				// section headers
				if(eltName.equals(XmlNames.ELT_H2))
				{	// get section name
					StringBuilder fakeRaw = new StringBuilder();
					StringBuilder fakeLinked = new StringBuilder();
					processParagraphElement(element,fakeRaw,fakeLinked);
					String str = fakeRaw.toString().trim().toLowerCase(Locale.ENGLISH);
					rawStr.append("\n-----");
					linkedStr.append("\n-----");
					processParagraphElement(element,rawStr,linkedStr);
				}
			
				else
				{	// lower sections
					if(eltName.equals(XmlNames.ELT_H3) || eltName.equals(XmlNames.ELT_H4) 
						|| eltName.equals(XmlNames.ELT_H5) || eltName.equals(XmlNames.ELT_H6))
					{	processParagraphElement(element,rawStr,linkedStr);
					}
					
					// paragraph
					else if(eltName.equals(XmlNames.ELT_P))
					{	String str = element.text();
						processParagraphElement(element,rawStr,linkedStr);
					}
					
					// list
					else if(eltName.equals(XmlNames.ELT_UL))
					{	processListElement(element,rawStr,linkedStr,false);
					}
					else if(eltName.equals(XmlNames.ELT_OL))
					{	processListElement(element,rawStr,linkedStr,true);
					}
					else if(eltName.equals(XmlNames.ELT_DL))
					{	processDescriptionListElement(element,rawStr,linkedStr);
					}
					
					// tables
					else if(eltName.equals(XmlNames.ELT_TABLE))
					{	//TODO should we completely ignore tables?
						//first = !processTableElement(element, rawStr, linkedStr); 
					}
					
					// divisions
					else if(eltName.equals(XmlNames.ELT_DIV))
					{	processDivisionElement(element, rawStr, linkedStr);
					}
				
					// we ignore certain types of span (phonetic trancription, WP buttons...) 
					else if(eltName.equals(XmlNames.ELT_SPAN))
					{	processSpanElement(element,rawStr,linkedStr);
					}
					
					// hyperlinks must be included in the linked string, provided they are not external
					else if(eltName.equals(XmlNames.ELT_A))
					{	processHyperlinkElement(element,rawStr,linkedStr);
					}
					
					// quotes are just processed recursively
					else if(eltName.equals(XmlNames.ELT_BLOCKQUOTE))
					{	processQuoteElement(element,rawStr,linkedStr);
					}
					
					// other tags are ignored
					else
						logger.log("Ignored tag: "+eltName);
				}
			}
			
			// create article object
			result = new Article(name);
			result.setTitle(title);
			result.setUrl(url);
			result.initRetrievalDate();
			
			// clean text
			String rawText = rawStr.toString();
			rawText = cleanText(rawText);
//			rawText = ArticleCleaning.replaceChars(rawText);
			result.setRawText(rawText);
			logger.log("Length of the raw text: "+rawText.length()+" chars.");
			String linkedText = linkedStr.toString();
			linkedText = cleanText(linkedText);
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
	// CONTENT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Minimal proportion of the document (text-wise) we want to access */
	private static final float MIN_CONTENT_RATIO = 0.5f;
	
	/**
	 * Identifies which part of the document may be the main element, containing the most
	 * relevant content.
	 * <br/>
	 * The basic rule is the following. First, we try to locate an article element. If it exists,
	 * we just return it. If there are several of them, we issue a warning and return the first one.
	 * Other wise, we perform a breadth-first search and go deeper as long as we get shortest elements 
	 * (in terms of text content) while they still represent more than {@link #MIN_CONTENT_RATIO} 
	 * percent of their parent. We keep the parent whose children do not respect this rule. In other
	 * words, we keep the largest node whose content is split (roughly) evenly among its children. 
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
		Elements articleElts = root.getElementsByTag(XmlNames.ELT_ARTICLE);
		if(!articleElts.isEmpty())
		{	logger.log("Found an <article> element in this Web page >> using it as the main content element");
			result = articleElts.first();
			if(articleElts.size()>1)
				logger.log("WARNING: found several <article> elements in this Web page");
		}
		
		// no <article> element: use text size 
		else
		{	logger.log("No <article> element in this Web page >> using text size");
			
			// set up data structures
			Map<Element,Float> sizes = new HashMap<Element, Float>();
			sizes.put(root, totalLength);
			Queue<Element> queue = new LinkedList<Element>();
			queue.offer(root);
			
			do
			{	Element element = queue.poll();
				Elements children = element.children();
				float size = sizes.get(element);
				boolean candidate = false;
				
				// if the node has no children, it's a candidate
				if(children.isEmpty())
					candidate = true;
				
				// if it has one or more child
				else
				{	candidate = true;
					// it is a candidate only if none of them contains the majority of its text
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
//		Elements articleElts = root.getElementsByTag(XmlNames.ELT_ARTICLE);
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
