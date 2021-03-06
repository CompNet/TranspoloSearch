package fr.univavignon.transpolosearch.processing.internal.modelbased.heideltime;

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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.entity.mention.MentionDate;
import fr.univavignon.transpolosearch.data.entity.mention.Mentions;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.internal.modelbased.AbstractModelbasedInternalDelegateRecognizer;
import fr.univavignon.transpolosearch.tools.string.StringTools;
import fr.univavignon.transpolosearch.tools.time.Date;
import fr.univavignon.transpolosearch.tools.xml.XmlNames;

/**
 * This class acts as a delegate for the mention recognition with HeidelTime.
 * <br/>
 * Recommended parameter values:
 * <ul>
 * 		<li>{@code doIntervalTagging}: {@code false}</li>
 * </ul>
 * 
 * @author Vincent Labatut
 */
class HeidelTimeDelegateRecognizer extends AbstractModelbasedInternalDelegateRecognizer<List<String>, HeidelTimeModelName>
{	
	/**
	 * Builds and sets up an object representing
	 * an HeidelTime recognizer.
	 * 
	 * @param heidelTime
	 * 		Recognizer in charge of this delegate.
	 * @param modelName
	 * 		Predefined mainModel used for mention detection.
	 * @param loadModelOnDemand
	 * 		Whether or not the mainModel should be loaded when initializing this
	 * 		recognizer, or only when necessary. 
	 * @param doIntervalTagging
	 * 		Whether intervals should be detected or ignored (?). 
	 * 
	 * @throws ProcessorException 
	 * 		Problem while loading the models or tokenizers.
	 */
	public HeidelTimeDelegateRecognizer(HeidelTime heidelTime, HeidelTimeModelName modelName, boolean loadModelOnDemand, boolean doIntervalTagging) throws ProcessorException
	{	super(heidelTime,modelName,loadModelOnDemand,false,false,false,false);
	
		this.doIntervalTagging = doIntervalTagging; //TODO this is actually ignored when loadModelOnDemand is false
	}

	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override	
	public String getFolder()
	{	String result = recognizer.getName().toString();
		
		result = result + "_" + "mainModel=" + modelName.toString();
		result = result + "_" + "intervals=" + doIntervalTagging;
//		result = result + "_" + "ignPro=" + ignorePronouns;
//		result = result + "_" + "ignNbr=" + ignoreNumbers;
//		result = result + "_" + "exclude=" + exclusionOn;
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// ENTITY TYPES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void updateHandledEntityTypes()
	{	handledTypes = new ArrayList<EntityType>();
		List<EntityType> temp = modelName.getHandledTypes();
		handledTypes.addAll(temp);
	}

	/////////////////////////////////////////////////////////////////
	// LANGUAGES	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public boolean canHandleLanguage(ArticleLanguage language)
	{	boolean result = modelName.canHandleLanguage(language);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// PARAMETERS		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Whether intervals should be detected or ignored */
	private boolean doIntervalTagging = false;
	
	/////////////////////////////////////////////////////////////////
	// MODELS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Model used by HeidelTime to detect mentions */
	private HeidelTimeStandalone mainModel;
	/** Alternative model, in case we have to deal with news */
	private HeidelTimeStandalone altModel;

    @Override
	protected boolean isLoadedModel()
    {	boolean result = mainModel!=null;
    	return result;
    }
    
    @Override
	protected void resetModel()
    {	mainModel = null;
    	altModel = null;
    }
	
	@Override
	protected void loadModel() throws ProcessorException
	{	logger.increaseOffset();
		
		mainModel = modelName.buildMainTool(doIntervalTagging);
		altModel = modelName.buildAltTool(doIntervalTagging);
		
		logger.decreaseOffset();
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** HeidelTime does not really need to split long text, but if we don't, it takes significantly more time */
	private static final int MAX_SIZE = 25000;
	
	@Override
	protected List<String> detectMentions(Article article) throws ProcessorException
	{	logger.increaseOffset();
		List<String> result = new ArrayList<String>();
		
		logger.log("Applying HeidelTime to detect dates");
		String text = article.getRawText();
		java.util.Date date = article.getPublishingDate();
		
		// we don't really need to break down the text, but HeidelTime takes too much time when the text is very long
		List<String> parts = StringTools.splitText(text, MAX_SIZE);
		
		for(int i=0;i<parts.size();i++)
		{	String part = parts.get(i);
			logger.log("Processing HeidelTime part #"+(i+1)+"/"+parts.size()+" ("+part.length()+" chars)");
			logger.increaseOffset();
			try
			{	String answer;
				
				// if HeidelTime needs a reference date
				if(modelName.requiresDate())
				{	if(date!=null)
						answer = mainModel.process(part, date);
					else
						answer = altModel.process(part);
				}
				
				// if it doesn't need a date
				else
				{	if(date!=null)
						answer = mainModel.process(part, date);
					else
						answer = mainModel.process(part);
				}
				
				result.add(part);
				result.add(answer);
			}
			catch (DocumentCreationTimeMissingException e)
			{	logger.log("ERROR: problem with the date given to HeidelTime ("+date+")");
//				e.printStackTrace();
				throw new ProcessorException(e.getMessage());
			}
			
			logger.decreaseOffset();
		}
		
	    logger.decreaseOffset();
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// CONVERSION		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Header we need to remove before parsing the output text */
	private static final String ORIGINAL_DOCTYPE = "<!DOCTYPE TimeML SYSTEM \"TimeML.dtd\">";
	/** Value of the TIMEX3 attribute "type" for a calendar date */
	private static final String TYPE_DATE = "DATE";
	/** Value of the TIMEX3 attribute "type" for a day time (and possibly date) */
	private static final String TYPE_TIME = "TIME";
	/** Separte time from date in certain TIMEX3 values */
	private static final String TIME_SEPARATOR = "T";
	/** Separte time from date in certain TIMEX3 values */
	private static final String WEEK_CODE = "W";
	/** Before Christ code */
	private static final String BC_CODE = "BC";
	/** List of strings not acceptable as year values */
	private static final List<String> YEAR_BLACKLIST = Arrays.asList(
		"PAST_REF","PRESENT_REF","FUTURE_REF",
		"UNDEF"
//		"XXXX"
	);
	/** List of strings not acceptable as month values */
	private static final List<String> MONTH_BLACKLIST = Arrays.asList(
		"H1","H2",				// half (=semester)
		"Q1","Q2","Q3","Q4",	// quarters
		"WI","SP","SU","FA"		// seasons
	);
	/** Begining of the value string when only the date is specified */
	private static final String TIME_PREFIX = "XXXX-XX-XX";
	
	@Override
	public Mentions convert(Article article, List<String> data) throws ProcessorException
	{	logger.increaseOffset();
		Mentions result = new Mentions(recognizer.getName());
		
		logger.log("Processing each part of data and its associated answer");
		Iterator<String> it = data.iterator();
		logger.increaseOffset();
		int i = 0;
		int prevSize = 0;
		while(it.hasNext())
		{	i++;
			logger.log("Processing part "+i+"/"+data.size()/2);
			String originalText = it.next();
			String ocAnswer = it.next();
			
			// parse the xml source
			logger.log("Parsing the XML source previously produced by HeidelTime");
			Element root;
			try
			{	String xmlSource = ocAnswer.replace(ORIGINAL_DOCTYPE, "");
				xmlSource = xmlSource.replaceAll("&", "&amp;"); // needed to handle possible "&" chars (we suppose the original text does not contain any mention)
				SAXBuilder sb = new SAXBuilder();
				Document doc = sb.build(new StringReader(xmlSource));
				root = doc.getRootElement();
			}
			catch (JDOMException e)
			{	//e.printStackTrace();
				System.err.println(ocAnswer);
				throw new ProcessorException(e.getMessage());
			}
			catch (IOException e)
			{	//e.printStackTrace();
				throw new ProcessorException(e.getMessage());
			}
	
			// process the xml document
			logger.log("Processing the resulting XML document");
			logger.increaseOffset();
			int index = -1; //-1 and not zero, because a new line is inserted at the beginning of the article in the XML file 
			XMLOutputter xo = new XMLOutputter();
			List<Content> children = root.getContent();
			for(Content child: children)
			{	// text content is just counted
				if(child instanceof Text)
				{	Text t = (Text)child;
					String str = t.getText();
					int length = str.length();
					logger.log("("+index+")"+str+ "[["+length+"]]");
					index = index + length;
				}
				
				// elements are processed individually
				else if(child instanceof Element)
				{	Element e = (Element)child;
					String str = e.getText();
					int length = str.length();
					logger.log("("+index+")"+xo.outputString(e)+ "[["+length+"]]");
					MentionDate mention = convertElement(e, prevSize+index);
					if(mention!=null)
						result.addMention(mention);
					index = index + length;
				}
			}
			logger.decreaseOffset();
			
			// update size
			prevSize = prevSize + originalText.length();
		}
		logger.decreaseOffset();

		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Receives an XML element, and processes it to
	 * extract a date mention.
	 * 
	 * @param element
	 * 		Element to process.
	 * @param index
	 * 		Current position in the original text (in characters).
	 * @return
	 * 		The created mention, or {@code null} if it was not
	 * 		possible to create it due to a lack of information.
	 */
	private MentionDate convertElement(Element element, int index)
	{	logger.increaseOffset();
		MentionDate result = null;
		XMLOutputter xo = new XMLOutputter();
				
		// check if the element does not contain any lower element
		List<Element> children = element.getChildren();
		if(!children.isEmpty())
			logger.log("WARNING: detected some encapsulated elements in "+xo.outputString(element));
		
		else
		{	String txType = element.getAttributeValue(XmlNames.ATT_TYPE);
			// we only focus on dates and date-times
			if(txType.equals(TYPE_DATE) || txType.equals(TYPE_TIME))
			{	String valueStr = element.getAttributeValue(XmlNames.ATT_VALUE);
				String text = element.getText();
				Date date = parseTimex3Value(valueStr,text);
				if(date==null)
					logger.log("WARNING: could not parse the date/time in element "+xo.outputString(element)); //TODO WARNING: 
				else
				{	int length = text.length();
					result = new MentionDate(index, index+length, recognizer.getName(), text, date);
				}
			}
			else
				logger.log("Element not describing a date/time (ignored): "+xo.outputString(element));
		}
		
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Parse the specified text to extract a date.
	 * If the string does not contain a numerical date,
	 * then the method returns {@code null}.
	 * 
	 * @param value
	 * 		A TIMEX3 value string supposed to contain a date.
	 * @param text
	 * 		The text associated to the TIMEX3 element (for debugging).
	 * @return
	 * 		A custom object representing contained same date,
	 * 		or {@code null} if some critical information is missing.
	 */
	private Date parseTimex3Value(String value, String text)
	{	Date result = null;
		int year = 0;
		int month = 0;
		int day = 0;
	
		// check if the string contains only a time
		if(value.startsWith(TIME_PREFIX))
			logger.log("There is only a time, not a precise date (original text: "+text+") >> discarding the whole date");
		
		// check if it's an AD year (by opposition to BC)
		else if(value.startsWith(BC_CODE))
			logger.log("The date contains a before-christ (BC) year (original text: "+text+") >> discarding the whole date");
		
		// relevant date
		else
		{	// break down the string
			String s[] = value.split("-");	//UNDEF-REF-hour-PLUS-5 UNDEF-REF-week-WE-PLUS-1 XXXX-XX-XXT07:00
			
			// process the year
			// check if the year is acceptable
			if(YEAR_BLACKLIST.contains(s[0]))
				logger.log("There is no precise year (original text: "+text+") >> the year is discarded, as well as the whole date");
			else
			{	try
				{	year = Integer.parseInt(s[0]);
					// for cases like "the 1950s", HeidelTime inexplicably returns the value 195
					// we try to define a workaround for this case
					if(year>99 && year <1000)
					{	logger.log("The year should not be <1000: HeidelTime returned the value "+year+" for the string \""+text+"\" >> multiplying by 10"); 
						year = year * 10;
					}
				}
				catch(NumberFormatException e)
				{	//e.printStackTrace();
					logger.log("WARNING: could not parse the year in string \""+value+"\" (original text: "+text+") >> discarding the whole date");
				}
				
				// process the month
				if(year!=0 && s.length>1)
				{	// check if the month is acceptable
					if(MONTH_BLACKLIST.contains(s[1]))
						logger.log("There is no precise month (original text: "+text+") >> the month is discarded, as well as the day");
					else
					{	// is the second date component actually a week number?
						if(s[1].startsWith(WEEK_CODE))
						{	String weekStr = s[1].substring(1);
							int week = Integer.parseInt(weekStr);
							Calendar cal = Calendar.getInstance();
							cal.setWeekDate(year, week, Calendar.MONDAY);
							logger.log("We got a week number \""+value+"\" (original text: "+text+") >> converted to a month: "+month+"");
						}
						else	
						{	try
							{	month = Integer.parseInt(s[1]);
							}
							catch(NumberFormatException e)
							{	//e.printStackTrace();
									logger.log("WARNING: could not parse the month string \""+value+"\" (original text: "+text+") >> discarding it, as well as the day");
							}
						}
						
						// process the day
						if(month!=0 && s.length>2)
						{	try
							{	String s2[] = s[2].split(TIME_SEPARATOR); // ignore the possible time indicated after the date (separated by a T)
								day = Integer.parseInt(s2[0]);
							}
							catch(NumberFormatException e)
							{	//e.printStackTrace();
								logger.log("WARNING: could not parse the day in string \""+value+"\" (original text: "+text+") >> discarding it"); 
							}
						}
					}
				}
			}
			
			// build date object
			if(year!=0)
				result = new Date(day,month,year);
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// RAW FILE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void writeRawResults(Article article, List<String> intRes) throws IOException
	{	String temp = "";
		int i = 0;
		for(String str: intRes)
		{	i++;
			if(i%2==1)
				temp = temp + "\n>>> Part " + ((i+1)/2) + "/" + intRes.size() + " - Original Text <<<\n" + str + "\n";
			else
				temp = temp + "\n>>> Part " + (i/2) + "/" + intRes.size() + " - HeidelTime Response <<<\n" + str + "\n";
		}
		writeRawResultsStr(article, temp);
	}
}
