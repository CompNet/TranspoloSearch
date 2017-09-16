package fr.univavignon.transpolosearch.data.search;

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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.event.Event;
import fr.univavignon.transpolosearch.data.event.MyPam;
import fr.univavignon.transpolosearch.data.event.DummyDistanceMetric;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import jsat.SimpleDataSet;
import jsat.classifiers.DataPoint;
import jsat.clustering.Clusterer;
import jsat.clustering.SeedSelectionMethods.SeedSelection;
import jsat.clustering.kmeans.HamerlyKMeans;
import jsat.clustering.kmeans.KMeans;
import jsat.clustering.kmeans.XMeans;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.distancemetrics.DistanceMetric;

/**
 * Collection of search results returned by a collection of
 * search engines, with additional info resulting from their
 * subsequent processing.
 * 
 * @param <T>
 * 		Type of results handled by this class. 
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractSearchResults<T extends AbstractSearchResult>
{
	/**
	 * Initializes the search result.
	 */
	public AbstractSearchResults()
	{	
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// ENGINES		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of search engines involved in the current search */
	protected Set<String> engineNames = new TreeSet<String>();
	
	/////////////////////////////////////////////////////////////////
	// RESULTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Map of results */
	protected Map<String,T> results = new HashMap<String,T>();
	
	/**
	 * Returns the number of entries in this collection of search results.
	 * 
	 * @return
	 * 		Number of entries in this map.
	 */
	public int size()
	{	int result = results.size();
		return result;
	}
	
	/** Tokenizer used to classify the results */
	private static TokenizerFactory TOKENIZER_FACTORY;
	/** Initialization of the tokenizer */
	static
	{	TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
		TOKENIZER_FACTORY = new LowerCaseTokenizerFactory(TOKENIZER_FACTORY);
//		TOKENIZER_FACTORY = new EnglishStopTokenizerFactory(TOKENIZER_FACTORY);		//stop list, but only for English
//		TOKENIZER_FACTORY = new PorterStemmerTokenizerFactory(TOKENIZER_FACTORY);	// don't know if the stemmer supports French
		
	}
    
	/**
	 * Identifies clusters of similar results (independently from
	 * the mentions detected later). 
	 * <br/>
	 * Most of this source code was taken from the LinkPipe website.
	 * http://alias-i.com/lingpipe/demos/tutorial/cluster/src/TokenCosineDocCluster.java
	 */
	public void clusterArticles()
	{	logger.log("Clustering the articles");
		logger.increaseOffset();
			// each result is processed separately
			List<T> remainingRes = new ArrayList<T>();
			List<Double> lengths = new ArrayList<Double>();
			List<ObjectToCounterMap<String>> counters = new ArrayList<ObjectToCounterMap<String>>();
			for(T result: results.values())
			{	if(result.status==null)
				{	remainingRes.add(result);
					
					// tokenize
					String text = result.article.getRawText();
					char[] charText = text.toCharArray();
					Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(charText,0,charText.length);//TODO vérifier si on vire la ponctuation
					ObjectToCounterMap<String> counter = new ObjectToCounterMap<String>();
					counters.add(counter);
					String token;
			    	while((token=tokenizer.nextToken()) != null)
			    		counter.increment(token);
			    	
			    	// compute length
			    	double sum = 0.0;
		            for (Counter c : counter.values())
		            {	double count = c.doubleValue();
		                sum = sum + count;  // tf =sqrt(count); sum += tf * tf
		            }
			    	double length = Math.sqrt(sum);
			    	lengths.add(length);
				}
			}
	
			// process the distance between all results
			double distanceMatrix[][] = new double[results.size()][results.size()];
			for(int i=0;i<results.size()-1;i++)
			{	double length1 = lengths.get(i);
				ObjectToCounterMap<String> counter1 = counters.get(i);
				
				for(int j=i+1;j<results.size();j++)
				{	double length2 = lengths.get(j);
					ObjectToCounterMap<String> counter2 = counters.get(j);
					
					double productVal = 0.0;
					for (String token : counter1.keySet())
				    {	int count1 = counter1.getCount(token);
						int count2 = counter2.getCount(token);
				        productVal = productVal + Math.sqrt(count2 * count1); // tf = sqrt(count)
				    }
					
					double cosVal = productVal / (length1 * length2);
					double dist = 1.0 - cosVal;
					distanceMatrix[i][j] = dist;
					distanceMatrix[j][i] = dist;
				}
			}
			DistanceMetric dm = new DummyDistanceMetric(distanceMatrix);
			
			// build a dummy dataset
			List<DataPoint> dp = new ArrayList<DataPoint>();
			for(int i=0;i<remainingRes.size();i++)
			{	Vec v = new DenseVector(Arrays.asList((double)i));
				DataPoint d = new DataPoint(v);
				dp.add(d);
			}
			SimpleDataSet ds = new SimpleDataSet(dp);
			
			// proceed with the cluster analysis
	        KMeans simpleKMeans = new HamerlyKMeans(dm,SeedSelection.FARTHEST_FIRST);
			Clusterer clusterer = new XMeans(simpleKMeans);
			int[] membership = new int[remainingRes.size()];
			clusterer.cluster(ds, membership);
			
			// set upt the clusters in the results themselves
			int maxClust = 0;
			int i = 0;
			for(T result: remainingRes)
			{	result.cluster = membership[i];
				if(membership[i] > maxClust)
					maxClust = membership[i];
				i++;
			}
			
		logger.decreaseOffset();
		logger.log("Article clustering complete: "+(maxClust+1)+" clusters detected for "+remainingRes.size()+" remaining articles");
	}

	/////////////////////////////////////////////////////////////////
	// FILTERING	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Discards results whose language does not matche the targeted one.
	 *
	 * @param language
	 * 		targeted language of the articles.
	 */
	private void filterByLanguage(ArticleLanguage language)
	{	logger.log("Removing articles not matching the language constraint: "+language);
		logger.increaseOffset();
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	total++;
					if(!result.filterByLanguage(language,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Language-based filtering complete: "+count+"/"+total);
	}
	
	/**
	 * Discards results describing only events not contained 
	 * in the specified date range.
	 *  
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 */
	private void filterByDate(Date startDate, Date endDate)
	{	logger.log("Removing articles not fitting the date constraints: "+startDate+"->"+endDate);
		logger.increaseOffset();
			fr.univavignon.transpolosearch.tools.time.Date start = new fr.univavignon.transpolosearch.tools.time.Date(startDate);
			fr.univavignon.transpolosearch.tools.time.Date end = new fr.univavignon.transpolosearch.tools.time.Date(endDate);
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	total++;
					if(!result.filterByDate(start,end,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Date-based filtering complete: "+count+"/"+total);
	}
	
	/**
	 * Discards results corresponding only to articles not containing 
	 * the compulsory expression.
	 *  
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article.
	 */
	private void filterByKeyword(String compulsoryExpression)
	{	logger.log("Discarding articles not containing the compulsory expression \""+compulsoryExpression+"\"");
		logger.increaseOffset();
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				
				if(result.status==null)
				{	total++;
					if(!result.filterByKeyword(compulsoryExpression,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Keyword-based filtering complete: "+count+"/"+total);
	}

	/**
	 * Discards results describing only events not contained 
	 * in the specified date range, or not containing the 
	 * compulsory expression.
	 *  
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there is no such constraint.
	 * @param language
	 * 		targeted language of the articles.
	 */
	public void filterByContent(String compulsoryExpression, ArticleLanguage language)
	{	logger.log("Starting filtering the articles by content");
		logger.increaseOffset();
		
		// log stuff
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("compulsoryExpression="+compulsoryExpression);
			logger.log("language="+language);
		logger.decreaseOffset();
		
		// filter depending on the language
		if(language!=null)
			filterByLanguage(language);
		else
			logger.log("No targeted language to process");
		
		// possibly filter the resulting texts depending on the compulsory expression
		if(compulsoryExpression!=null)
			filterByKeyword(compulsoryExpression);
		else
			logger.log("No compulsory expression to process");
		
		logger.decreaseOffset();
		logger.log("Content-based filtering complete");
	}
	
	/**
	 * Discards results describing only events not contained
	 * in the specified time period.
	 * 
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 * @param searchDate
	 * 		Whether the date constraint was applied before ({@code true}) at search time,
	 * 		or should be applied <i>a posteriori</i> here ({@code false}).
	 */
	public void filterByEntity(Date startDate, Date endDate, boolean searchDate)
	{	logger.log("Starting filtering the articles by entity");
		logger.increaseOffset();
		
		// log stuff
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			String txt = "searchDate="+searchDate;
			if(searchDate)
				txt = txt + "(dates are ignored here, because they were already used during the search)";
			logger.log(txt);
		logger.decreaseOffset();
		
		// possibly filter the texts depending on the dates they contain
		if(!searchDate)
		{	if(startDate==null || endDate==null)
				logger.log("WARNING: one date is null, so both of them are ignored");
			else
				filterByDate(startDate, endDate);
		}
		
		logger.decreaseOffset();
		logger.log("Entity-based filtering complete");
	}
	
	/////////////////////////////////////////////////////////////////
	// MENTIONS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Detects the entity mentions present in each specified article.
	 * 
	 * @param recognizer
	 * 		The recognizer used to detect the mentions.
	 * @throws ProcessorException
	 * 		Problem while applying the NER tool.
	 */
	public void detectMentions(InterfaceRecognizer recognizer) throws ProcessorException
	{	logger.log("Detecting entity mentions in all the articles");
		logger.increaseOffset();
		
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				
				if(result.status==null)
				{	total++;
					if(result.detectMentions(recognizer,total)>0)
						count++;
				}
			}
		
		logger.decreaseOffset();
		logger.log("Mention detection complete: ("+count+" for "+total+" articles)");
	}

	/**
	 * Displays the entity mentions associated to each remaining article.
	 */
	public void displayRemainingMentions()
	{	logger.log("Displaying remaining articles and entity mentions");
		logger.increaseOffset();
		
		int total = 0;
		for(T result: results.values())
		{	if(result.status==null)
			{	total++;
				result.displayRemainingMentions(total);
			}
		}
		
		logger.decreaseOffset();
		logger.log("Display complete");
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Results corresponding to the events constituting the clusters */
	protected List<List<T>> mapClustRes = new ArrayList<List<T>>();
	/** Position in the result corresponding to the events constituting the clusters */
	protected List<List<Integer>> mapClustEvt = new ArrayList<List<Integer>>();
	
	/**
	 * Identifies the events described in the articles associated to
	 * the search results.
	 * 
	 * @param bySentence
	 * 		Whether to retrieve events by sentence (all event-related entity mentions
	 * 		must be in the same sentence) or by article.
	 */
	public void extractEvents(boolean bySentence)
	{	logger.log("Extracting events from all the articles");
		logger.increaseOffset();
		
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				
				if(result.status==null)
				{	total++;
					if(result.extractEvents(bySentence,total)>0)
						count++;
				}
			}
		
		logger.decreaseOffset();
		logger.log("Event extraction complete: ("+count+"/"+total+")");
	}
	
	/** Title of the Web resource */
	public static final String COL_TITLE = "Page title";
	/** URL of the Web resource */
	public static final String COL_URL = "URL";
	/** Length of the article, in characters */
	public static final String COL_LENGTH = "Text length";
	/** Author of the article */
	public static final String COL_AUTHORS = "Author(s)";
	/** Status of the page (if none, it means it is still handled) */
	public static final String COL_STATUS = "Status";
	/** Publication date of the Web resource */
	public static final String COL_PUB_DATE = "Publication date";
	/** Name of the social search engine */
	public static final String COL_SOCIAL_ENGINE = "Social media";
	/** Rank according to some search engine */
	public static final String COL_RANK = "Rank ";
	/** Name of the search engine or social media */
	public static final String COL_ENGINE = "Engine";
	/** Rank of the event in the article */
	public static final String COL_EVENT_RANK = "Event rank";
	/** Dates associated to the event */
	public static final String COL_EVENT_DATES = "Dates";
	/** Locations associated to the event */
	public static final String COL_EVENT_LOCATIONS = "Locations";
	/** Persons associated to the event */
	public static final String COL_EVENT_PERSONS = "Persons";
	/** Organizations associated to the event */
	public static final String COL_EVENT_ORGANIZATIONS = "Organizations";
	/** Personal roles associated to the event */
	public static final String COL_EVENT_FUNCTIONS = "Functions";
	/** Intellectual productions associated to the event */
	public static final String COL_EVENT_PRODUCTIONS = "Production";
	/** Meetings associated to the event */
	public static final String COL_EVENT_MEETINGS = "Meetings";
	/** Misc comments */
	public static final String COL_COMMENTS = "Comments";
	/** Whether a social post was written by the targeted person, or not */
	public static final String COL_ORIGINAL = "Original post";
	/** ID of the cluster of events */
	public static final String COL_EVENT_CLUSTER = "Event Cluster";
	/** ID of the cluster of articles */
	public static final String COL_ARTICLE_CLUSTER = "Article Cluster";
	/** Column name for the URL of the web search or id of the social search */
	public static final String COL_URL_ID = "URL/ID";
	/** Column name for the title of the web search or content of the social search */
	public static final String COL_TITLE_CONTENT = "Title/Content";
	
	/**
	 * Records the results of the search as a CSV file.
	 * 
	 * @param bySentence 
	 * 		Whether the events are searched in the whole article or in
	 * 		individual sentences.
	 * @param byCluster
	 * 		Whether the individual instances of events should be recorded,
	 * 		or the clusters of events detected later.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while accessing to the result file.
	 * @throws FileNotFoundException
	 * 		Problem while accessing to the result file.
	 */
	public abstract void exportEvents(boolean bySentence, boolean byCluster) throws UnsupportedEncodingException, FileNotFoundException;

	/**
	 * Identify groups of similar events among the previously identified events.
	 */
	public void clusterEvents()
	{	logger.log("Clustering events from all the articles");
		logger.increaseOffset();
			mapClustRes.clear();
			mapClustEvt.clear();
		
			// build a list of all events
			List<Event> allEvents = new ArrayList<Event>();
			Map<Integer,T> resMap = new HashMap<Integer,T>();
			Map<Integer,Integer> evtMap = new HashMap<Integer,Integer>();
			int idx = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	for(int i=0;i<result.events.size();i++)
					{	Event event = result.events.get(i);
						allEvents.add(event);
						resMap.put(idx, result);
						evtMap.put(idx, i);
						idx++;
					}
				}
			}
			
			if(allEvents.size()<2)
				logger.log("Not enough events to process, so no event clustering");
			else
			{	// init the distances between events
				double[][] dist = new double[allEvents.size()][allEvents.size()];
				for(int i=0;i<allEvents.size()-1;i++)
				{	Event event1 = allEvents.get(i);
					for(int j=i+1;j<allEvents.size();j++)
					{	Event event2 = allEvents.get(j);
						float sim = event1.processJaccardSimilarity(event2);
						dist[i][j] = 1 - sim;
						dist[j][i] = 1 - sim;
					}
				}
				DistanceMetric dm = new DummyDistanceMetric(dist);
				
				// build a dummy dataset
				List<DataPoint> dp = new ArrayList<DataPoint>();
				for(int i=0;i<allEvents.size();i++)
				{	Vec v = new DenseVector(Arrays.asList((double)i));
					DataPoint d = new DataPoint(v);
					dp.add(d);
				}
				SimpleDataSet ds = new SimpleDataSet(dp);
				
				// apply partition around medoids (PAM)
				MyPam pam = new MyPam(dm);
				int[] membership = new int[allEvents.size()];
				pam.cluster(ds, membership);
				
				// setup event clusters
				for(int m=0;m<membership.length;m++)
				{	// get group membership for the current event
					int memb = membership[m];
				
					// build/get both group lists
					List<T> clusterRes;
					List<Integer> clusterEvt;
					if(mapClustRes.size()<=memb)
					{	clusterRes = new ArrayList<T>();
						mapClustRes.add(clusterRes);
						clusterEvt = new ArrayList<Integer>();
						mapClustEvt.add(clusterEvt);
					}
					else
					{	clusterRes = mapClustRes.get(memb);
						clusterEvt = mapClustEvt.get(memb);
					}
					
					// add the event to both lists
					T res = resMap.get(m);
					clusterRes.add(res);
					int evt = evtMap.get(m);
					clusterEvt.add(evt);
				}
			}
			
		logger.decreaseOffset();
		logger.log("Event clustering complete: "+mapClustRes.size()+" clusters detected for "+allEvents.size()+" events");
	}

	/////////////////////////////////////////////////////////////////
	// CSV			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Receives a print writer and writes a summary of these results.
	 * This method is meant to be used when exporting a combined file
	 * of both web and social results.
	 * 
	 * @param pw
	 * 		Print write pointing at a file which was previously opened
	 * 		in write mode.
	 */
	public abstract void writeCombinedResults(PrintWriter pw);
}
