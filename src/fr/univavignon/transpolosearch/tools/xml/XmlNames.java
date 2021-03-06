package fr.univavignon.transpolosearch.tools.xml;

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

/**
 * This class contains XML-related strings.
 * 
 * @author Vincent Labatut 
 */
public class XmlNames
{	
	/////////////////////////////////////////////////////////////////
	// CUSTOM ATTRIBUTES	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Represents an article */
	public static final String ATT_ARTICLE = "article";
	/** Represents a corpus */
	public static final String ATT_CORPUS = "corpus";
	/** Represents a creation date */
	public static final String ATT_CREATION = "creation";
//	/** Represents... a date! */
//	public static final String ATT_DATE = "date";
	/** Whether the text can be edited, or not */
	public static final String ATT_EDITABLE = "editable";
	/** Person who originally annotated an article */
	public static final String ATT_EDITOR = "editor";
	/** Mention end position */
	public static final String ATT_END = "end";
	/** Id of the entity associated to a mention */
	public static final String ATT_ENTITY_ID = "entityId";
	/** Font size */
	public static final String ATT_FONT_SIZE = "fontSize";
	/** Some identification code */
	public static final String ATT_ID = "id";
	/** Representation of a knowledge base */
	public static final String ATT_KNOWLEDGE_BASE = "knowledgeBase";
	/** Linker applied to the entity */
	public static final String ATT_LINKER = "linker";
	/** Represents a modification date */
	public static final String ATT_MODIFICATION = "modification";
	/** Some object name */
	public static final String ATT_NAME = "name";
	/** Recognizer used to detect the mention */
	public static final String ATT_RECOGNIZER = "recognizer";
	/** Resolver applied to the mention/entity */
	public static final String ATT_RESOLVER = "resolver";
	/** Sex of an entity */
	public static final String ATT_SEX = "sex";
	/** Mention start position */
	public static final String ATT_START = "start";
	/** GUI tooltip */
	public static final String ATT_TOOLTIP = "tooltip";
	/** Mention type */
	public static final String ATT_TYPE = "type";
	/** Some value (generally associated to a name or key) */
	public static final String ATT_VALUE = "value";

	/////////////////////////////////////////////////////////////////
	// CUSTOM ELEMENTS		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Set of texts to be associated to categories */
	public static final String ELT_ACCEPT = "accept";
	/** Represents an article */
	public static final String ELT_ARTICLE = "article";
	/** Author of an article */
	public static final String ELT_AUTHOR = "author";
	/** List of article authors */
	public static final String ELT_AUTHORS = "authors";
	/** Category of article (military, scientist, etc.) */
	public static final String ELT_CATEGORY = "category";
	/** Editor configuration */
	public static final String ELT_CONFIGURATION = "configuration";
	/** Represents a corpus */
	public static final String ELT_CORPUS = "corpus";
	/** Dates associated to an article */
	public static final String ELT_DATES = "dates";
	/** Editor name */
	public static final String ELT_EDITOR = "editor";
	/** A list of named entities */
	public static final String ELT_ENTITIES = "entities";
	/** An entity in a list of named entities */
	public static final String ELT_ENTITY = "entity";
	/** Text expressions which must be ignored because of how they end, when retrieving categories */
	public static final String ELT_ENDS_WITH = "endsWith";
	/** One of the external ids (DBpedia, Freebase, etc.) of an entity */
	public static final String ELT_EXTERNAL_ID = "externalId";
	/** List of the external ids (DBpedia, Freebase, etc.) of an entity */
	public static final String ELT_EXTERNAL_IDS = "externalIds";
	/** A list of mentions */
	public static final String ELT_MENTIONS = "mentions";
	/** A mention in a list of mentions */
	public static final String ELT_MENTION = "mention";
	/** A group of GUI texts */
	public static final String ELT_GROUP = "group";
	/** Language of an article */
	public static final String ELT_LANGUAGE = "language";
	/** Last loaded values (editor) */
	public static final String ELT_LAST = "last";
	/** Date of modification of an article */
	public static final String ELT_MODIFICATION_DATE = "modification";
	/** Date of publishing of an article */
	public static final String ELT_PUBLISHING_DATE = "publishing";
	/** Various properties of an article */
	public static final String ELT_PROPERTIES = "properties";
	/** Set of texts not to be associated to categories */
	public static final String ELT_REJECT = "reject";
	/** Date of retrieval of an article */
	public static final String ELT_RETRIEVAL_DATE = "retrieval";
	/** Text expressions which must be ignored because of how they start, when retrieving categories */
	public static final String ELT_STARTS_WITH = "startsWith";
	/** String describing a mention */
	public static final String ELT_STRING = "string";
	/** One of the surface forms of an entity */
	public static final String ELT_SURFACE_FORM = "surfaceForm";
	/** List of the surface forms of an entity */
	public static final String ELT_SURFACE_FORMS = "surfaceForms";
	/** Text properties in the editor */
	public static final String ELT_TEXT = "text";
	/** Title of an article */
	public static final String ELT_TITLE = "title";
	/** Address of an article */
	public static final String ELT_URL = "url";
	/** Whether or not to use the last value */
	public static final String ELT_USE = "use";
	/** Actual value of a mention (might differ from its textual representation */
	public static final String ELT_VALUE = "value";
}
