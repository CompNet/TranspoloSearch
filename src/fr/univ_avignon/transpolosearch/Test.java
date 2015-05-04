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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

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

//		nero();
		
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
		
//		URL url = new URL("http://www.lemonde.fr/culture/article/2014/07/16/la-prise-de-position-d-olivier-py-sur-le-fn-a-heurte-les-avignonnais_4457735_3246.html");
//		URL url = new URL("http://www.lemonde.fr/afrique/article/2015/05/02/au-togo-l-opposition-coincee-apres-son-echec-a-la-presidentielle_4626476_3212.html");
//		URL url = new URL("http://www.lemonde.fr/climat/article/2015/05/04/climat-les-energies-propres-en-panne-de-credits-de-recherche_4626656_1652612.html");
//		URL url = new URL("http://www.lemonde.fr/les-decodeurs/article/2015/05/03/les-cinq-infos-a-retenir-du-week-end_4626595_4355770.html");
//		URL url = new URL("http://www.lemonde.fr/les-decodeurs/article/2015/04/29/seisme-au-nepal-une-perte-economique-superieure-au-pib_4624928_4355770.html");
		URL url = new URL("http://www.liberation.fr/vous/2015/05/04/coeur-carmat-le-deuxieme-greffe-decede-a-son-tour_1289323");
		
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
	
	/////////////////////////////////////////////////////////////////
	// OTHER STUFF	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Testing the NERO named entity recognition tool.
	 * TODO: remove the libs if not used here
	 * 
	 * @throws Exception
	 * 		Some problem occurred...
	 */
//	private static void nero() throws Exception
//	{	byte[] encodedBytes = Base64.encodeBase64("vince.labatut@gmail.com:rlm40PPO".getBytes());
//		String encoding = new String(encodedBytes);
//
//		// première requête
//		String url = "https://nero.irisa.fr/texts.xml";
//		HttpPost post = new HttpPost(url);
//		post.setHeader("Authorization", "Basic " + encoding);
//		
//		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
//		urlParameters.add(new BasicNameValuePair("text[content]", "Je vais à Marseille cet été voir l'Olympique de Marseille."));
//		post.setEntity(new UrlEncodedFormEntity(urlParameters));
//		
//		HttpClient client = new DefaultHttpClient();
//		HttpResponse response = client.execute(post);
//		int responseCode = response.getStatusLine().getStatusCode();
//		System.out.println("Response Code : " + responseCode);
//		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),"UTF-8"));
//		StringBuffer res = new StringBuffer();
//		String line = "";
//		while ((line = rd.readLine()) != null)
//		{	System.out.println(line);
//			res.append(line);
//		}
//		
//		SAXBuilder sb = new SAXBuilder();
//		Document doc = sb.build(new StringReader(res.toString()));
//		Element root = doc.getRootElement();
//		Element idElt = root.getChild("id");
//		String id = idElt.getValue();
//		
//		// seconde requête
//		int i = 1;
//		do
//		{	System.out.println("\nRepetition "+i);
//			Thread.sleep(5000);
//			url = "https://nero.irisa.fr/texts/"+id+".xml";
//			System.out.println("url="+url);
//			HttpGet get = new HttpGet(url);
//			get.setHeader("Authorization", "Basic " + encoding);
//
//			client = new DefaultHttpClient();
//			response = client.execute(get);
//			responseCode = response.getStatusLine().getStatusCode();
//			System.out.println("Response Code : " + responseCode);
//			rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),"UTF-8"));
//			res = new StringBuffer();
//			while((line = rd.readLine()) != null)
//			{	System.out.println(line);
//				res.append(line);
//			}
//			i++;
//		}
//		while(responseCode!=200);
//		
//		sb = new SAXBuilder();
//		doc = sb.build(new StringReader(res.toString()));
//		root = doc.getRootElement();
//		Element resultElt = root.getChild("result");
//		String result = resultElt.getValue();
//		System.out.println("\nResult="+result);
//	}
}
