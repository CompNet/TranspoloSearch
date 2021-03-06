package fr.univavignon.transpolosearch.data.entity;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.jdom2.Attribute;
import org.jdom2.Element;

import fr.univavignon.tools.log.HierarchicalLogger;
import fr.univavignon.tools.log.HierarchicalLoggerManager;

import fr.univavignon.transpolosearch.tools.xml.XmlNames;

/**
 * Abstract class representing a named entity, i.e. an entity
 * that can appears under different names.
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractNamedEntity extends AbstractEntity
{	
	/**
	 * General constructor for a named entity.
	 * 
	 * @param name
	 * 		Main string representation of the entity to create.
	 * @param internalId
	 * 		Internal id of the entity to create.
	 */
	public AbstractNamedEntity(String name, long internalId)
	{	super(internalId);
		
		this.name = name;
		surfaceForms.add(name);
	}
	
	/**
	 * Builds a named entity of the specified type, using the specified
	 * name and id.
	 * 
	 * @param internalId
	 * 		Id of the entity to build ({@code -1} to automatically define it 
	 * 		when inserting in an {@link Entities} object).
	 * @param name
	 * 		Name of the entity to build.
	 * @param type
	 * 		Entity type of the entity to build.
	 * @return
	 * 		The built entity.
	 */
	public static AbstractNamedEntity buildEntity(long internalId, String name, EntityType type)
	{	AbstractNamedEntity result = null;
		switch(type)
		{	case FUNCTION:
				result = new EntityFunction(name,internalId);
				break;
			case LOCATION:
				result = new EntityLocation(name,internalId);
				break;
			case MEETING:
				result = new EntityMeeting(name,internalId);
				break;
			case ORGANIZATION:
				result = new EntityOrganization(name,internalId);
				break;
			case PERSON:
				result = new EntityPerson(name,internalId);
				break;
			case PRODUCTION:
				result = new EntityProduction(name,internalId);
				break;
		}
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	private static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// MAIN NAME		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Main string representation of this entity */
	protected String name = null;
	
	/**
	 * Returns the main string representation of this entity.
	 * 
	 * @return
	 * 		Original string representation of this entity. 
	 */
	public String getName()
	{	return name;
	}

	/**
	 * Changes the main string representation of this entity.
	 * 
	 * @param name
	 * 		Change the main surface form of this entity.
	 */
	public void setName(String name)
	{	this.name = name;
	}
	
	/////////////////////////////////////////////////////////////////
	// SURFACE FORMS	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** All strings representing this entity */
	protected Set<String> surfaceForms = new TreeSet<String>();
	
	/**
	 * Returns all the strings representing this entity.
	 * 
	 * @return
	 * 		Set of strings representing this entity. 
	 */
	public Set<String> getSurfaceForms()
	{	return surfaceForms;
	}

	/**
	 * Add one name to this entity. If the name is already
	 * present, it is not added.
	 * 
	 * @param surfaceForm
	 * 		New name for this entity.
	 */
	public void addSurfaceForm(String surfaceForm)
	{	surfaceForms.add(surfaceForm);
	}
	
	/**
	 * Removes one of the names of this entity.
	 * 
	 * @param surfaceForm
	 * 		The name to remove.
	 */
	public void removeSurfaceForm(String surfaceForm)
	{	surfaceForms.remove(surfaceForm);
	}

	/////////////////////////////////////////////////////////////////
	// EXTERNAL IDS		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Internal ids of this entity */
	protected Map<String,String> externalIds = new HashMap<String,String>();
	
	/**
	 * Returns the requested external id of this entity, or {@code null}
	 * if none was defined.
	 * 
	 * @param knowledgeBase 
	 * 		Name of the concerned knowledge base.
	 * @return
	 * 		External id of this entity in the knowledge base. 
	 */
	public String getExternalId(String knowledgeBase)
	{	return externalIds.get(knowledgeBase);
	}
	
	/**
	 * Returns the maps of external ids of this entity.
	 * 
	 * @return
	 * 		External ids of this entity in the knowledge bases. 
	 */
	public Map<String,String> getExternalIds()
	{	return externalIds;
	}
	
	/**
	 * Changes the internal id of this entity, for
	 * the specified knowledge base.
	 * 
	 * @param knowledgeBase 
	 * 		Name of the knowledge base.
	 * @param externalId
	 * 		External id of this entity in the knowledge base.
	 */
	public void setExternalId(String knowledgeBase, String externalId)
	{	if(!KnowledgeBase.isRegistered(knowledgeBase))
			throw new IllegalArgumentException("String \""+knowledgeBase+"\" is not registered as a knowledge base name");
		else
			externalIds.put(knowledgeBase,externalId);
	}
	
	/**
	 * Checks whether the ids associated to this entity and the specified
	 * one intersect. In other words, if the entities have at least one
	 * id in common.
	 * 
	 * @param entity
	 * 		Entity to compare to this entity.
	 * @return
	 * 		{@code true} iff both entities have at least one id in common. 
	 */
	public boolean doExternalIdsIntersect(AbstractNamedEntity entity)
	{	boolean result = false;
		
		Iterator<Entry<String,String>> it = externalIds.entrySet().iterator(); 
		while(it.hasNext() && !result)
		{	Entry<String,String> entry = it.next();
			String key = entry.getKey();
			String value1 = entry.getValue();
			String value2 = entity.getExternalId(key);
			result = value2!=null && value1.equals(value2);
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// XML				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public Element exportAsElement()
	{	Element result = new Element(XmlNames.ELT_ENTITY);
		
		// set type
		{	Attribute typeAttr = new Attribute(XmlNames.ATT_TYPE, getType().toString());
			result.setAttribute(typeAttr);
		}
		// set internal id
		{	Attribute internalIdAttr = new Attribute(XmlNames.ATT_ID, Long.toString(internalId));
			result.setAttribute(internalIdAttr);
		}
		// set main name
		{	Attribute nameAttr = new Attribute(XmlNames.ATT_NAME, name);
			result.setAttribute(nameAttr);
		}
		// set surface forms
		{	Element surfaceFormsElt = new Element(XmlNames.ELT_SURFACE_FORMS);
			result.addContent(surfaceFormsElt);
			for(String surfaceForm: surfaceForms)
			{	Element surfaceFormElt = new Element(XmlNames.ELT_SURFACE_FORM);
				surfaceFormElt.setText(surfaceForm);
				surfaceFormsElt.addContent(surfaceFormElt);
			}
		}
		// external ids
		{	Element externalIdsElt = new Element(XmlNames.ELT_EXTERNAL_IDS);
			result.addContent(externalIdsElt);
			for(Entry<String,String> entry: externalIds.entrySet())
			{	// retrieve the data
				String kb = entry.getKey();
				String externalId = entry.getValue();
				// set up id element
				Element externalIdElt = new Element(XmlNames.ELT_EXTERNAL_ID);
				externalIdElt.setText(externalId);
				// set up knowledge base attribute
				Attribute kbAttr = new Attribute(XmlNames.ATT_KNOWLEDGE_BASE, kb);
				externalIdElt.setAttribute(kbAttr);
				// add to result
				externalIdsElt.addContent(externalIdElt);
			}
		}
		return result;
	}

	/**
	 * Builds a function entity from the specified
	 * XML element.
	 * 
	 * @param element
	 * 		XML element representing the entity.
	 * @param type
	 * 		Type of the entity to extract from the element.
	 * @return
	 * 		The entity built from the element.
	 */
	public static AbstractNamedEntity importFromElement(Element element, EntityType type)
	{	// get the id
		String internalIdStr = element.getAttributeValue(XmlNames.ATT_ID);
		long internalId = Long.parseLong(internalIdStr);
		
		// get the name
		String name = element.getAttributeValue(XmlNames.ATT_NAME);
		
		// build the entity
		AbstractNamedEntity result = AbstractNamedEntity.buildEntity(internalId, name, type);

		// get the surface forms
		{	Element surfaceFormsElt = element.getChild(XmlNames.ELT_SURFACE_FORMS);
			List<Element> surfaceFormList = surfaceFormsElt.getChildren();
			for(Element surfaceFormElt: surfaceFormList)
			{	String surfaceForm = surfaceFormElt.getText();
				result.addSurfaceForm(surfaceForm);
			}
		}
		// get the external ids
		{	Element externalIdsElt = element.getChild(XmlNames.ELT_EXTERNAL_IDS);
			List<Element> externalIdsList = externalIdsElt.getChildren();
			for(Element externalIdElt: externalIdsList)
			{	// external id
				String externalId = externalIdElt.getText();
				// knowledge base name
				String kb = element.getAttributeValue(XmlNames.ATT_KNOWLEDGE_BASE);
				// add to result
				result.setExternalId(kb,externalId);
			}
		}
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// ENTITIES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Complete this entity with the fields of the specified one, which
	 * is supposed to be equivalent. Concretely, we update the name and
	 * complete the surface forms and external ids.
	 * 
	 * @param entity
	 * 		Entity uses to complete this entity.
	 */
	public void completeWith(AbstractNamedEntity entity)
	{	// check the types
		EntityType type1 = getType();
		EntityType type2 = entity.getType();
		if(type1!=type2)
		{	//throw new IllegalArgumentException("Trying to merge entities of different types: "+type1+" vs. "+type2);
			logger.log("WARNING: Trying to merge entities of different types: "+type1+" vs. "+type2);
			logger.log(Arrays.asList(toString(),entity.toString()));
			//TODO for now, we just keep the old type
		}
		
		// possibly update the name
		if(entity.name.length() > name.length())
			name = entity.name;
		
		// complete the surface forms
		surfaceForms.addAll(entity.surfaceForms);
		
		// complete the external ids
		for(Entry<String,String> entry: entity.externalIds.entrySet())
		{	String kb = entry.getKey();
			String extId1 = externalIds.get(kb);
			String extId2 = entry.getValue();
			if(extId1==null)
				externalIds.put(kb, extId2);
			else if(!extId1.equals(extId2))
				throw new IllegalArgumentException("The specified entity has a different external id for KB "+kb+": "+extId1+" vs. "+extId2);
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// OBJECT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{	String result = getType().toString()+"(";
		result = result + "ID=" + internalId + "";
		result = result + ", NAME=\"" + name +"\"";
		if(!externalIds.isEmpty())
		{	Entry<String,String> entry = externalIds.entrySet().iterator().next();
			String kb = entry.getKey();
			String id = entry.getValue();
			result = result + ", " + kb + "=\"" + id +"\"";
		}
		result = result + ")";
		return result;
	}
}
