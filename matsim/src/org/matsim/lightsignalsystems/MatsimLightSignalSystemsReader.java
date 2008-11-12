/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.lightsignalsystems;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;

import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.matsim.basic.lightsignalsystems.BasicLane;
import org.matsim.basic.lightsignalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.lightsignalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.lightsignalsystems.BasicLightSignalSystemDefinition;
import org.matsim.basic.lightsignalsystems.BasicLightSignalSystems;
import org.matsim.basic.lightsignalsystems.BasicLightSignalSystemsFactory;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.xml.lightsignalsystems.ObjectFactory;
import org.matsim.basic.xml.lightsignalsystems.XMLIdRefType;
import org.matsim.basic.xml.lightsignalsystems.XMLLaneType;
import org.matsim.basic.xml.lightsignalsystems.XMLLanesToLinkAssignmentType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalGroupDefinitionType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalSystemDefinitionType;
import org.matsim.basic.xml.lightsignalsystems.XMLLightSignalSystems;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class MatsimLightSignalSystemsReader {
	
	private BasicLightSignalSystems lightSignalSystems;
   
  private BasicLightSignalSystemsFactory factory = new BasicLightSignalSystemsFactory();

	
	public MatsimLightSignalSystemsReader(BasicLightSignalSystems lightSignalSystems) {
		this.lightSignalSystems = lightSignalSystems;
	}
	
	
	public void readFile(final String filename) {
  	JAXBContext jc;
    XMLLightSignalSystems xmlLssDefinition;
		try {
			jc = JAXBContext.newInstance(org.matsim.basic.xml.lightsignalsystems.ObjectFactory.class);
			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
			Schema schema = schemaFac.newSchema(new URL("http://www.matsim.org/files/dtd/lightSignalSystems_v1.0.xsd"));
			u.setSchema(schema);
			xmlLssDefinition = (XMLLightSignalSystems)u.unmarshal( 
					new FileInputStream( filename ) );

			BasicLanesToLinkAssignment l2lAssignment;
			BasicLane lane = null;
			for (XMLLanesToLinkAssignmentType lldef : xmlLssDefinition.getLanesToLinkAssignment()) {
				l2lAssignment = factory.createLanesToLinkAssignment(new IdImpl(lldef.getLinkIdRef()));
				for (XMLLaneType laneType : lldef.getLane()) {
					lane = factory.createLane(new IdImpl(laneType.getId()));
					for (XMLIdRefType toLinkId : laneType.getToLink()) {
						lane.addToLinkId(new IdImpl(toLinkId.getRefId()));
					}
					if (laneType.getRepresentedLanes() == null) {
						laneType.setRepresentedLanes(fac.createXMLLaneTypeXMLRepresentedLanes());
					}
					lane.setNumberOfRepresentedLanes(laneType.getRepresentedLanes().getNumber());
					if (laneType.getLength() == null) {
						laneType.setLength(fac.createXMLLaneTypeXMLLength());
					}
					lane.setLength(laneType.getLength().getMeter());
					l2lAssignment.addLane(lane);
				}
				lightSignalSystems.addLanesToLinkAssignment(l2lAssignment);
			}
			
			BasicLightSignalSystemDefinition lssdef;
			for (XMLLightSignalSystemDefinitionType xmllssDef : xmlLssDefinition.getLightSignalSystemDefinition()) {
				lssdef = factory.createLightSignalSystemDefinition(new IdImpl(xmllssDef.getId()));
				lssdef.setDefaultCirculationTime(xmllssDef.getDefaultCirculationTime().getSeconds());
				lssdef.setDefaultInterimTime(xmllssDef.getDefaultInterimTime().getSeconds());
				lssdef.setDefaultSyncronizationOffset(xmllssDef.getDefaultSyncronizationOffset().getSeconds());
				lightSignalSystems.addLightSignalSystemDefinition(lssdef);
			}
			//parsing lightSignalGroupDefinitions
			BasicLightSignalGroupDefinition lsgdef;
			for (XMLLightSignalGroupDefinitionType xmllsgdef : xmlLssDefinition.getLightSignalGroupDefinition()) {
				lsgdef = factory.createLightSignalGroupDefinition(new IdImpl(xmllsgdef.getLinkIdRef()), new IdImpl(xmllsgdef.getId()));
				lsgdef.setLightSignalSystemDefinitionId(new IdImpl(xmllsgdef.getLightSignalSystemDefinition().getRefId()));
				for (XMLIdRefType refIds : xmllsgdef.getLane()) {
					lsgdef.addLaneId(new IdImpl(refIds.getRefId()));
				}
				for (XMLIdRefType refIds : xmllsgdef.getToLink()) {
					lsgdef.addToLinkId(new IdImpl(refIds.getRefId()));
				}
				lightSignalSystems.addLightSignalGroupDefinition(lsgdef);
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			 e.printStackTrace();
		 } catch (SAXException e) {
			 e.printStackTrace();
		} 

	}
}
