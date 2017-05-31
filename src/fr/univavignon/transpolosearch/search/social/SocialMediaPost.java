package fr.univavignon.transpolosearch.search.social;

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

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Represents a post published on a social media.
 * 
 * @author Vincent Labatut
 */
public class SocialMediaPost
{
	/**
	 * Initializes the post.
	 * 
	 * @param author
	 * 		Author of this post. 
	 * @param date 
	 * 		Date of publication of this post.
	 * @param content 
	 * 		Textual content of this post.
	 */
	public SocialMediaPost(String author, Date date, String content)
	{	this.content = content;
		this.author = author;
		this.date = date;
	}
	
	/////////////////////////////////////////////////////////////////
	// CONTENT		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Textual content of this post */
	public String content;
	
	/////////////////////////////////////////////////////////////////
	// AUTHOR		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of the author of this post */
	public String author;
	
	/////////////////////////////////////////////////////////////////
	// DATE			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Publication date of this post */
	public Date date;
	
	/////////////////////////////////////////////////////////////////
	// COMMENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Posts commenting this post */ 
	public List<SocialMediaPost> comments = new ArrayList<SocialMediaPost>();

	/////////////////////////////////////////////////////////////////
	// FILE			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Date format used to write/read comments in text files */
	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	/** 
	 * Write this post (and its comments) to 
	 * a text file.
	 * 
	 * @param writer
	 * 		Writer on the currently open text file.
	 */
	public void writeAsText(PrintWriter writer)
	{	writer.println(author);
		writer.println(DATE_FORMAT.format(date));
		writer.println(content);
		writer.println(comments.size());
		for(SocialMediaPost comment: comments)
			comment.writeAsText(writer);
	}
	
	/**
	 * Retrieve a post from a text file, as well as its comments
	 * (if any).
	 * 
	 * @param scanner
	 * 		Scanner on the previously opened text file.
	 * @return
	 * 		An object representing the post read from the text file.
	 */
	public static SocialMediaPost readFromText(Scanner scanner)
	{	SocialMediaPost result = null;
	
		try
		{	// init the post
			String author = scanner.nextLine().trim();
			String dateStr = scanner.nextLine().trim();
			Date date = DATE_FORMAT.parse(dateStr);
			String content = scanner.nextLine().trim();
			result = new SocialMediaPost(author, date, content);
			
			// add its comments
			String nbrStr = scanner.nextLine().trim();
			int nbr = Integer.parseInt(nbrStr);
			for(int i=0;i<nbr;i++)
			{	SocialMediaPost comment = readFromText(scanner);
				result.comments.add(comment);
			}
		}
		catch (ParseException e)
		{	e.printStackTrace();
		}
		
		return result;
	}
}
