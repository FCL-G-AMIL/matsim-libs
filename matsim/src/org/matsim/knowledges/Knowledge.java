/* *********************************************************************** *
 * project: org.matsim.*
 * Knowledge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.knowledges;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.basic.v01.BasicKnowledge;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;

public class Knowledge implements BasicKnowledge<ActivityOption> {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String desc = null;
	private static final int INIT_ACTIVITY_CAPACITY = 5;

	/**
	 * Contains all known {@link ActivityOption Activities} of a {@link PersonImpl}. Each activity can at most occur
	 * one time, independent of its {@code isPrimary} flag.
	 */
	private Set<KActivity> activities = null;
	private ArrayList<ActivitySpace> activitySpaces = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public Knowledge() {
	}

	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////

	/**
	 * Internal representation of of a pair consists of an {@link ActivityOption} and its {@code isPrimary} flag.
	 * Two {@link KActivity KActivities} are equal if the containing {@link ActivityOption Activities} are equal, independent
	 * of their {@code isPrimary} flag.
	 */
	private static class KActivity {
		/*package*/ boolean isPrimary;
		/*package*/ final ActivityOption activity;
		
		/*package*/ KActivity(ActivityOption activity, boolean isPrimary) {
			this.activity = activity;
			this.isPrimary = isPrimary;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof KActivity) {
				KActivity ka = (KActivity)obj;
				return ka.activity.equals(this.activity);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.activity.hashCode();
		}
	}
	

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final ActivitySpace createActivitySpace(final String type, final String act_type) {
		ActivitySpace asp = null;
		if (type.equals("ellipse")) {
			asp = new ActivitySpaceEllipse(act_type);
		} else if (type.equals("cassini")) {
			asp = new ActivitySpaceCassini(act_type);
		}else if (type.equals("superellipse")) {
			asp = new ActivitySpaceSuperEllipse(act_type);
		}else if (type.equals("bean")) {
			asp = new ActivitySpaceBean(act_type);
		} else {
			Gbl.errorMsg("[type="+type+" not allowed]");
		}
		if (this.activitySpaces == null) {
			this.activitySpaces = new ArrayList<ActivitySpace>(1);
		}
		this.activitySpaces.add(asp);
		return asp;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * <p>Adds an {@link ActivityOption} to a {@link PersonImpl Persons} {@link Knowledge}.
	 * It leaves the list of activities unchanged, if the given {@link ActivityOption} is already present,
	 * independent of the {@code isPrimary} flag.</p>
	 * 
	 * <p> Use {@link #setPrimaryFlag(ActivityOption, boolean)} to change the {@code isPrimary} flag of an already present {@link ActivityOption}.</p>
	 * 
	 * @param activity The {@link ActivityOption} to add to the {@link PersonImpl}s {@link Knowledge}.
	 * @param isPrimary To define if the {@code activity} is a primary activity
	 * @return <code>true</code> if the {@code activity} is not already present in the list (independent of the {@code isPrimary} flag)
	 */
	public final boolean addActivity(ActivityOption activity, boolean isPrimary) {
		if (activity == null) { return false; }
		if (activities == null) { activities = new LinkedHashSet<KActivity>(INIT_ACTIVITY_CAPACITY); }
		KActivity ka = new KActivity(activity,isPrimary);
		return activities.add(ka);
	}

	public void addActivity(ActivityOption activity) {
		this.addActivity(activity, false);
	}
	
	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Removes an {@link ActivityOption} from the {@link Knowledge} only if it is present and holds
	 * the given {@code isPrimary} flag.
	 * @param activity The {@link ActivityOption} to remove
	 * @param isPrimary The flag for which the <code>activity</code> is stored.
	 * @return <code>true</code> only if the <code>activity</code> is present and holds the given {@code isPrimary} flag.
	 */
	public final boolean removeActivity(ActivityOption activity, boolean isPrimary) {
		if (activity == null) { return false; }
		if (activities == null) { return false; }
		Iterator<KActivity> ka_it = activities.iterator();
		while (ka_it.hasNext()) {
			KActivity ka = ka_it.next();
			if (ka.activity.equals(activity) && (ka.isPrimary == isPrimary)) {
				ka_it.remove();
				if (activities.isEmpty()) { activities = null; }
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Removes an {@link ActivityOption} from the {@link Knowledge}, independent of the {@code isPrimary} flag.
	 * @param activity The {@link ActivityOption} to remove
	 * @return <code>true</code> if the <code>activity</code> was present successfully removed
	 */
	public final boolean removeActivity(ActivityOption activity) {
		if (activity == null) { return false; }
		if (activities == null) { return false; }
		boolean b = activities.remove(new KActivity(activity,false));
		if (activities.isEmpty()) { activities = null; }
		return b;
	}
	
	/**
	 * Removes all {@link ActivityOption Activities} that are equal to the given {@code isPrimary}
	 * @param isPrimary To define which of the {@link ActivityOption Activities} should be removed
	 * @return <code>true</code> only if the was at least one {@link ActivityOption} with given {@code isPrimary} to remove.
	 */
	public final boolean removeActivities(boolean isPrimary) {
		if (activities == null) { return false; }
		boolean b = false;
		Iterator<KActivity> ka_it = activities.iterator();
		while (ka_it.hasNext()) {
			KActivity ka = ka_it.next();
			if (ka.isPrimary == isPrimary) {
				ka_it.remove();
				b = true;
			}
		}
		if (activities.isEmpty()) { activities = null; }
		return b;
	}
	
	/**
	 * Removes all existing {@link ActivityOption Activities}.
	 * @return <code>true</code> if there was at least one {@link ActivityOption} given.
	 */
	public final boolean removeAllActivities() {
		if (activities == null) { return false; }
		activities.clear();
		activities = null;
		return true;
	}

	/**
	 * Removes all existing {@link ActivityOption Activities} of a given {@code act_type}
	 * @param act_type The activity type of {@link ActivityOption Activities} to remove.
	 * @return <code>true</code> if there was at least one {@link ActivityOption} of the given {@code act_type} removed.
	 */
	public final boolean removeActivities(final String act_type) {
		if (activities == null) { return false; }
		boolean b = false;
		Iterator<KActivity> ka_it = activities.iterator();
		while (ka_it.hasNext()) {
			KActivity ka = ka_it.next();
			if (ka.activity.getType().equals(act_type)) {
				ka_it.remove();
				b = true;
			}
		}
		if (activities.isEmpty()) { activities = null; }
		return b;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Returns all occurrences of an given {@link ActivityOption},
	 * that is part of a {@link ActivityFacility} with the given {@link Id}.
	 * @param facilityId The {@link Id} of a {@link ActivityFacility}
	 * @return The list of {@link ActivityOption Activities} that are part of the {@link Knowledge} and fulfill the above.
	 * The list can also be empty.
	 */
	public final ArrayList<ActivityOption> getActivities(Id facilityId) {
		if (activities == null) { return new ArrayList<ActivityOption>(0); }
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>(INIT_ACTIVITY_CAPACITY);
		for (KActivity ka : activities) {
			if (ka.activity.getFacility().getId().equals(facilityId)) {
				acts.add(ka.activity);
			}
		}
		return acts;
	}

	/**
	 * Returns all {@link ActivityOption Activities} of the given {@code isPrimary} flag.
	 * @param isPrimary To define which of the {@link ActivityOption Activities} should be returned
	 * @return The list of {@link ActivityOption Activities} of the given flag. The list may be empty
	 */
	public final ArrayList<ActivityOptionImpl> getActivities(boolean isPrimary) {
		if (activities == null) { return new ArrayList<ActivityOptionImpl>(0); }
		ArrayList<ActivityOptionImpl> acts = new ArrayList<ActivityOptionImpl>(INIT_ACTIVITY_CAPACITY);
		for (KActivity ka : activities) {
			if (ka.isPrimary == isPrimary) {
				acts.add((ActivityOptionImpl) ka.activity);
			}
		}
		return acts;
	}
	
	/**
	 * Returns all {@link ActivityOption Activities}.
	 * @return The list of {@link ActivityOption Activities}. The list may be empty.
	 */
	public final ArrayList<ActivityOption> getActivities() {
		if (activities == null) { return new ArrayList<ActivityOption>(0); }
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>(activities.size());
		for (KActivity ka : activities) {
			acts.add(ka.activity);
		}
		return acts;
	}

	/**
	 * Returns all {@link ActivityOption Activities} of a given activity type and a given flag
	 * @param act_type The activity type of the {@link ActivityOption Activities} should be returned
	 * @param isPrimary To define which of the {@link ActivityOption Activities} should be returned
	 * @return The list of {@link ActivityOption Activities}. The list may be empty.
	 */
	public final ArrayList<ActivityOption> getActivities(String act_type, boolean isPrimary) {
		if (activities == null) { return new ArrayList<ActivityOption>(0); }
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>(activities.size());
		for (KActivity ka : activities) {
			if ((ka.isPrimary == isPrimary) && (ka.activity.getType().equals(act_type))) {
				acts.add(ka.activity);
			}
		}
		return acts;
	}
	
	/**
	 * Returns all {@link ActivityOption Activities} of a given activity type
	 * @param act_type The activity type of the {@link ActivityOption Activities} should be returned
	 * @return The list of {@link ActivityOption Activities}. The list may be empty.
	 */
	public final ArrayList<ActivityOption> getActivities(final String act_type) {
		if (activities == null) { return new ArrayList<ActivityOption>(0); }
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>(activities.size());
		for (KActivity ka : activities) {
			if (ka.activity.getType().equals(act_type)) {
				acts.add(ka.activity);
			}
		}
		return acts;
	}
	
	/**
	 * Returns the set of activity types of the {@link ActivityOption Activities} with the given flag.
	 * @param isPrimary To define which of the activity types should be returned
	 * @return a set of activity types. The set may be empty.
	 */
	public final Set<String> getActivityTypes(boolean isPrimary) {
		if (activities == null) { return new TreeSet<String>(); }
		Set<String> types = new TreeSet<String>();
		for (KActivity ka : activities) {
			if (ka.isPrimary == isPrimary) {
				types.add(ka.activity.getType());
			}
		}
		return types;
	}

	/**
	 * Returns the set of activity types of the {@link ActivityOption Activities}.
	 * @return a set of activity types. The set may be empty.
	 */
	public final Set<String> getActivityTypes() {
		if (activities == null) { return new TreeSet<String>(); }
		Set<String> types = new TreeSet<String>();
		for (KActivity ka : activities) {
			types.add(ka.activity.getType());
		}
		return types;
	}
	
	
	/**
	 * Returns if a specific activity of a specific facility is primary
	 * @param act_type The activity type of the {@link ActivityOption Activities}
	 * @param facilityId The {@link Id} of a {@link ActivityFacility}
	 */
	public final boolean isPrimary(String act_type, Id facilityId) {
		if (activities == null) { 
			return false; 
		}
		for (KActivity ka : activities) {
			if ((ka.isPrimary) &&  (ka.activity.getType().equals(act_type)) &&
					(ka.activity.getFacility().getId().equals(facilityId))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns if act_type is defined as primary somewhere
	 * @param act_type The activity type of the {@link ActivityOption Activities}
	 */
	public final boolean isSomewherePrimary(String act_type) {
		if (activities == null) { 
			return false; 
		}
		for (KActivity ka : activities) {
			if ((ka.isPrimary) &&  (ka.activity.getType().equals(act_type))) {
				return true;
			}
		}
		return false;
	}
	
	

	public final String getDescription() {
		return this.desc;
	}

	/**
	 * @return List, may be null
	 */
	public final ArrayList<ActivitySpace> getActivitySpaces() {
		return this.activitySpaces;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Sets the activitySpaces to null
	 */
	public final void resetActivitySpaces(){
		this.activitySpaces = null;
	}
	
	/**
	 * Sets the {@code isPrimary} flag for the given {@link ActivityOption}
	 * @param activity the {@link ActivityOption} to set the flag
	 * @param isPrimary the flag
	 * @return <code>false</code> if the given {@link ActivityOption} does not exist.
	 */
	public final boolean setPrimaryFlag(ActivityOption activity, boolean isPrimary) {
		if (activities == null) { return false; }
		boolean found = false;
		for (KActivity ka : activities) {
			if (ka.activity.equals(activity)) {
				ka.isPrimary = isPrimary;
				found = true;
			}
		}
		return found;
	}

	/**
	 * Sets the {@code isPrimary} flag for all existing {@link ActivityOption Activities}
	 * @param isPrimary the flag
	 * @return <code>false</code> only if no {@link ActivityOption Activities} exist.
	 */
	public final boolean setPrimaryFlag(boolean isPrimary) {
		if (activities == null) { return false; }
		for (KActivity ka : activities) {
			ka.isPrimary = isPrimary;
		}
		return true;
	}
	
	public void setDescription(String desc) {
		this.desc = desc;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[desc=" + this.desc + "]" + "[nof_activities=" + this.activities.size() + "]";
	}
}
