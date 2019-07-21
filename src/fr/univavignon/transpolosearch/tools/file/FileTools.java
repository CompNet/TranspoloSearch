package fr.univavignon.transpolosearch.tools.file;

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
import java.io.FileFilter;
import java.io.FilenameFilter;

/**
 * This class contains various methods
 * related to file management.
 *  
 * @author Vincent Labatut
 */
public class FileTools extends fr.univavignon.tools.file.FileTools
{	
	/////////////////////////////////////////////////////////////////
	// FILTERS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Filter focusing on raw.txt files */
	public final static FilenameFilter FILTER_RAW_TEXT = createFilenameFilter(FileNames.FI_RAW_TEXT);
	
	/** Filter able to retain only directories containing a raw.txt file */
	public final static FileFilter FILTER_ARTICLES = new FileFilter()
	{	@Override
		public boolean accept(File file)
		{	boolean result = false;
			if(file.isDirectory())
			{	String rf[] = file.list(FILTER_RAW_TEXT);
				result = rf!=null && rf.length>0;
			}
			return result;
		}
	};
}
