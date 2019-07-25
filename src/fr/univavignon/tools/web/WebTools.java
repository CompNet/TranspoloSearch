package fr.univavignon.tools.web;

/*
 * CommonTools
 * Copyright 2010-19 Vincent Labatut
 * 
 * This file is part of CommonTools.
 * 
 * CommonTools is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 2 of the License, or (at your option) any later version.
 * 
 * CommonTools is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with CommonTools. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import fr.univavignon.tools.log.HierarchicalLogger;
import fr.univavignon.tools.log.HierarchicalLoggerManager;

/**
 * This class contains a set of methods related to Web communication.
 * 
 * @author Vincent Labatut 
 * @version 2
 */
public class WebTools
{	
	/////////////////////////////////////////////////////////////////
	// LOGGING			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	protected static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// GET				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Sends a get request to the specified URL, retrieve the answer and
	 * returns it as a {@code String}.
	 * 
	 * @param url
	 * 		Targeted URL.
	 * @return
	 * 		String corresponding to the obtained text.
	 * 
	 * @throws ClientProtocolException
	 * 		Problem while accessing the URL.
	 * @throws IOException
	 * 		Problem while reading the answer.
	 */
	public static String processGet(URI url) throws ClientProtocolException, IOException
	{	// query the server	
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		HttpResponse response = httpClient.execute(request);
		
		// read answer
		String result = readAnswer(response);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// ANSWERS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Receives an object representing an HTTP answer, reads its content
	 * and returns the corresponding string.
	 *  
	 * @param response
	 * 		Object representing the HTTP answer.
	 * @return
	 * 		A String representing the content of the answer.
	 * 
	 * @throws IllegalStateException
	 * 		Problem while reading the answer.
	 * @throws IOException
	 * 		Problem while reading the answer.
	 */
	public static String readAnswer(HttpResponse response) throws IllegalStateException, IOException
	{	logger.log("Read HTTP answer");
		logger.increaseOffset();
		
		// init reader
		HttpEntity entity = response.getEntity();
		InputStream stream = entity.getContent();
		InputStreamReader streamReader = new InputStreamReader(stream,"UTF-8");
		BufferedReader bufferedReader = new BufferedReader(streamReader);
		
		// read answer
		StringBuffer stringBuffer = new StringBuffer();
		String line;
		int nbr = 0;
		while((line = bufferedReader.readLine())!=null)
		{	stringBuffer.append(line+"\n");
			nbr++;
//			logger.log("Line:" +line);
		}
		logger.log("Lines read: "+nbr);
		
		String result = stringBuffer.toString();
		logger.decreaseOffset();
		return result;
	}
}