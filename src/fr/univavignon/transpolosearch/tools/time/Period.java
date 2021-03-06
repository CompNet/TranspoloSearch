package fr.univavignon.transpolosearch.tools.time;	

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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

/**
 * Represents a time period, whose bounds are expressed
 * as two possibly partial dates. If the start and end date
 * are the same, then it is not a period but a punctual date. 
 * 
 * @author Vincent Labatut
 */
public class Period implements Comparable<Period>
{	
	/**
	 * Builds a period based on the two specified
	 * (possibly partial) dates.
	 * 
	 * @param startDate
	 * 		Start of the period.
	 * @param endDate
	 * 		End of the period.
	 */
	public Period(Date startDate, Date endDate)
	{	this.startDate = startDate;
		this.endDate = endDate;
	}
	
	/**
	 * Builds a period based on the single specified
	 * (possibly partial) date.
	 * 
	 * @param date
	 * 		Start and end of the period.
	 */
	public Period(Date date)
	{	this.startDate = date;
		this.endDate = date;
	}
	
	/////////////////////////////////////////////////////////////////
	// START DATE		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Start of this period */
	private Date startDate;

	/**
	 * Gets the start date of this period.
	 * 
	 * @return
	 * 		Start date of this period.
	 */
	public Date getStartDate()
	{	return startDate;
	}

//	public void setDay(int day)
//	{	this.day = day;
//	}
	
	/////////////////////////////////////////////////////////////////
	// END DATE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** End of this period */
	private Date endDate;

	/**
	 * Gets the end date of this period.
	 * 
	 * @return
	 * 		End date of this period.
	 */
	public Date getEndDate()
	{	return endDate;
	}
	
	/////////////////////////////////////////////////////////////////
	// TEXT				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{	String result = "";
		if(startDate!=null)
			result = result + startDate.toString();
		result = result + "-";
		if(endDate!=null)
			result = result + endDate.toString();
		return result;
	}
	
	/**
	 * Returns a string representing this date,
	 * meant to be written in text files.
	 * 
	 * @return
	 * 		A full string representation of this date.
	 */
	public String exportToString()
	{	String result = startDate.exportToString() + "-" + endDate.exportToString();
		return result;
	}
	
	/**
	 * Parse a string representing this date,
	 * meant to be read from a text file,
	 * and returns the corresponding date object.
	 * 
	 * @param string
	 * 		A full string representation of this date.
	 * @return
	 * 		The date built from the string.
	 */
	public static Period importFromString(String string)
	{	String tmp[] = string.split("-");
		Date startDate = Date.importFromString(tmp[0]);
		Date endDate = Date.importFromString(tmp[1]);
		Period result = new Period(startDate,endDate);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// DATES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Tests whether the specified date is contained in this period.
	 * 
	 * @param date
	 * 		Considered date.
	 * @return
	 * 		{@code true} iff the date belongs to this period.
	 */
	public boolean contains(Date date)
	{	boolean result = date.isContained(startDate, endDate);
		return result;
	}
	
	/**
	 * Checks if the specified period is compatible with this period,
	 * i.e. if they match exactly when ignoring their missing parts.
	 * 
	 * @param period
	 * 		The period we want to compare to this date.
	 * @return
	 * 		{@code true} iff they match exactly when ignoring their
	 * 		missing parts.
	 */
	public boolean isCompatible(Period period)
	{	boolean result = false;
		if(period!=null)
		{	Date startDate2 = period.startDate;
			Date endDate2 = period.endDate;
			result = ((startDate==null && startDate2==null) || startDate.isCompatible(startDate2))
					&& ((endDate==null && endDate2==null) || endDate.isCompatible(endDate2));
		}
		return result;
	}
	
	/**
	 * Returns a {@code Period} instance which is a copy of this {@code Period},
	 * but with possibly completed dates, when the day or month is
	 * missing. Does not complete dates where the "larger" elements
	 * are missing (e.g. day and month are known, but not year): in this
	 * case, the method returns {@code null}.
	 *  
	 * @return
	 * 		Completed instance of this period, or {@code null} if completion
	 * 		is not possible.
	 */
	public Period completeDates()
	{	int startDay = this.startDate.getDay();
		int startMonth = this.startDate.getMonth();
		int startYear = this.startDate.getYear();
		int endDay = this.endDate.getDay();
		int endMonth = this.endDate.getMonth();
		int endYear = this.endDate.getYear();
		
		boolean completable = true;
		
		// complete the years
		if(startYear==0)
		{	if(endYear==0)
				completable = false;
			else
				startYear = endYear;
		}
		else
		{	if(endYear==0)
				endYear = startYear;
		}
		
		if(completable)
		{	// complete the months
			if(startMonth==0)
			{	if(endMonth==0)
				{	startMonth = 1;
					endMonth = 12;
				}
				else
					endMonth = startMonth;
			}
			else
			{	if(endMonth==0)
					endMonth = startMonth;
			}
			
			// complete the days
			if(startDay==0)
			{	if(endDay==0)
				{	startDay = 1;
					Calendar c = Calendar.getInstance();
					c.set(endYear, endMonth-1, endDay);
					c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
					endDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
				}
				else
					startDay = endDay;
			}
			else
			{	if(endDay==0)
					endDay = startDay;
				
			}
		}
		
		// build and return result
		Period result = null;
		if(completable)
		{	Date startDate = new Date(startDay,startMonth,startYear);
			Date endDate = new Date(endDay,endMonth,endYear);
			result = new Period(startDate,endDate);
		}
		return result;
	}
	
	/**
	 * Combines the specified date to this period. If the 
	 * date is more extreme than one of the current bounds, 
	 * it becomes the new bound. Same thing if it is more 
	 * <i>precise</i>.
	 * 
	 * @param date
	 * 		The date to merge to this period.
	 * @return
	 * 		{@code true} iff one of the start/end dates of
	 * 		this period was modified during the processed.
	 */
	public boolean mergeDate(Date date)
	{	boolean changedStart = false;
		boolean changedEnd = false;
		
		if(startDate==null)
		{	startDate = date;
			changedStart = true;
		}
		else
		{	int year1 = startDate.getYear();
			int year2 = date.getYear();
			int month1 = startDate.getMonth();
			int month2 = date.getMonth();
			int day1 = startDate.getDay();
			int day2 = date.getDay();
			
			// check date vs start date
			if(year1==0)
			{	startDate = date;
				changedStart = true;
			}
			else if(year2<year1)
			{	if(endDate==null)
				{	endDate = startDate;
					changedEnd = true;
				}
				startDate = date;
				changedStart = true;
			}	
			else if(year2==year1)
			{	if(month1==0)
				{	startDate = date;
					changedStart = true;
				}
				else if(month2<month1)
				{	if(endDate==null)
					{	endDate = startDate;
						changedEnd = true;
					}
					startDate = date;
					changedStart = true;
				}
				else if(month2==month1)
				{	if(day1==0)
					{	startDate = date;
						changedStart = true;
					}
					else if(day2<day1)
					{	if(endDate==null)
						{	endDate = startDate;
							changedEnd = true;
						}
						startDate = date;
						changedStart = true;
					}
				}
			}
			
			// check date vs end date
			if(!changedStart && !changedEnd)
			{	if(endDate==null)
				{	endDate = date;
					changedEnd = true;
				}
			
				else
				{	year1 = endDate.getYear();
					month1 = endDate.getMonth();
					day1 = endDate.getDay();
					
					if(year2>year1)
					{	endDate = date;
						changedEnd = true;
					}
					else if(year2==year1)
					{	if(month2>month1)
						{	endDate = date;
							changedEnd = true;
						}
						else if(month2==month1)
						{	if(day2>day1)
							{	endDate = date;
								changedEnd = true;
							}
						}
					}
				}
			}
		}
		
		boolean result = changedEnd || changedStart;
		return result;
	}
	
	/**
	 * Combines the specified period to this period. If some
	 * of its bounds (or both) is more extreme than one of the 
	 * current bounds, it replaces it. Same thing if it is more 
	 * <i>precise</i>.
	 * 
	 * @param period
	 * 		The period to merge with this one.
	 * @return
	 * 		{@code true} iff one of the start/end dates of
	 * 		this period was modified during the processed.
	 */
	public boolean mergePeriod(Period period)
	{	boolean result = false;
		if(period!=null)
		{	Date startDate = period.getStartDate();
			result = mergeDate(startDate);
			Date endDate = period.getEndDate();
			result = result || mergeDate(endDate);
		}
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// COMPARISON		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public int compareTo(Period period)
	{	Date startDate2 = period.startDate;
		Date endDate2 = period.endDate;
		
		int result = startDate.compareTo(startDate2);
		if(result==0)
			result = endDate.compareTo(endDate2);
		
		return result;
	}
	
	@Override
	public boolean equals(Object object)
	{	boolean result = false;
		if(object instanceof Period)
		{	Period period = (Period)object;
			result = compareTo(period)==0;
		}
		return result;
	}

	
	/**
	 * Processes the proportion of overlap between two periods.
	 * It is processed according to the following formula:
	 * overlap/shortest where overlap is the overlap expressed
	 * in days and shortest is the shortest of the two periods,
	 * also expressed in days.
	 * 
	 * @param period
	 * 		The period to which we want to compare this period.
	 * @return
	 * 		A float in [0;1] measuring how similar the periods are.
	 */
	public float processOverlap(Period period)
	{	float result = 0;
		
		// first we complete the possibly incomplete dates
		Period period1 = completeDates();
		Period period2 = period.completeDates();
		
		// then we compare the completed periods
		if(period1!=null && period2!=null)
		{	// get the start/end dates
			Date startDate1 = period1.getStartDate();
			Date endDate1 = period1.getEndDate();
			Date startDate2 = period2.getStartDate();
			Date endDate2 = period2.getEndDate();
			
			// exchange periods if needed
			float overlap = 0;
			if(startDate1.compareTo(startDate2)>0)
			{	startDate1 = period2.getStartDate();
				endDate1 = period2.getEndDate();
				startDate2 = period1.getStartDate();
				endDate2 = period1.getEndDate();
			}
			
			// process overlap
			if(endDate1.compareTo(startDate2)>=0)
			{	Date endDate = endDate1;
				if(endDate1.compareTo(endDate2)>=0)
					endDate = endDate2;
				LocalDate d1 = LocalDate.of(startDate1.getYear(), startDate1.getMonth(), startDate1.getDay());
				LocalDate d2 = LocalDate.of(endDate.getYear(), endDate.getMonth(), endDate.getDay());
				overlap = ChronoUnit.DAYS.between(d1, d2);
			}
				
			// process shortest period length
			if(overlap>0)
			{	// length of the first period
				LocalDate d11 = LocalDate.of(startDate1.getYear(), startDate1.getMonth(), startDate1.getDay());
				LocalDate d12 = LocalDate.of(endDate1.getYear(), endDate1.getMonth(), endDate1.getDay());
				long length1 = ChronoUnit.DAYS.between(d11, d12);
				// length of the second period
				LocalDate d21 = LocalDate.of(startDate2.getYear(), startDate2.getMonth(), startDate2.getDay());
				LocalDate d22 = LocalDate.of(endDate2.getYear(), endDate2.getMonth(), endDate2.getDay());
				long length2 = ChronoUnit.DAYS.between(d21, d22);
				// process overlap proportion
				float shortest = Math.max(length1,length2);
				result = overlap / shortest;
			}
		}
		
		return result;
	}

	/**
	 * Processes the time interval between two non-overlapping periods,
	 * expressed as a proportion of the shortest of the two periods.
	 * 
	 * @param period
	 * 		The period to which we want to compare this period.
	 * @return
	 * 		A positive float in [0;1] measuring the time separating the periods,
	 * 		zero if the periods do not overlap.
	 */
	public float processInterval(Period period)
	{	float result = 0;
		
		// first we complete the possibly incomplete dates
		Period period1 = completeDates();
		Period period2 = period.completeDates();
		
		// then we compare the completed periods
		if(period1!=null && period2!=null)
		{	// get the start/end dates
			Date startDate1 = period1.getStartDate();
			Date endDate1 = period1.getEndDate();
			Date startDate2 = period2.getStartDate();
			Date endDate2 = period2.getEndDate();
			
			// exchange periods if needed
			float interval = 0;
			if(startDate1.compareTo(startDate2)>0)
			{	startDate1 = period2.getStartDate();
				endDate1 = period2.getEndDate();
				startDate2 = period1.getStartDate();
				endDate2 = period1.getEndDate();
			}
			
			// process overlap
			if(endDate1.compareTo(startDate2)<0)
			{	LocalDate d1 = LocalDate.of(endDate1.getYear(), endDate1.getMonth(), endDate1.getDay());
				LocalDate d2 = LocalDate.of(startDate2.getYear(), startDate2.getMonth(), startDate2.getDay());
				interval = ChronoUnit.DAYS.between(d1, d2);
			}
				
			// process shortest period length
			if(interval>0)
			{	// length of the first period
				LocalDate d11 = LocalDate.of(startDate1.getYear(), startDate1.getMonth(), startDate1.getDay());
				LocalDate d12 = LocalDate.of(endDate1.getYear(), endDate1.getMonth(), endDate1.getDay());
				long length1 = ChronoUnit.DAYS.between(d11, d12);
				// length of the second period
				LocalDate d21 = LocalDate.of(startDate2.getYear(), startDate2.getMonth(), startDate2.getDay());
				LocalDate d22 = LocalDate.of(endDate2.getYear(), endDate2.getMonth(), endDate2.getDay());
				long length2 = ChronoUnit.DAYS.between(d21, d22);
				// process interval proportion
				float shortest = Math.max(length1,length2);
				result = interval / shortest;
			}
		}
		
		return result;
	}
}
