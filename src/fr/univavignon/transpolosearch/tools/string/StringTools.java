package fr.univavignon.transpolosearch.tools.string;

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
 * along with TranspoloSearch. If not, see <http://www.gnu.org/licenses/>.
 */

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains various methods
 * used when processing strings.
 *  
 * @author Vincent Labatut
 */
public class StringTools
{
	/**
	 * Tests the {@link #splitText(String, int)} method.
	 * 
	 * @param args
	 * 		No need.
	 */
	public static void main(String[] args)
	{	
		// test split
//		String text = "This is a first sentence. Cela semble marcher très bien."
//			+ "What if no space after dot? Or even other punctuation marks!\n"
//			+ "Et même plein de points !?! Ou... des nombres 12.32 et 12,65.\n"
//			+ "On pourrait aussi avoir des abréviations comme M.Dupont ou M. Dupont ; "
//			+ "enfin, there could be spaces and stuff in between sentences.   Like this.  End.";
//		splitText(text, 70);
		
		// sentence positions
//		List<Integer> pos = getSentencePositions(text);
//		System.out.println(text);
//		Iterator<Integer> it = pos.iterator();
//		int current = it.next();
//		for(int i=0;i<text.length();i++)
//		{	if(current==i)
//			{	if(it.hasNext())
//					current = it.next();
//				System.out.print("^");
//			}
//			else
//				System.out.print(" ");
//		}
		
		// test clean
//		String str = " abc\u00A0defg h\ni\rk\tl";
//		String res = replaceSpaces(str);
//		System.out.println("\""+str+"\" vs \""+res+"\"");
//		System.out.println((int)(str.charAt(0))+" vs "+(int)(res.charAt(0)));
//		System.out.println((int)(str.charAt(4))+" vs "+(int)(res.charAt(4)));
//		System.out.println((int)(str.charAt(9))+" vs "+(int)(res.charAt(9)));
//		System.out.println((int)(str.charAt(11))+" vs "+(int)(res.charAt(11)));
//		System.out.println((int)(str.charAt(13))+" vs "+(int)(res.charAt(13)));
//		System.out.println((int)(str.charAt(15))+" vs "+(int)(res.charAt(15)));
	}

	/////////////////////////////////////////////////////////////////
	// COMPARISON		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Compares two strings while ignoring their case */
	public static final Comparator<String> COMPARATOR = new Comparator<String>()
	{	@Override
		public int compare(String s1, String s2)
		{	// remove accents
			String string1 = removeDiacritics(s1);
			String string2 = removeDiacritics(s2);
			
			// remove case
			string1 = string1.toUpperCase(Locale.ENGLISH);
			string2 = string2.toUpperCase(Locale.ENGLISH);
			
			// normalize double quotes
			string1 = string1.replaceAll("«", "\"");
			string2 = string2.replaceAll("«", "\"");
			string1 = string1.replaceAll("»", "\"");
			string2 = string2.replaceAll("»", "\"");
			
			// compare
			int result = string1.compareTo(string2);
			return result;
		}	
	};
	
	/**
	 * Compare the specified characters, using {@link #COMPARATOR},
	 * i.e. ignoring case and diacritics.
	 * 
	 * @param c1
	 * 		First character to compare.
	 * @param c2
	 * 		Second character to compare.
	 * @return
	 * 		Integer representing a classic comparison result.
	 */
	public static int compareCharsRelaxed(int c1, int c2)
	{	
//if(c1=='û')
//	System.out.print("");
		
		String s1 = new String(new int[]{c1},0,1);
		String s2 = new String(new int[]{c2},0,1);
		
		int result = COMPARATOR.compare(s1, s2);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// CHARACTER TYPES	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks whether the specified character
	 * is a punctuation mark or not.
	 * 
	 * @param c
	 * 		The character of interest.
	 * @return
	 * 		{@code true} iff the character is a punctuation mark.
	 */
	public static boolean isPunctuation(int c)
	{	String string = new String(new int[]{c},0,1);
		boolean result = Pattern.matches("\\p{Punct}", string);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// INITIALS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Changes the case of the specified String,
	 * so that the first letter is upper case
	 * and the rest is lower case.
	 * 
	 * @param string
	 * 		String to process.
	 * @return
	 * 		Normalized string resulting of the change in cases.
	 */
	public static String initialize(String string)
	{	String first = string.substring(0,1);
		String rest = string.substring(1);
		String result = first.toUpperCase(Locale.ENGLISH) + rest.toLowerCase(Locale.ENGLISH);
		return result;
	}
	
	/**
	 * Checks if the specified string
	 * starts with an upercase character.
	 * 
	 * @param string
	 * 		The string of interest.
	 * @return 
	 * 		{@code true} iff the string starts with an uppercase.
	 */
	public static boolean hasInitial(String string)
	{	char initial = string.charAt(0);
		boolean result = !Character.isLowerCase(initial); 
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// SPACES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Process the specified string in order to remove
	 * space character-related problems.
	 *  
	 * @param string
	 * 		The original string (not modified).
	 * @return
	 * 		Modified version of the input string.
	 */
	public static String cleanSpaces(String string)
	{	String result = string;
		
		if(result!=null)
		{	// replace all white spaces by regular spaces
			result = result.replaceAll("\\s", " ");
			
			// replace all consecutive spaces by a single one
			result = result.replaceAll(" +", " ");
			
			// remove initial/final spaces
			result = result.trim();
		}
		
		return result;
	}

	/**
	 * Process the specified string in order to replace
	 * non-standard whitespace characters. The number
	 * of characters in the text is not modified
	 * (unlike {@link #cleanSpaces(String)}).
	 *  
	 * @param string
	 * 		The original string (not modified).
	 * @return
	 * 		Modified version of the input string.
	 */
	public static String replaceSpaces(String string)
	{	String result = string;
		
		if(result!=null)
		{	// replace all white spaces by regular spaces
			// new line and tabs are not affected
			result = result.replaceAll("\\p{Z}", " "); // \p{Z} includes more different whitespaces than \s
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// LETTERS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks if the specified string contains any letters.
	 * 
	 * @param string
	 * 		String to analyze.
	 * @return
	 * 		{@code true} iff the string contains at least one letters.
	 */
	public static boolean hasNoLetter(String string)
	{	Pattern r = Pattern.compile("[a-zA-Z]");
		Matcher m = r.matcher(string);
		boolean result = !m.find();
		return result;
	}

	/**
	 * Remove diacritics from the specified text.
	 * <br/>
	 * Taken from <a href="http://stackoverflow.com/questions/15190656/easy-way-to-remove-utf-8-accents-from-a-string">
	 * http://stackoverflow.com/questions/15190656/easy-way-to-remove-utf-8-accents-from-a-string</a>.
	 * 
	 * @param string
	 * 		String to process, containing diacritics.
	 * @return
	 * 		Same string, but withouth the diacritics.
	 */
	public static String removeDiacritics(String string) 
	{	String result = Normalizer.normalize(string, Normalizer.Form.NFD);
		result = result.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	    return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// SPLIT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** 
	 * Pattern used to detect sentences when splitting text 
	 * (taken from Ruchira Gayan Ranaweera's answer from 
	 * http://stackoverflow.com/questions/21430447/how-to-split-paragraphs-into-sentences) 
	 */ 
	private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)", Pattern.MULTILINE);
    
	/**
	 * Returns the position of the first character of each sentence in the
	 * specified text. The sentence splitter is very basic, we consider 
	 * either the newline character, or the presence of a dot followed by 
	 * a space and not preceeded by an uppercase letter.
	 *  
	 * @param text
	 * 		The text to process.
	 * @return
	 * 		A list of integers corresponding to sentence positions in the text.
	 */
	public static List<Integer> getSentencePositions(String text)
	{	List<Integer> result = new ArrayList<Integer>();
		
		Matcher matcher = SENTENCE_PATTERN.matcher(text);
		while(matcher.find())
		{	//String sentence = matcher.group();
			int startPos = matcher.start();
			result.add(startPos);
		}
		
		return result;
	}
	
	/**
	 * Breaks down the specified text in chunks of {@code maxSize} characters.
	 * <br/>
	 * A sentence splitter is used to perform the split between two sentences.
	 * It is a very simple one, we consider either the newline character,
	 * or the presence of a dot followed by a space and not preceeded by an
	 * uppercase letter. 
	 * 
	 * @param text
	 * 		The text to split.
	 * @param maxSize
	 * 		The maximal size of the chunks to produce.
	 * @return
	 * 		A list of smaller chunks corresponding to a broken down version of 
	 * 		the original text.
	 */
	public static List<String> splitText(String text, int maxSize)
	{	List<String> result = new ArrayList<String>();
		
		// identify the sentences
		Matcher matcher = SENTENCE_PATTERN.matcher(text);
		
		// build the chunks
		int start = 0;
		int prevEnd = 0;
		while(matcher.find())
		{	// for debug
			String sentence = matcher.group();
//			System.out.println(sentence);
			
			int curEnd = matcher.end();
			if(curEnd-start > maxSize)
			{	if(start==prevEnd)
					// TODO we could force-split between words, it's better than nothing
					throw new IllegalArgumentException("The sentence \""+sentence+"\" ("+sentence.length()+" chars) is too long to be split using maxSize="+maxSize);
				String chunk = text.substring(start, prevEnd);
				result.add(chunk);
				start = prevEnd;
			}
			
			prevEnd = curEnd;
		}
		
		if(start<text.length())
			result.add(text.substring(start,text.length()));
		
		// for debug
//		System.out.println("result:\n"+result);
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// MISC				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Takes a string and a position in this string,
	 * and returns a part of the string centered around
	 * the specified postion, using the specified range
	 * to define the interval.
	 * <br/>
	 * Another line is used to mark the exact position
	 * with a ^. This function is used mainly for debug
	 * purposes.
	 * 
	 * @param pos
	 * 		The position to highlight.
	 * @param string
	 * 		The concerned text.
	 * @param range
	 * 		The range used to process the interval.
	 * @return
	 * 		The highlighted string.
	 */
	public static String highlightPosition(int pos, String string, int range)
	{	// process the interval
		int beginIndex = Math.max(0, pos-range);
		int endIndex = Math.min(string.length(), pos+range);
		
		// define the result string
		String result = "";
		if(beginIndex>0)
			result = result + "[...]";
		result = result + string.substring(beginIndex, endIndex);
		if(endIndex<string.length())
			result = result + "[...]\n";

		for(int i=0;i<pos-beginIndex+5;i++)
			result = result + " ";
		result = result + "^";
		
		return result;
	}
}