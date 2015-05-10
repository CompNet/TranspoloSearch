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
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
}
// TODO remove "<" and ">" (or replace them by [ and ]) from the retrieved text, in order to avoid problems when handling xml later