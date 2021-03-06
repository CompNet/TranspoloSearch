package fr.univavignon.transpolosearch.data.article;

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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jdom2.Element;
import org.xml.sax.SAXException;

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.mention.Mentions;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;
import fr.univavignon.transpolosearch.tools.string.StringTools;
import fr.univavignon.transpolosearch.tools.xml.XmlNames;

import fr.univavignon.tools.log.HierarchicalLogger;
import fr.univavignon.tools.log.HierarchicalLoggerManager;
import fr.univavignon.tools.xml.XmlTools;

/**
 * This class represents an article, i.e. all
 * the relevant data extracted from the source page.
 * 
 * @author Vincent Labatut
 */
public class Article implements Comparable<Article>
{
	/**
	 * Creates a new article.
	 * 
	 * @param name
	 * 		Name of the article, also the name of its folder.
	 */
	public Article(String name)
	{	this.name = name;
		
		initFiles(FileNames.FO_WEB_PAGES);
	}
	
	/**
	 * Creates a new article.
	 * 
	 * @param name
	 * 		Name of the article, also the name of its folder.
	 * @param corpusFolder
	 * 		Folder containing the corpus.
	 */
	public Article(String name, String corpusFolder)
	{	this.name = name;
		
		initFiles(corpusFolder);
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of this article, also the name of its folder */
	private String name;
	
	/**
	 * Returns the name of this article.
	 * 
	 * @return
	 * 		Name of this article.
	 */
	public String getName()
	{	return name;
	}
	
	/////////////////////////////////////////////////////////////////
	// TITLE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Title of this article, also the name of the described person */
	private String title;
	
	/**
	 * Returns the title of this article.
	 * 
	 * @return
	 * 		Title of this article.
	 */
	public String getTitle()
	{	return title;
	}
	
	/**
	 * Changes the title of this article.
	 * 
	 * @param title
	 * 		New title for this article.
	 */
	public void setTitle(String title)
	{	this.title = title;
	}

	/////////////////////////////////////////////////////////////////
	// AUTHORS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Authors of this article */
	private List<String> authors = new ArrayList<String>();
	
	/**
	 * Returns the authors of this article.
	 * 
	 * @return
	 * 		List of authors.
	 */
	public List<String> getAuthors()
	{	return authors;
	}
	
	/**
	 * Adds an author to this article.
	 * 
	 * @param author
	 * 		Author to add to this article.
	 */
	public void addAuthor(String author)
	{	authors.add(author);
	}

	/**
	 * Adds the listed authors to this article.
	 * 
	 * @param authors
	 * 		AuthorS to add to this article.
	 */
	public void addAuthors(List<String> authors)
	{	this.authors.addAll(authors);
	}

	/////////////////////////////////////////////////////////////////
	// SOURCE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Address of the source page */
	private URL url = null;
	
	/**
	 * Returns the source URL of this article.
	 * 
	 * @return
	 * 		URL of this article.
	 */
	public URL getUrl()
	{	return url;
	}

	/**
	 * Changes the source URL of this article.
	 * 
	 * @param url
	 * 		New url of this article.
	 */
	public void setUrl(URL url)
	{	this.url = url;
	}

	/////////////////////////////////////////////////////////////////
	// LANGUAGE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Language of the article (only one allowed) */
	private ArticleLanguage language;
	
	/**
	 * Returns the language of this article.
	 * 
	 * @return
	 * 		Language of this article.
	 */
	public ArticleLanguage getLanguage()
	{	return language;
	}
	
	/**
	 * Changes the language of this article.
	 * 
	 * @param language
	 * 		New language for this article.
	 */
	public void setLanguage(ArticleLanguage language)
	{	this.language = language;
	}

	/////////////////////////////////////////////////////////////////
	// DATES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Used to read/write dates */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy HH:mm",Locale.ENGLISH);
	/** Date the page was last retrieved */
	private Date retrievalDate = null;
	/** Date the page was originally published */
	private Date publishingDate = null;
	/** Date the page was last edited */
	private Date modificationDate = null;
	
	/**
	 * Returns the retrieval date of this article.
	 * 
	 * @return
	 * 		Date of retrieval of this article.
	 */
	public Date getRetrievalDate()
	{	return retrievalDate;
	}

	/**
	 * Changes the date this article was retrieved.
	 * 
	 * @param retrievalDate
	 * 		New date of retrieval.
	 */
	public void setRetrievalDate(Date retrievalDate)
	{	this.retrievalDate = retrievalDate;
	}

	/**
	 * Sets the retrieval date to the current date.
	 */
	public void initRetrievalDate()
	{	Calendar calendar = Calendar.getInstance();
		retrievalDate = calendar.getTime();
	}
	
	/**
	 * Returns the publishing date of this article.
	 * 
	 * @return
	 * 		Date of publishing of this article.
	 */
	public Date getPublishingDate()
	{	return publishingDate;
	}

	/**
	 * Changes the date this article was published.
	 * 
	 * @param publishingDate
	 * 		New date of publishing.
	 */
	public void setPublishingDate(Date publishingDate)
	{	this.publishingDate = publishingDate;
	}

	/**
	 * Returns the date of last modification for this article.
	 * 
	 * @return
	 * 		Date of modification of this article.
	 */
	public Date getModificationDate()
	{	return modificationDate;
	}

	/**
	 * Changes the date this article was last modified.
	 * 
	 * @param modificationDate
	 * 		New date of modification.
	 */
	public void setModificationDate(Date modificationDate)
	{	this.modificationDate = modificationDate;
	}

	/////////////////////////////////////////////////////////////////
	// CATEGORIES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Categories of the source page (military, science, etc.) */
	private final List<ArticleCategory> categories = new ArrayList<ArticleCategory>();
	
	/**
	 * Returns the categories of this article
	 * (military, science, etc.).
	 * 
	 * @return
	 * 		Categories of this article.
	 */
	public List<ArticleCategory> getCategories()
	{	return categories;
	}

	/**
	 * Changes the categories of this article
	 * (military, science, etc.).
	 * 
	 * @param categories
	 * 		New categories of this article.
	 */
	public void setCategories(Collection<ArticleCategory> categories)
	{	this.categories.clear();
		this.categories.addAll(categories);
	}

	/////////////////////////////////////////////////////////////////
	// ORIGINAL PAGE	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Original source code of the web page */
	private String originalPage = null;
	
	/**
	 * Returns the original source code
	 * of this page.
	 * 
	 * @return
	 * 		Original source code of this page.
	 */
	public String getOriginalPage()
	{	return originalPage;
	}

	/**
	 * Changes the original source code
	 * of this page.
	 * 
	 * @param originalPage
	 * 		New original source code of this page.
	 */
	public void setOriginalPage(String originalPage)
	{	this.originalPage = originalPage;
	}

	/////////////////////////////////////////////////////////////////
	// FOLDER & FILES	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Folder containing the cached files */
	private String folderPath = null;
	/** Original html file */
	private File originalFile = null;
	/** File containing the raw text */
	private File rawFile = null;
	/** File containing the text with hyperlinks */
	private File linkedFile = null;
	/** File containing the article metadata */
	private File propertiesFile = null;
	
	/**
	 * Initializes all file-related variables.
	 * 
	 * @param corpusFolder
	 * 		Folder containing the corpus.
	 */
	private void initFiles(String corpusFolder)
	{	folderPath = corpusFolder + File.separator + name;
		originalFile = new File(folderPath + File.separator + FileNames.FI_ORIGINAL_PAGE);
		rawFile = new File(folderPath + File.separator + FileNames.FI_RAW_TEXT);
		linkedFile = new File(folderPath + File.separator + FileNames.FI_LINKED_TEXT);
		propertiesFile = new File(folderPath + File.separator + FileNames.FI_PROPERTIES);
	}
	
	/**
	 * Returns the path of the folder containing
	 * the cached files corresponding to this article.
	 * 
	 * @return
	 * 		Path of this article files.
	 */
	public String getFolderPath()
	{	return folderPath;
	}

	/////////////////////////////////////////////////////////////////
	// RAW TEXT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Raw text */
	private String rawText = null;

	/**
	 * Returns the raw text
	 * of this page.
	 * 
	 * @return
	 * 		Raw text of this page.
	 */
	public String getRawText()
	{	return rawText;
	}

	/**
	 * Changes the raw text
	 * of this page.
	 * 
	 * @param rawText
	 * 		New raw text of this page.
	 */
	public void setRawText(String rawText)
	{	this.rawText = rawText;
	}
	
	/////////////////////////////////////////////////////////////////
	// LINKED TEXT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Raw text with hyperlinks */
	private String linkedText = null;

	/**
	 * Returns the linked text
	 * of this page.
	 * 
	 * @return
	 * 		Linked text of this page.
	 */
	public String getLinkedText()
	{	return linkedText;
	}

	/**
	 * Changes the linked text
	 * of this page.
	 * 
	 * @param linkedText
	 * 		New linkedText of this page.
	 */
	public void setLinkedText(String linkedText)
	{	this.linkedText = linkedText;
	}

	/////////////////////////////////////////////////////////////////
	// CONTENT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Cleans both texts (raw and linked) of the article, in order to
	 * remove non-standard space characters, punctuation, ligatures, which
	 * could complicate the task of the recognizers, or any subsequent 
	 * processing.
	 * <br/>
	 * This method is called when reading an {@code Article} from file, and when
	 * retrieving an article from the Web. It should not be called if
	 * the article has already been annotated (be it manually or automatically), 
	 * because it does <i>not</i> update the position of mentions in
	 * the corresponding files. Consequently, if some characters are added
	 * or removed during the cleaning, the position of certain mentions
	 * can become incorrect.
	 */
	public void cleanContent()
	{	logger.log("Clean article content (can take quite some time for very long articles)");
		logger.increaseOffset();
		
		// raw text
		logger.log("Clean raw text");
		rawText = StringTools.cleanText(rawText,language.getLocale());
		
		// linked text
		logger.log("Clean linked text");
		linkedText = StringTools.cleanText(linkedText,language.getLocale());
		
		// remove < and > signs
		logger.log("Remove tag signs");
		removeTagSigns();
		
		// clean title
		logger.log("Possibly setup title (if none), and clean it");
		if(title==null || title.isEmpty())
		{	int length = Math.min(20,rawText.length());
			if(length>0)
				title = rawText.substring(0,length);
			else
				title = "Empty article!";
		}
		title = StringTools.cleanTitle(title,language.getLocale());
		
		logger.decreaseOffset();
	}
	
	/**
	 * Parses both raw and linked texts in order to remove the remaining {@code <} and
	 * {@code >} signs (not belonging to an hyperlink, in the case of the linked text).
	 */
	private void removeTagSigns()
	{	if(rawText.contains("<") || rawText.contains(">"))
		{	StringBuffer rt = new StringBuffer();
			StringBuffer lt = new StringBuffer();
			int i = 0;
			int j = 0;
			while(i<rawText.length())
			{	char c = rawText.charAt(i);
				if(c=='<')
				{	rt.append('(');
					lt.append('(');
				}
				else if(c=='>')
				{	rt.append(')');
					lt.append(')');
				}
				else
				{	rt.append(c);
					c = linkedText.charAt(j); 
					if(c=='<')
					{	do
						{	lt.append(c);
							j++;
							c = linkedText.charAt(j); 
						}
						while(c!='>');
					}
					lt.append(c);
				}
				i++;
				j++;
			}
			
			rawText = rt.toString();
			linkedText = rt.toString();
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// READ				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks if the article corresponding
	 * to the specified article name/title
	 * was previously recorded.
	 * 
	 * @param name
	 * 		Name/title of the article of interest.
	 * @return
	 * 		{@code true} iff the specified article was recorded.
	 */
	public static boolean isCached(String name)
	{	
//		for(int i=0;i<name.length();i++)
//			System.out.println(i+": '"+name.charAt(i)+"'=='"+"Ahmet_Davutoglu".charAt(i)+"' >> "+(name.charAt(i)=="Ahmet_Davutoglu".charAt(i)));
		
		String folderPath = FileNames.FO_WEB_PAGES + File.separator + name;
//		File originalFile = new File(folderPath + File.separator + FileNames.FI_ORIGINAL_PAGE);
		File rawFile = new File(folderPath + File.separator + FileNames.FI_RAW_TEXT);
		File linkedFile = new File(folderPath + File.separator + FileNames.FI_LINKED_TEXT);
//		File propertiesFile = new File(folderPath + File.separator + FileNames.FI_PROPERTIES);
		
		boolean result = rawFile.exists() && linkedFile.exists() /*&& originalFile.exists()*/;
		return result;
	}
	
	/**
	 * Reads an article from file. The location
	 * of the article is automatically inferred
	 * from its name/title. The article is read
	 * in the default corpus folder.
	 * 
	 * @param name
	 * 		Name/title of the article.
	 * @return
	 * 		The corresponding {@code Article} object.
	 * 
	 * @throws ParseException
	 * 		Problem while accessing the article files.
	 * @throws SAXException
	 * 		Problem while accessing the article files.
	 * @throws IOException
	 * 		Problem while accessing the article files.
	 */
	public static Article read(String name) throws ParseException, SAXException, IOException
	{	Article result = new Article(name);
		read(result);
		return result;
	}
	/**
	 * Reads an article from file. The location
	 * of the article is automatically inferred
	 * from its name/title. The article is read
	 * in the specified corpus folder.
	 * 
	 * @param name
	 * 		Name/title of the article.
	 * @param corpusFolder
	 * 		Folder containing the corpus.
	 * @return
	 * 		The corresponding {@code Article} object.
	 * 
	 * @throws ParseException
	 * 		Problem while accessing the article files.
	 * @throws SAXException
	 * 		Problem while accessing the article files.
	 * @throws IOException
	 * 		Problem while accessing the article files.
	 */
	public static Article read(String name, String corpusFolder) throws ParseException, SAXException, IOException
	{	Article result = new Article(name,corpusFolder);
		read(result);
		return result;
	}
	
	/**
	 * Method actually performing the article reading.
	 * 
	 * @param result
	 * 		Blank article, to be filled with the read data.
	 * 
	 * @throws ParseException
	 * 		Problem while accessing the article files.
	 * @throws SAXException
	 * 		Problem while accessing the article files.
	 * @throws IOException
	 * 		Problem while accessing the article files.
	 */
	private static void read(Article result) throws ParseException, SAXException, IOException
	{	// properties
		if(result.propertiesFile.exists())
			result.readProperties();
		else
		{	// if the file does not exist, we create it
			result.initRetrievalDate();
			result.writeProperties();
		}
		
		// original page
		if(result.originalFile.exists())
		{	String originalPage = FileTools.readTextFile(result.originalFile,"UTF-8");
			result.setOriginalPage(originalPage);
		}
		
		// raw text
		String rawText = FileTools.readTextFile(result.rawFile,"UTF-8");
		rawText = rawText.trim();
		result.setRawText(rawText);
		
		// raw text with hyperlinks
		String linkedText = rawText;
		if(result.linkedFile.exists())
		{	linkedText = FileTools.readTextFile(result.linkedFile,"UTF-8");
			linkedText = linkedText.trim();
		}
		result.setLinkedText(linkedText);
		
//		// clean the texts
//		result.cleanContent();
//		// possibly re-record the article if its content was changed due to cleaning
//		boolean changed = !rawText.equals(result.getRawText()) || !linkedText.equals(result.getLinkedText());
//		if(changed)
//			result.write();
	}
	
	/**
	 * Reads the properties of this article 
	 * in an XML file.
	 * 
	 * @throws ParseException
	 * 		Problem while accessing the file.
	 * @throws SAXException
	 * 		Problem while accessing the file.
	 * @throws IOException
	 * 		Problem while accessing the file.
	 */
	private void readProperties() throws ParseException, SAXException, IOException
	{	// schema file
		String schemaPath = FileNames.FO_SCHEMA+File.separator+FileNames.FI_PROPERTY_SCHEMA;
		File schemaFile = new File(schemaPath);

		// load file
		Element root = XmlTools.getRootFromFile(propertiesFile,schemaFile);
		
		// article title
		{	Element titleElt = root.getChild(XmlNames.ELT_TITLE);
			if(titleElt!=null)
			{	String titleStr = titleElt.getTextTrim();
				this.title = titleStr;
			}
		}

		// origine url
		{	Element urlElt = root.getChild(XmlNames.ELT_URL);
			if(urlElt!=null)
			{	String urlStr = urlElt.getTextTrim();
				URL url = new URL(urlStr);
				this.url = url;
			}
		}
		
		// language
		{	Element languageElt = root.getChild(XmlNames.ELT_LANGUAGE);
			if(languageElt!=null)
			{	String languageStr = languageElt.getTextTrim().toUpperCase(Locale.ENGLISH);
				ArticleLanguage language = ArticleLanguage.valueOf(languageStr);
				this.language = language;
			}
		}
		
		// authors
		{	Element authorsElt = root.getChild(XmlNames.ELT_AUTHORS);
			if(authorsElt!=null)
			{	List<Element> authorList = authorsElt.getChildren(XmlNames.ELT_AUTHOR);
				for(Element authorElt: authorList)
				{	String authorStr = authorElt.getTextTrim();
					this.authors.add(authorStr);
				}
			}
		}
		
		// dates
		{	Element datesElt = root.getChild(XmlNames.ELT_DATES);
			// retrieval
			{	Element retrievalDateElt = datesElt.getChild(XmlNames.ELT_RETRIEVAL_DATE);
				String retrievalDateStr = retrievalDateElt.getTextTrim();
				Date retrievalDate = DATE_FORMAT.parse(retrievalDateStr);
				this.retrievalDate = retrievalDate;
			}
			// publishing
			{	Element publishingDateElt = datesElt.getChild(XmlNames.ELT_PUBLISHING_DATE);
				if(publishingDateElt!=null)
				{	String publishingDateStr = publishingDateElt.getTextTrim();
					Date publishingDate = DATE_FORMAT.parse(publishingDateStr);
					this.publishingDate = publishingDate;
				}
			}
			// modification
			{	Element modificationDateElt = datesElt.getChild(XmlNames.ELT_MODIFICATION_DATE);
				if(modificationDateElt!=null)
				{	String modificationDateStr = modificationDateElt.getTextTrim();
					Date modificationDate = DATE_FORMAT.parse(modificationDateStr);
					this.modificationDate = modificationDate;
				}
			}
		}
		
		// categories of biography
		{	Element catElt = root.getChild(XmlNames.ELT_CATEGORY);
			if(catElt!=null)
			{	String catsStr = catElt.getTextTrim().toUpperCase(Locale.ENGLISH);
				String temp[] = catsStr.split(" ");
				categories.clear();
				for(String catStr: temp)
				{	ArticleCategory cat = ArticleCategory.valueOf(catStr);
					categories.add(cat);
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////
	// WRITE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Writes this article in
	 * its predefined folder. All
	 * files are writen, possibly
	 * erasing pre-existing content.
	 * 
	 * @throws IOException
	 * 		Problem while writing this article files. 
	 */
	public void write() throws IOException
	{	// original html code
// now already done in the reader class
//		if(originalPage!=null)
//			FileTools.writeTextFile(originalFile,originalPage,"UTF-8");

		// raw text only
		FileTools.writeTextFile(rawFile,rawText,"UTF-8");
		
		// raw text with hyperlinks
		FileTools.writeTextFile(linkedFile,linkedText,"UTF-8");
		
		// properties
		writeProperties();
	}
	
	/**
	 * Writes the properties of this article
	 * in an XML file.
	 * 
	 * @throws IOException
	 * 		Problem while accessing the file.
	 */
	private void writeProperties() throws IOException
	{	// check folder
		File folder = propertiesFile.getParentFile();
		if(!folder.exists())
			folder.mkdirs();
		// schema file
		String schemaPath = FileNames.FO_SCHEMA+File.separator+FileNames.FI_PROPERTY_SCHEMA;
		File schemaFile = new File(schemaPath);
		
		// build xml document
		Element root = new Element(XmlNames.ELT_PROPERTIES);

		// article title
		if(title!=null)
		{	Element titleElt = new Element(XmlNames.ELT_TITLE);
			titleElt.setText(title);
			root.addContent(titleElt);
		}
		
		// origine url
		if(url!=null)
		{	String urlStr = url.toString();
			Element urlElt = new Element(XmlNames.ELT_URL);
			urlElt.setText(urlStr);
			root.addContent(urlElt);
		}
		
		// language
		if(language!=null)
		{	String languageStr = language.toString();
			Element languageElt = new Element(XmlNames.ELT_LANGUAGE);
			languageElt.setText(languageStr);
			root.addContent(languageElt);
		}
		
		// authors
		if(!authors.isEmpty())
		{	Element authorsElt = new Element(XmlNames.ELT_AUTHORS);
			root.addContent(authorsElt);
			for(String authorStr: authors)
			{	Element authorElt = new Element(XmlNames.ELT_AUTHOR);
				authorElt.setText(authorStr);
				authorsElt.addContent(authorElt);
			}
		}
		
		// dates
		{	Element datesElt = new Element(XmlNames.ELT_DATES);
			root.addContent(datesElt);
				// retrieval
				{	String retrievalDateStr = DATE_FORMAT.format(retrievalDate);
					Element retrievalDateElt = new Element(XmlNames.ELT_RETRIEVAL_DATE);
					retrievalDateElt.setText(retrievalDateStr);
					datesElt.addContent(retrievalDateElt);
				}
				// publishing
				if(publishingDate!=null)
				{	String publishingDateStr = DATE_FORMAT.format(publishingDate);
					Element publishingDateElt = new Element(XmlNames.ELT_PUBLISHING_DATE);
					publishingDateElt.setText(publishingDateStr);
					datesElt.addContent(publishingDateElt);
				}
				// modification
				if(modificationDate!=null)
				{	String modificationDateStr = DATE_FORMAT.format(modificationDate);
					Element modificationDateElt = new Element(XmlNames.ELT_MODIFICATION_DATE);
					modificationDateElt.setText(modificationDateStr);
					datesElt.addContent(modificationDateElt);
				}
		}
		
		// categories of biography
		if(!categories.isEmpty())
		{	String catStr = "";
			for(ArticleCategory category: categories)
				catStr = catStr + category.toString() + " ";
			catStr = catStr.substring(0,catStr.length()-1);
			Element catElt = new Element(XmlNames.ELT_CATEGORY);
			catElt.setText(catStr);
			root.addContent(catElt);
		}
		
		// record file
		XmlTools.makeFileFromRoot(propertiesFile,schemaFile,root);
	}
	
	/////////////////////////////////////////////////////////////////
	// MENTIONS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the list of reference mentions
	 * for this article.
	 * 
	 * @return
	 * 		The list of reference mentions.
	 * 		
	 * @throws IOException
	 * 		Problem while accessing the file.
	 * @throws SAXException
	 * 		Problem while accessing the file.
	 * @throws ParseException 
	 * 		Problem while accessing the file.
	 */
	public Mentions getReferenceMentions() throws SAXException, IOException, ParseException
	{	String path = folderPath + File.separator + FileNames.FI_MENTION_LIST;
		File file = new File(path);
		Mentions result = Mentions.readFromXml(file);
		return result;
	}
	
	/**
	 * Returns the list of mentions for this article, as estimated
	 * by the specified recognizer.
	 * 
	 * @param recognizer
	 * 		Concerned recognizer.
	 * @return
	 * 		The list of reference mentions.
	 * 		
	 * @throws IOException
	 * 		Problem while accessing the file.
	 * @throws SAXException
	 * 		Problem while accessing the file.
	 * @throws ParseException 
	 * 		Problem while accessing the file.
	 */
	public Mentions getEstimatedMentions(InterfaceRecognizer recognizer) throws SAXException, IOException, ParseException
	{	String path = folderPath + File.separator + recognizer.getRecognizerFolder() + File.separator + FileNames.FI_MENTION_LIST;
		File file = new File(path);
		Mentions result = Mentions.readFromXml(file);
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// OBJECT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{	String result = "["+name+"] "+title;
		return result;
	}

	@Override
	public int compareTo(Article o) 
	{	String title2 = o.title;
		int result = title.compareTo(title2);
		return result;
	}
	
	@Override
	public boolean equals(Object obj) 
	{	boolean result = false;
		if(obj!=null && obj instanceof Article)
		{	Article article = (Article)obj;
			result = this.compareTo(article)==0;
		}
		return result;
	}
}
