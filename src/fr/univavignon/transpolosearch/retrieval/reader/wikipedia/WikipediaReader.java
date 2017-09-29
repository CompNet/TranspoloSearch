package fr.univavignon.transpolosearch.retrieval.reader.wikipedia;

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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.retrieval.reader.ArticleReader;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.tools.html.HtmlNames;
import fr.univavignon.transpolosearch.tools.string.StringTools;

/**
 * From a specified URL, this class retrieves a Wikipedia page,
 * and gives access to the raw and linked texts.
 * 
 * @author Vincent Labatut
 */
public class WikipediaReader extends ArticleReader
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
//		URL url = new URL("https://fr.wikipedia.org/wiki/Boeing_767");
//		URL url = new URL("https://fr.wikipedia.org/wiki/Appareil_informatique");
//		URL url = new URL("https://fr.wikipedia.org/wiki/Apache_Software_Foundation");
		URL url = new URL("https://fr.wikipedia.org/wiki/Transport_Layer_Security");
		
		ArticleReader reader = new WikipediaReader();
		reader.setCacheEnabled(false);
		Article article = reader.processUrl(url, ArticleLanguage.FR);
		article.cleanContent();
		System.out.println(article);
		article.write();
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String getName(URL url)
	{	String address = url.toString();
		
//		// get the last part of the URL as the page name
//		String temp[] = address.split("/");
//		String result = temp[temp.length-1];
//		// doesn't work if there's "/" in the name, e.g. "MPEG-1/2_Audio_Layer_III"
		
		String prfx = "wiki/";
		int pos = address.indexOf(prfx);
		pos = pos + prfx.length();
		String result = address.substring(pos);
		
		// remove diacritics
		result = StringTools.removeDiacritics(result);
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text allowing to detect the Wikipedia domain */
	public static final String DOMAIN = "wikipedia.org";
	
	@Override
	public String getDomain()
	{	return DOMAIN;
	}
	
	/////////////////////////////////////////////////////////////////
	// LANGUAGE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Language of the page currently processed */
	private ArticleLanguage language;
	
	/////////////////////////////////////////////////////////////////
	// RETRIEVE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////	
	/** Id of the element containing the article content in the Wikipedia page */
	private final static String ID_CONTENT = "mw-content-text";
	/** Id of the element containing the article title in the Wikipedia page */
	private final static String ID_TITLE = "firstHeading";
	
	/** Class of main content */
	private final static String CLASS_CONTENT = "mw-parser-output";
	/** Class of announcements */
	private final static String[] CLASS_ANNOUNCEMENTS = {"hatnote","bandeau"};
	/** Class of phonetic transcriptions */
	private final static String[] CLASS_IPA = {"IPA","API"};
	/** Class of disambiguisation links */
	private final static String[] CLASS_DISAMB_LINK = {"plainlinks","homonymie"};
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
	private final static String[] CLASS_REFERENCES = {"reflist","references"};
	/** Class of the element containing a related link */
	private final static String CLASS_RELATEDLINK = "rellink"; //TODO seems obsolete
	/** Class of the element containing the table of content */
	private final static String CLASS_TABLEOFCONTENT = "toc";
	/** Class used for certain pictures */
	private final static String CLASS_THUMB = "thumb";
	/** Class of icones located at the begining of pages */
	private final static String CLASS_TOPICON = "topicon";
	/** Class of the element containing some kind of generated table */
	private final static String CLASS_WIKITABLE = "wikitable";
	/** Class of boxes referring to other Wikimedia projects */
	private final static String CLASS_INTERPROJECTS = "js-interprojets";
	
	/** Title of audio links  */
	private final static String TITLE_LISTEN = "Listen";
	/** Disambiguation link */
	private final static String PARAGRAPH_FORTHE = "For the";

	/** List of sections to be ignored */
	private final static List<List<String>> IGNORED_SECTIONS = Arrays.asList(
		Arrays.asList("audio books", "audio recordings", /*"awards", "awards and honors",*/
			"bibliography", "books",
			"collections", "collections (selection)",
			"directed",
			"external links",
			"film adaptations", "film and television adaptations", "filmography", "footnotes", "further reading",
			"gallery",
//			"honours",
			"main writings",
			"notes", "nudes",
			"patents", "publications",
			"quotes",
			"references",
			"secondary bibliography", "see also", "selected bibliography", "selected filmography", "selected list of works", "selected works", "self-portraits", "sources", "stage adaptations",
			"texts of songs", "theme exhibitions", "theme exhibitions (selection)",
			"works"),
		Arrays.asList(
			"annexes",
			"bibliographie",	// keep when dealing with biographies
			"notes et références","références","notes",
			"liens externes", "lien externe",
//			"voir aussi"		// keep when extracting networks of WP pages
			"source"
	));
	
	/////////////////////////////////////////////////////////////////
	// QUOTES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
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
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
			if(c=='\n')
			{	rawStr.deleteCharAt(rawStr.length()-1);
				linkedStr.deleteCharAt(linkedStr.length()-1);
			}
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
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
			if(c=='\n')
			{	rawStr.deleteCharAt(rawStr.length()-1);
				linkedStr.deleteCharAt(linkedStr.length()-1);
			}
		}

		// insert quotes
		rawStr.append("\"");
		linkedStr.append("\"");
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// HYPERLINKS		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Retrieve the text located in a hyperlink (A) HTML element.
	 * <br/>
	 * We ignore all external links, as well as linked images.
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
	
	/////////////////////////////////////////////////////////////////
	// LISTS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Whether to extract the textual content of lists, or not */
	private boolean getLists = true;
	
	/**
	 * Determines whether the reader will extract the text content
	 * of lists present in the processed article, or not. By default,
	 * the lists are extracted.
	 * 
	 * @param getLists
	 * 		{@code true} to extract the lists.
	 */
	public void setGetLists(boolean getLists)
	{	this.getLists = getLists;
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
	{	if(getLists)
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
	{	if(getLists)
		{	// possibly remove the last new line character
			if(rawStr.length()>0)
			{	char c = rawStr.charAt(rawStr.length()-1);
				if(c=='\n')
				{	rawStr.deleteCharAt(rawStr.length()-1);
					linkedStr.deleteCharAt(linkedStr.length()-1);
				}
			}
			
			// possibly remove the preceding space
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
					if(rawStr.length()>0)
					{	char c = rawStr.charAt(rawStr.length()-1);
						if(c=='\n')
						{	rawStr.deleteCharAt(rawStr.length()-1);
							linkedStr.deleteCharAt(linkedStr.length()-1);
						}
					}
					
					// possibly remove preceding space
					if(rawStr.length()>0)
					{	char c = rawStr.charAt(rawStr.length()-1);
						if(c==' ')
						{	rawStr.deleteCharAt(rawStr.length()-1);
							linkedStr.deleteCharAt(linkedStr.length()-1);
						}
					}
					
					// possibly add a column and space
					if(rawStr.length()>0)
					{	char c = rawStr.charAt(rawStr.length()-1);
						if(c!='.' && c!=':' && c!=';')
						{	rawStr.append(": ");
							linkedStr.append(": ");
						}
					}
					
					// go to next element
					if(it.hasNext())
						tempElt = it.next();
					else
						tempElt = null;
				}
				
				// get definition
//				if(tempName.equals(HtmlNames.ELT_DD))
				if(tempElt!=null)
				{	// process term
					processAnyElement(tempElt,rawStr,linkedStr);
					
					// possibly remove the last new line character
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
					
					// possibly add a semi-column
					if(rawStr.length()>0)
					{	char c = rawStr.charAt(rawStr.length()-1);
						if(c!='.' && c!=':' && c!=';')
						{	rawStr.append(";");
							linkedStr.append(";");
						}
					}
					
					// go to next element
					if(it.hasNext())
						tempElt = it.next();
					else
						tempElt = null;
				}
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
	}
	
	/////////////////////////////////////////////////////////////////
	// DIVIDERS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
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
			// we don't need phonetic transcriptions, and they can mess up recognizers
			(!eltClass.contains(CLASS_IPA[language.ordinal()])
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
		(	// we ignore tables of content
			!eltClass.contains(CLASS_TABLEOFCONTENT)
			// list of bibiliographic references located at the end of the page
			&& !eltClass.contains(CLASS_REFERENCES[language.ordinal()])
			// WP warning links (disambiguation and such)
			&& !eltClass.contains(CLASS_DISAMB_LINK[language.ordinal()])
			// related links
			&& !eltClass.contains(CLASS_RELATEDLINK)
			// audio or video clip
			&& !eltClass.contains(CLASS_MEDIA)
			// button used to magnify images
			&& !eltClass.contains(CLASS_MAGNIFY)
			// icons located at the top of the page
			&& !eltClass.contains(CLASS_TOPICON)
			// announcements at the top of the page
			&& !eltClass.contains(CLASS_ANNOUNCEMENTS[language.ordinal()])
			// link to other wikimedia projects
			&& !eltClass.contains(CLASS_INTERPROJECTS)
			// navigation boxes
			&& !eltClass.contains(CLASS_NAVIGATIONBOX)
			// information boxes
			&& !eltClass.contains(CLASS_INFORMATIONBOX)
		))
		{	result = true;
			processAnyElement(element, rawStr, linkedStr);
		}
		
		else
			result = false;
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// TABLES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Whether to extract the textual content of tables, or not */
	private boolean getTables = true;
	
	/**
	 * Determines whether the reader will extract the text content
	 * of table present in the processed article, or not. By default,
	 * the tables are extracted.
	 * 
	 * @param getTables
	 * 		{@code true} to extract the tables.
	 */
	public void setGetTables(boolean getTables)
	{	this.getTables = getTables;
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
	{	boolean result = false;
		
		if(getTables)
		{	String eltClass = element.attr(HtmlNames.ATT_CLASS);
			
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
				
			{	Elements children = element.children();
				if(!children.isEmpty())
				{	result = true;
					Element tbodyElt = children.first();
					
					for(Element rowElt: tbodyElt.children())
					{	for(Element colElt: rowElt.children())
						{	// process cell content
							processAnyElement(colElt, rawStr, linkedStr);
							
							// possibly add final dot and space. 
							if(rawStr.length()>0)
							{	char c = rawStr.charAt(rawStr.length()-1);
								if(c!=' ')
								{	if(c=='.')
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
				}
			}
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// MISC				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Whether to extract the sections, or not */
	private boolean getSections = true;
	
	/**
	 * Determines whether the reader will extract the sections
	 * present in the processed article, or not. By default,
	 * the sections are extracted.
	 * 
	 * @param getSections
	 * 		{@code true} to extract the sections.
	 */
	public void setGetSections(boolean getSections)
	{	this.getSections = getSections;
	}
	
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
		if(rawStr.length()>0)
		{	char c =  rawStr.charAt(rawStr.length()-1);
			if(c!='\n')
			{	rawStr.append("\n");
				linkedStr.append("\n");
			}
		}
		
		// recursive processing
		processAnyElement(element,rawStr,linkedStr);
		
		// possibly add a new line character
		if(rawStr.length()>0)
		{	char c = rawStr.charAt(rawStr.length()-1);
			if(c!='\n')
			{	rawStr.append("\n");
				linkedStr.append("\n");
			}
		}
	}
	
	/**
	 * Extract text and hyperlinks from an element
	 * supposedly containing only text.
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
				{	if(getSections)
						processParagraphElement(element,rawStr,linkedStr);
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
				{	// we don't need them, and they can mess up recognizers
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
				// if at the beginning of a new line, or already preceded by a space, remove leading spaces
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
	
	/////////////////////////////////////////////////////////////////
	// PROCESS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public Article processUrl(URL url, ArticleLanguage language) throws ReaderException
	{	Article result = null;
		this.language = language;
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
			logger.log("Get title: "+title);
			
			// get raw and linked texts
			logger.log("Get raw and linked texts.");
			StringBuilder rawStr = new StringBuilder();
			StringBuilder linkedStr = new StringBuilder();
			Element mainContentElt = document.getElementsByAttributeValue(HtmlNames.ATT_ID,ID_CONTENT).first();
			Element bodyContentElt = mainContentElt.getElementsByClass(CLASS_CONTENT).first();
			// processing each element in the content part
			int ignoringSectionLevel = HtmlNames.ELT_HS.size();	// indicates whether the current section should be ignored
			boolean first = true;
			for(Element element: bodyContentElt.children())
			{	String eltName = element.tag().getName();
				String eltClass = element.attr(HtmlNames.ATT_CLASS);
			
				// section headers
				if(HtmlNames.ELT_HS.contains(eltName))
				{	first = false;
					// get section name
					StringBuilder fakeRaw = new StringBuilder();
					StringBuilder fakeLinked = new StringBuilder();
					processParagraphElement(element,fakeRaw,fakeLinked);
					String str = fakeRaw.toString().trim().toLowerCase(Locale.ENGLISH);
					// check section name
					int level = HtmlNames.ELT_HS.indexOf(eltName);
					if(IGNORED_SECTIONS.get(language.ordinal()).contains(str))
						ignoringSectionLevel = Math.min(ignoringSectionLevel,level);
					else
					{	if(level<=ignoringSectionLevel)
						{	ignoringSectionLevel = HtmlNames.ELT_HS.size();
							rawStr.append("\n");
							linkedStr.append("\n");
							processParagraphElement(element,rawStr,linkedStr);
						}
					}
				}
				
				else if(ignoringSectionLevel==HtmlNames.ELT_HS.size())
				{	// paragraph
					if(eltName.equals(HtmlNames.ELT_P))
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
