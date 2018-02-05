package fr.univavignon.transpolosearch.retrieval.reader;

/*
 * TranspoloSearch
 * Copyright2015-18Vincent Labatut
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

/**
 * This exception is thrown when a problem
 * happens while retrieving a text.
 * 
 * @author Yasa Akbulut
 */
public class ReaderException extends Exception
{	/** Class id */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Creates a new exception, with no message. 
	 * By default, the cause of the exception is "article list".
	 */
	public ReaderException()
	{	super();
	}
	
	/**
	 * Creates a new exception, with no message.
	 * 
	 * @param articleList
	 * 		Whether the exception was thrown because the
	 * 		targeted page is an article list.
	 */
	public ReaderException(boolean articleList)
	{	super();
		this.articleList = articleList;
	}
	
	/**
	 * Creates a new exception,
	 * with a specific message.
	 * 
	 * @param message
	 * 		Message of the exception.
	 */
	public ReaderException(String message)
	{	super(message);
	}
	
	/**
	 * Creates a new exception,
	 * with a specific message.
	 * 
	 * @param message
	 * 		Message of the exception.
	 * @param articleList
	 * 		Whether the exception was thrown because the
	 * 		targeted page is an article list.
	 */
	public ReaderException(String message, boolean articleList)
	{	super(message);
		this.articleList = articleList;
	}
	
	/////////////////////////////////////////////////////////////////
	// ARTICLE LIST	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Indicates whether the exception was thrown because the page is an article list (and not a single article) */
	boolean articleList = false;

	/**
	 * Indicates whether this exception was thrown because the targeted page
	 * is not a single article, but rather a list of articles (or a list of
	 * article summaries).
	 * 
	 * @return
	 * 		{@code true} iff the targeted page is a list of articles.
	 */
	public boolean isArticleList()
	{	return articleList;
	}
}
