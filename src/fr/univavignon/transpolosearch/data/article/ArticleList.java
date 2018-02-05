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
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class represents a list of articles. It is used when assessing 
 * NER performance, to process stats on groups of articles. For instance,
 * to distinguish between a training and a testing sets.
 * 
 * @author Vincent Labatut
 */
public class ArticleList extends ArrayList<File>
{	/** Class id */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new article list.
	 * 
	 * @param name
	 * 		Name of the article list.
	 */
	public ArticleList(String name)
	{	super();
		this.name = name;
	}

	/**
	 * Creates a new article list.
	 * 
	 * @param name
	 * 		Name of the article list.
	 * @param articles
	 * 		List of articles (as files).
	 */
	public ArticleList(String name, Collection<File> articles)
	{	super(articles);
		this.name = name;
	}

	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of this article list */
	private String name;
	
	/**
	 * Returns the name of this article list.
	 * 
	 * @return
	 * 		Name of this article list.
	 */
	public String getName()
	{	return name; 
	}
}
