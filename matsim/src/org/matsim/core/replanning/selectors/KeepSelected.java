/* *********************************************************************** *
 * project: org.matsim.*
 * KeepSelected.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.selectors;

import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * Keeps the currently selected plan selected and returns it.
 * 
 * @author mrieser
 */
public class KeepSelected implements PlanSelector {

	/**
	 * returns the already selected plan for this person
	 */
	public PlanImpl selectPlan(PersonImpl person) {
		return person.getSelectedPlan();
	}

}
