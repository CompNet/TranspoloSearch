package fr.univavignon.transpolosearch.tools.string;

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

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains various methods
 * used when processing strings representing hyperlinks.
 *  
 * @author Vincent Labatut
 */
public class LinkTools
{
	/////////////////////////////////////////////////////////////////
	// POSITION			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Gets some position in a linked text. The 
	 * specified position is expressed relatively
	 * to the text without links. The returned position
	 * concerns the linked text. Here, 'linked' means
	 * there are html hyperlinks in the text.
	 * 
	 * @param linkedText
	 * 		Text with hyperlinks.
	 * @param position
	 * 		Position in the link-less text.
	 * @return
	 * 		Same position, but in the linked text.
	 */
	public static int getLinkedTextPosition(String linkedText, int position)
	{	int p = 0;
		int result = 0;
		boolean open = false;
		
		// parse text until position is reached
		while(p<position)
		{	char c = linkedText.charAt(result);
			
			if(open)
				open = c!='>';
			else
			{	open = c=='<';
				if(!open)
					p++;
			}
			
			result++;
		}
		
		if(result<linkedText.length())
		{	char c = linkedText.charAt(result);
			if(c=='<')
				result = linkedText.indexOf(">", result) + 1;
		}
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// REMOVAL			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Removes a piece of text from the specified linked text,
	 * without dammaging the hyperlinks. If an hyperlink
	 * becomes empty after the removal, then it is itself removed.
	 * 
	 * @param linkedText
	 * 		Original text with hyperlinks.
	 * @param position
	 * 		Start of the piece of text to be removed,
	 * 		expressed without regards for the links.
	 * @param length
	 * 		Length ot the piece of text to be removed,
	 * 		expressed in characters and without regards for the links.
	 * @return
	 * 		Shortened text.
	 */
	public static String removeFromLinkedText(String linkedText, int position, int length)
	{	String result = linkedText;
		int pos = getLinkedTextPosition(linkedText,position);
		
		for(int i=0;i<length;i++)
		{	char c = result.charAt(pos);
			if(c=='<')
				pos = result.indexOf(">", pos) + 1;
			result = result.substring(0,pos) + result.substring(pos+1);
		}
		
		return result;
	}

	/**
	 * Removes empty hyperlinks (i.e. whose element does
	 * not have any text content) from the specified
	 * linked text.
	 * 
	 * @param linkedText
	 * 		Original text with hyperlinks.
	 * @return
	 * 		Linked text without empty hyperlinks.
	 */
	public static String removeEmptyLinks(String linkedText)
	{	String result = linkedText;
		int idx = result.indexOf("></");
		
		while(idx!=-1)
		{	int startPos = idx;
			while(result.charAt(startPos)!='<')
				startPos--;
			int endPos = result.indexOf(">", idx+2);
			result = result.substring(0,startPos) + result.substring(endPos+1);
			idx = result.indexOf("></");
		}
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// URL				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Scans the specified linked text, and returns a list of all the
	 * URL present in this text as hyperlinks. 
	 * 
	 * @param linkedText
	 * 		The linked text to process.
	 * @return
	 * 		List of hyperlinks in this text.
	 */
	public static List<String> extractUrls(String linkedText)
	{	List<String> result = new ArrayList<String>();
		String tag = "<a ";
		String att = " href=";
	
		int pos = linkedText.indexOf(tag);
		while(pos!=-1 && pos<linkedText.length()-1)
		{	int pos2 = linkedText.indexOf(att,pos);
			int start = pos2 + att.length() + 1;
			char delim = linkedText.charAt(start-1);
			int end = linkedText.indexOf(delim,start);
			String urlStr = linkedText.substring(start,end).trim();
			result.add(urlStr);
			pos = linkedText.indexOf(tag,pos+1);
		}
		
		return result;
	}
}
