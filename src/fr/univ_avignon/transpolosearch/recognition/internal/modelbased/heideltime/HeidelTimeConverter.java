package fr.univ_avignon.transpolosearch.recognition.internal.modelbased.heideltime;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import opennlp.tools.util.Span;

import fr.univ_avignon.transpolosearch.data.article.Article;
import fr.univ_avignon.transpolosearch.data.entity.AbstractEntity;
import fr.univ_avignon.transpolosearch.data.entity.Entities;
import fr.univ_avignon.transpolosearch.data.entity.EntityType;
import fr.univ_avignon.transpolosearch.recognition.ConverterException;
import fr.univ_avignon.transpolosearch.recognition.RecognizerName;
import fr.univ_avignon.transpolosearch.recognition.internal.AbstractInternalConverter;
import fr.univ_avignon.transpolosearch.tools.file.FileNames;

/**
 * This class is the converter associated to HeidelTime.
 * It is able to convert the text outputed by this NER tool
 * into objects compatible with Nerwip.
 * <br/>
 * It can also read/write these results using raw text
 * and our XML format.
 * 
 * @author Vincent Labatut
 */
public class HeidelTimeConverter extends AbstractInternalConverter<Map<EntityType,List<Span>>>
{	
	/**
	 * Builds a new converter using the specified info.
	 * 
	 * @param nerFolder
	 * 		Folder used to stored the results of the NER tool.
	 */
	public HeidelTimeConverter(String nerFolder)
	{	super(RecognizerName.HEIDELTIME, nerFolder, FileNames.FI_OUTPUT_TEXT);
	}

	/////////////////////////////////////////////////////////////////
	// PROCESS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public Entities convert(Article article, Map<EntityType,List<Span>> data) throws ConverterException
	{	Entities result = new Entities(recognizerName);
		
		String rawText = article.getRawText();
		for(Entry<EntityType,List<Span>> entry: data.entrySet())
		{	EntityType type = entry.getKey();
			List<Span> spans = entry.getValue();
			for(Span span: spans)
			{	// build internal representation of the entity
				int startPos = span.getStart();
				int endPos = span.getEnd();
				String valueStr = rawText.substring(startPos,endPos);
				AbstractEntity<?> entity = AbstractEntity.build(type, startPos, endPos, recognizerName, valueStr);
				
				// ignore overlapping entities
//				if(!result.hasEntity(entity))	//TODO don't remember if i'm supposed to change that, or what?
					result.addEntity(entity);
			}
		}	

		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// RAW				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void writeRawResults(Article article, Map<EntityType,List<Span>> intRes) throws IOException
	{	StringBuffer string = new StringBuffer();
		
		String rawText = article.getRawText();
		for(Entry<EntityType,List<Span>> entry: intRes.entrySet())
		{	EntityType type = entry.getKey();
			List<Span> spans = entry.getValue();
			for(Span span: spans)
			{	// build internal representation of the entity
				int startPos = span.getStart();
				int endPos = span.getEnd();
				String valueStr = rawText.substring(startPos,endPos);
				string.append("["+type.toString()+" '"+valueStr+"' ("+startPos+","+endPos+")]\n");
			}
		}
		
		writeRawResultsStr(article, string.toString());
	}
}
