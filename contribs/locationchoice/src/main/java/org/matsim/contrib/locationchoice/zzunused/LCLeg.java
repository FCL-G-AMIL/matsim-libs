/* *********************************************************************** *
 * project: org.matsim.*
 * LCLeg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.zzunused;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author cdobler
 */
class LCLeg implements Leg, LCPlanElement {

	private final LCPlan plan;
	private final int arrayIndex;
	private final int planElementIndex;
	
	public LCLeg(final LCPlan plan, final int arrayIndex, final int planElementIndex) {
		this.plan = plan;
		this.arrayIndex = arrayIndex;
		this.planElementIndex = planElementIndex;
	}
	
	@Override
	public final String getMode() {
		return this.plan.modes[this.arrayIndex];
	}

	@Override
	public final void setMode(String mode) {
		this.plan.modes[this.arrayIndex] = mode;
	}

	@Override
	public final String getRoutingMode() {
		return this.plan.routingModes[this.arrayIndex];
	}

	@Override
	public final void setRoutingMode(String routingMode) {
		this.plan.routingModes[this.arrayIndex] = routingMode;
	}

	@Override
	public final Route getRoute() {
		return this.plan.routes[this.arrayIndex];
	}

	@Override
	public final void setRoute(Route route) {
		this.plan.routes[this.arrayIndex] = route;
	}

	@Override
	public final OptionalTime getDepartureTime() {
		return this.plan.depTimes[this.arrayIndex];
	}

	@Override
	public final void setDepartureTime(double seconds) {
		this.plan.depTimes[this.arrayIndex] = OptionalTime.defined(seconds);
	}

	@Override
	public void setDepartureTimeUndefined() {
		this.plan.depTimes[this.arrayIndex] = OptionalTime.undefined();
	}

	@Override
	public final OptionalTime getTravelTime() {
		return this.plan.travTimes[this.arrayIndex];
	}

	@Override
	public final void setTravelTime(double seconds) {
		this.plan.travTimes[this.arrayIndex] = OptionalTime.defined(seconds);
	}

	@Override
	public void setTravelTimeUndefined() {
		this.plan.travTimes[this.arrayIndex] = OptionalTime.undefined();
	}

	public final OptionalTime getArrivalTime() {
		return this.plan.arrTimes[this.arrayIndex];
	}
	
	public final void setArrivalTime(final double arrTime) {
		this.plan.arrTimes[this.arrayIndex] = OptionalTime.defined(arrTime);
	}

	@Override
	public int getArrayIndex() {
		return this.arrayIndex;
	}
	
	@Override
	public final int getPlanElementIndex() {
		return this.planElementIndex;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
