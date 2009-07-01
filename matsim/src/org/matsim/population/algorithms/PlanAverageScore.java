/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAverageScore.java
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

package org.matsim.population.algorithms;

import java.util.Iterator;

import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

public class PlanAverageScore extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private double sumScores = 0.0;
	private long cntScores = 0;
	
	public PlanAverageScore() {
		super();
	}
	
	@Override
	public void run(PersonImpl person) {
		Iterator<PlanImpl> iter = person.getPlans().iterator();
		while (iter.hasNext()) {
			PlanImpl plan = iter.next();
			if (plan.isSelected()) {
				run(plan);
			}
		}
	}
	
	public final void run(PlanImpl plan) {
		Double score = plan.getScore();

		if ((score != null) && (!score.isInfinite()) && (!score.isNaN())) {
			sumScores += score.doubleValue();
			cntScores++;
		}
	}
	
	public final double getAverage() {
		return (sumScores / cntScores);
	}
	
	public final long getCount() {
		return cntScores;
	}
	
}

