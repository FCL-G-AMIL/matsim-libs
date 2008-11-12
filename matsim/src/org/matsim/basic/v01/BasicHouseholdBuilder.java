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

package org.matsim.basic.v01;

import java.util.List;

import org.matsim.interfaces.basic.v01.BasicHousehold;
import org.matsim.interfaces.basic.v01.HouseholdBuilder;

/**
 * @author dgrether
 */
public class BasicHouseholdBuilder implements HouseholdBuilder {

	private List<BasicHousehold> households;

	public BasicHouseholdBuilder(List<BasicHousehold> households) {
		this.households = households;
	}

	public List<BasicHousehold> getHouseholds() {
		return this.households;
	}

	public BasicHouseholdImpl createHousehold(Id householdId,
			List<Id> membersPersonIds, List<Id> vehicleIds) {
		BasicHouseholdImpl hh = new BasicHouseholdImpl(householdId);
		hh.setMemberIds(membersPersonIds);
		hh.setVehicleIds(vehicleIds);
		this.households.add(hh);
		return hh;
	}

}
