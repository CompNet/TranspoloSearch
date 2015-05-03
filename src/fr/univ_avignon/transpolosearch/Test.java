package fr.univ_avignon.transpolosearch;

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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.services.customsearch.model.Result;

import fr.univ_avignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univ_avignon.transpolosearch.retrieval.reader.ArticleReader;
import fr.univ_avignon.transpolosearch.search.GoogleSearch;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLoggerManager;

/**
 * This class is used to launch some processes
 * testing the various features of the software.
 * 
 * @author Vincent Labatut
 */
@SuppressWarnings({ "unused" })
public class Test
{	/**
	 * Basic main function, launches the
	 * required test. Designed to be modified
	 * and launched from Eclipse, no command-line
	 * options.
	 * 
	 * @param args
	 * 		None needed.
	 * 
	 * @throws Exception
	 * 		Something went wrong... 
	 */
	public static void main(String[] args) throws Exception
	{	
		// retrieval
		testRetrievalGeneric();
		
		// search
//		testGoogleSearch();

		logger.close();
	}
	
	/////////////////////////////////////////////////////////////////
	// RETRIEVAL	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests the retrieval of text comming from random Web sites.
	 * 
	 * @throws Exception
	 * 		Problem during the retrieval.
	 */
	private static void testRetrievalGeneric() throws Exception
	{	logger.setName("Test-GenericRetrieval");
		logger.log("Start testing the generic Web page retrieval");
		logger.increaseOffset();
		
		URL url = new URL("http://www.lemonde.fr/culture/article/2014/07/16/la-prise-de-position-d-olivier-py-sur-le-fn-a-heurte-les-avignonnais_4457735_3246.html");
//		URL url = new URL("http://www.lemonde.fr/afrique/article/2015/05/02/au-togo-l-opposition-coincee-apres-son-echec-a-la-presidentielle_4626476_3212.html");
		
		ArticleRetriever retriever = new ArticleRetriever(false);
		retriever.process(url);
		
		logger.log("Test terminated");
		logger.decreaseOffset();
	}
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests specifically Google's custom search wrapper.
	 * 
	 * @throws Exception
	 * 		Something went wrong during the search. 
	 */
	private static void testGoogleSearch() throws Exception
	{	logger.setName("Test-GoogleSearch");
		logger.log("Start testing Google Custom Search");
		logger.increaseOffset();
		
		GoogleSearch gs = new GoogleSearch();
	
		// number of results
		gs.resultNumber = 200;
	
		// sort by date
		gs.sortByDate = true;
		// range
		gs.dateRange = "20150101:20150131";
		
		// launch search
		List<Result> result = gs.search("Cécile Helle");
		
		logger.log("Displaying results: "+result.size()+"/"+gs.resultNumber);
		logger.increaseOffset();
			int i = 0;
			for(Result res: result)
			{	i++;
				logger.log(Arrays.asList(
					"Result "+i+"/"+result.size(),
					res.getHtmlTitle(),
					res.getFormattedUrl(),
					"----------------------------------------")
				);
			}
		logger.decreaseOffset();
		
		logger.log("Test terminated");
		logger.decreaseOffset();
	}

	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	private static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
}
