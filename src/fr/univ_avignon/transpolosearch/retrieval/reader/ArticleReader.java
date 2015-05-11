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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import fr.univ_avignon.transpolosearch.data.article.Article;
import fr.univ_avignon.transpolosearch.data.article.ArticleLanguage;
import fr.univ_avignon.transpolosearch.tools.file.FileNames;
import fr.univ_avignon.transpolosearch.tools.file.FileTools;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univ_avignon.transpolosearch.tools.string.StringTools;
import fr.univ_avignon.transpolosearch.tools.xml.XmlNames;

/**
 * All classes automatically getting articles
 * from the Web using a starting name or URL 
 * should inherit from this abstract class.
 * 
 * TODO list of readers to implement:
 * 	- les échos
 *  - le figaro
 *  - la provence
 *  - médiapart
 * l'approche générique ne fonctionne pas si la page contient plusieurs articles
 * 
 * @author Vincent Labatut
 */
public abstract class ArticleReader
{	
	/////////////////////////////////////////////////////////////////
	// FACTORY		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Builds the appropriate reader to handle the specified
	 * Web address, then returns it.
	 *  
	 * @param url
	 * 		The Web address to process. 
	 * @return
	 * 		An appropriate reader for the specified address.
	 */
	public static ArticleReader buildReader(String url)
	{	ArticleReader result;
		
		if(url.contains(WikipediaReader.DOMAIN))
			result = new WikipediaReader();
		else if(url.contains(LeMondeReader.DOMAIN))
			result = new LeMondeReader();
		else if(url.contains(LiberationReader.DOMAIN))
			result = new LiberationReader();
		else
			result = new GenericReader();
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	protected HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();

	/////////////////////////////////////////////////////////////////
	// CACHE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Whether or not original source code should be cached localy */
	protected boolean cache = true;
	
	/**
	 * Switches the cache flag.
	 * 
	 * @param enabled
	 * 		{@code true} to enable caching.
	 */
	public void setCacheEnabled(boolean enabled)
	{	this.cache = enabled;
	}

	/////////////////////////////////////////////////////////////////
	// MISC				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Processes the name of the article
	 * from the specified URL.
	 * 
	 * @param url
	 * 		URL of the article.
	 * @return
	 * 		Name of the article.
	 */
	public String getName(URL url)
	{	String address = url.toString();
		
		// convert the full URL to a file-compatible name
		String result = null;
		try 
		{	result = URLEncoder.encode(address,"UTF-8");
			// reverse the transformation :
			// String original = URLDecoder.decode(result, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{	e.printStackTrace();
		}
		
		// alternative : generate a random name (not reproducible, though)
//		UUID.randomUUID();

		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// DOMAIN			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the Web domain handled
	 * by this reader.
	 * 
	 * @return
	 * 		A string representing the Web domain.
	 */
	public abstract String getDomain();
	
	/////////////////////////////////////////////////////////////////
	// CLEANING			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Cleans the specified string, in order to remove characters
	 * causing problems when detecting named entities.
	 *    
	 * @param input
	 * 		The string to process.
	 * @return
	 * 		Cleaned string.
	 */
	protected String cleanText(String input)
	{	String output = input.trim();
		
		String previous = output;
		do
		{	previous = output;
		
			// move punctuation out of hyperlinks
			String punctuation = "[ \\n\\.,;]";
			output = output.replaceAll("<a ([^>]*?)>("+punctuation+"*)([^<]*?)("+punctuation+"*)</a>","$2<a $1>$3</a>$4");
			output = output.replaceAll("<a ([^>]*?)>(\\()([^<]*?)(\\))</a>","$2<a $1>$3</a>$4");
			output = output.replaceAll("<a ([^>]*?)>(\\[)([^<]*?)(\\])</a>","$2<a $1>$3</a>$4");
			
			// replace multiple consecutive spaces by a single one 
			output = output.replaceAll("( )+", " ");
			
			// replace multiple consecutive newlines by a single one 
			output = output.replaceAll("(\\n)+", "\n");
			
			// replace multiple space-separated punctuations by single ones 
//			output = output.replaceAll("; ;", ";");
//			output = output.replaceAll(", ,", ",");
//			output = output.replaceAll(": :", ":");
//			output = output.replaceAll("\\. \\.", "\\.");
			
			// replace multiple consecutive punctuation marks by a single one 
			output = output.replaceAll("([\\.,;:] )[\\.,;:]", "$1");
	
			// remove spaces before dots 
			output = output.replaceAll(" \\.", ".");
			
			// remove space after opening parenthesis
			output = output.replaceAll("\\( +", "(");
			// remove space before closing parenthesis
			output = output.replaceAll(" +\\)", ")");
			
			// remove various combinations of punctuation marks
			output = output.replaceAll("\\(;", "(");
	
			// remove empty square brackets and parentheses
			output = output.replaceAll("\\[\\]", "");
			output = output.replaceAll("\\(\\)", "");
			
			// adds final dot when it is missing at the end of a sentence (itself detected thanks to the new line)
			output = output.replaceAll("([^(\\.|\\-)])\\n", "$1.\n");
			
			// insert a space after coma, when missing
			output = output.replaceAll(",([^ _])", ", $1");
	
			// insert a space after semi-column, when missing
			output = output.replaceAll(";([^ _])", "; $1");
			
			// replace 2 single quotes by double quotes
			output = output.replaceAll("''+", "\"");
		}
		while(!output.equals(previous));
		
		return output;
	}
	
	/**
	 * Cleans the specified article (both the raw and linked version)
	 * by replacing non-breaking space by regular spaces, etc.
	 *    
	 * @param article
	 * 		The article to process.
	 */
	protected void cleanArticle(Article article)
	{	// raw text
		String rawText = article.getRawText();
		rawText = StringTools.replaceSpaces(rawText);
		article.setRawText(rawText);
		
		// linked text
		String linkedText = article.getLinkedText();
		if(linkedText==null)
			linkedText = rawText;
		else
			linkedText = StringTools.replaceSpaces(linkedText);
		article.setLinkedText(linkedText);
	}
	
	/**
	 * Removes the signs {@code <} and {@code >}
	 * from the specified text.
	 * <br/>
	 * This method is meant to be used by article reader
	 * to clean text by removing signs that could be
	 * later mistaken for xml elements (and are <i>a
	 * priori</i> not necessary for NER).
	 * 
	 * @param text
	 * 		Original text.
	 * @return
	 * 		Same text without the signs.s
	 */
	protected String removeGtst(String text)
	{	String result = text;
		result = result.replace("<", "");
		result = result.replace(">", "");
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Processes the specified URL to get the
	 * targetted article. Also applies a cleaning step
	 * (removing non-breaking space, and so on.
	 * 
	 * @param url
	 * 		Article address.
	 * @param language
	 * 		Language of the retrieved article, or {@code null} if it is unknown.
	 * @return
	 * 		An Article object corresponding to the targetted URL.
	 * 
	 * @throws ReaderException
	 * 		Problem while retrieving the article.
	 */
	public Article read(URL url, ArticleLanguage language) throws ReaderException
	{	Article result = processUrl(url, language);
		
		cleanArticle(result);
		
		return result;
	}

	/**
	 * Processes the specified URL to get the
	 * targetted article.
	 * 
	 * @param url
	 * 		Article address.
	 * @param language
	 * 		Language of the retrieved article, or {@code null} if it is unknown.
	 * @return
	 * 		An Article object corresponding to the targetted URL.
	 * 
	 * @throws ReaderException
	 * 		Problem while retrieving the article.
	 */
	public abstract Article processUrl(URL url, ArticleLanguage language) throws ReaderException;

	/**
	 * Loads the html source code from the cached file,
	 * or fetches it from the Web server if needed.
	 * 
	 * @param name
	 * 		Name of the concerned article.
	 * @param url
	 * 		URL of the concerned article.
	 * @return
	 * 		The DOM representation of the original page.
	 * 
	 * @throws IOException
	 * 		Problem while accessing the cache or web page.
	 */
	protected Document retrieveSourceCode(String name, URL url) throws IOException
	{	Document result = null;
		logger.increaseOffset();
		logger.log("Retrieve HTML source code");
		
		// check if the cache can/must be used
		String folderPath = FileNames.FO_OUTPUT + File.separator + name;
		File originalFile = new File(folderPath + File.separator + FileNames.FI_ORIGINAL_PAGE);
		if(cache && originalFile.exists())
		{	logger.log("Cache enabled and HTML already retrieved >> we use the cached file ("+originalFile.getName()+")");
			String sourceCode = FileTools.readTextFile(originalFile);
			result = Jsoup.parse(sourceCode);
		}
		
		// otherwise, load and cache the html file
		else
		{	logger.log("Cache disabled or HTML never retrieved before>> we get it from the Web server");
			logger.increaseOffset();
			
			// use custom page loader
//			String sourceCode = manuallyReadUrl(url);
//			System.out.println(sourceCode.toString());
//			result = new Source(sourceCode);
			
			// use jericho page loader
			int timeOut = 5000;
			boolean again;
			do
			{	again = false;
				try
				{	logger.log("Trying to download the Web page");
					result = Jsoup.parse(url,timeOut);
				}
				catch(SocketTimeoutException e)
				{	logger.log("Could not download the page (timeout="+timeOut+" ms) >> trying again");
					timeOut = timeOut + 5000;
					again = true;
				}
				catch(UnsupportedMimeTypeException e)
				{	logger.log(Arrays.asList(
						"WARNING: Could not download the page, the MIME format is not supported.",
						"Error message: "+e.getMessage()
					));
				}
				catch(HttpStatusException e)
				{	logger.log(Arrays.asList(
						"WARNING: Could not download the page, the server returned an error "+e.getStatusCode()+" .",
						"Error message: "+e.getMessage()
					));
				}
			}
			while(again);
			logger.decreaseOffset();
			
			if(result!=null)
			{	logger.log("Page downloaded");
				String sourceCode = result.toString();
				
				// cache html source code
				FileTools.writeTextFile(originalFile, sourceCode);
			}
		}

		//System.out.println(source.toString());
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Reads the source code of the Web page at the specified
	 * URL.
	 * 
	 * @param url
	 * 		Address of the web page to be read.
	 * @return
	 * 		String containing the read HTML source code.
	 * 
	 * @throws IOException
	 * 		Problem while accessing the specified URL.
	 */
	protected String manuallyReadUrl(URL url) throws IOException
	{	boolean trad = false;
		
		BufferedReader br = null;
		
		// open page the traditional way
		if(trad)
		{	InputStream is = url.openStream();
			InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
		}
		
		// open with more options
		else
		{	// setup connection
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setReadTimeout(2000);
            connection.setChunkedStreamingMode(0);
            connection.setRequestProperty("Content-Length", "0");
//			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36");
            connection.connect();
            
            // setup input stream
            // part retrieved from http://stackoverflow.com/questions/538999/java-util-scanner-and-wikipedia
            // original author: Marco Beggio
            InputStream is = null;
            String encoding = connection.getContentEncoding();
            if(connection.getContentEncoding()!=null && encoding.equals("gzip"))
            {	is = new GZIPInputStream(connection.getInputStream());
            }
            else if (encoding != null && encoding.equals("deflate"))
            {	is = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
            }
            else
            {	is = connection.getInputStream();
            }
            
// alternative to spot error details            
//			InputStream is;
//			if (connection.getResponseCode() != 200) 
//				is = connection.getErrorStream();
//			else 
//				is = connection.getInputStream();
            
			InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
		}
		
		// read page
		StringBuffer sourceCode = new StringBuffer();
		String line = br.readLine();
		while (line != null)
		{	sourceCode.append(line+"\n");
			line = br.readLine();
		}
		
		String result = sourceCode.toString();
		br.close();
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// TIME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Extract a date from the specified TIME html element.
	 *  
	 * @param timeElt
	 * 		HTML element.
	 * @param dateFormat 
	 * 		Format used to parse the date.
	 * @return
	 * 		The corresponding date.
	 */
	public Date getDateFromTimeElt(Element timeElt, DateFormat dateFormat)
	{	Date result = null;
	
		String valueStr = timeElt.attr(XmlNames.ATT_DATETIME);
		try
		{	result = dateFormat.parse(valueStr);
		}
		catch (ParseException e)
		{	e.printStackTrace();
		}
	
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// ELEMENTS			/////////////////////////////////////////////
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
	protected void processParagraphElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	// possibly add a new line character first (if the last one is not already a newline)
		if(rawStr.length()>0 && rawStr.charAt(rawStr.length()-1)!='\n')
		{	rawStr.append("\n");
			linkedStr.append("\n");
		}
		
		// recursive processing
		processTextElement(element,rawStr,linkedStr);
		
		// possibly add a new line character (if the last one is not already a newline)
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
	protected boolean processSpanElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result = true;
		processTextElement(element,rawStr,linkedStr);
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
	protected boolean processHyperlinkElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result = true;
			
		// simple text
		String str = element.text();
		if(!str.isEmpty())
		{	str = removeGtst(str);
			rawStr.append(str);
		}
		
		// hyperlink
		String href = element.attr(XmlNames.ATT_HREF);
		String code = "<" + XmlNames.ELT_A + " " +XmlNames.ATT_HREF + "=\"" + href + "\">" + str + "</" + XmlNames.ELT_A + ">";
		linkedStr.append(code);
		
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
	protected void processListElement(Element element, StringBuilder rawStr, StringBuilder linkedStr, boolean ordered)
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
	protected boolean processDivisionElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result = true;
		
		processTextElement(element, rawStr, linkedStr);
		
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
	protected boolean processTableElement(Element element, StringBuilder rawStr, StringBuilder linkedStr)
	{	boolean result = true;
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
	protected void processTextElement(Element textElement, StringBuilder rawStr, StringBuilder linkedStr)
	{	// we process each element contained in the specified text element
		for(Node node: textElement.childNodes())
		{	// element node
			if(node instanceof Element)
			{	Element element = (Element) node;
				String eltName = element.tag().getName();
				
				// section headers: same thing
				if(eltName.equals(XmlNames.ELT_H1) || eltName.equals(XmlNames.ELT_H2) || eltName.equals(XmlNames.ELT_H3)
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
				
				// hyperlinks must be included in the linked version of the article
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
}
