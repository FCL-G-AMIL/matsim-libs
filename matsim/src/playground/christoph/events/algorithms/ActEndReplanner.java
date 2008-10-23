/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndReplanner.java
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

package playground.christoph.events.algorithms;

import org.apache.log4j.Logger;
import org.matsim.events.ActEndEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.KnowledgePlansCalcRoute;

/*
 * This is a EventHandler that replans the Route of a Person every time an
 * Activity has ended. Alternatively it can also be called "by Hand".
 * 
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 * 
 * If a Person had just ended one Activity (ActEndEvent), a new Plan is created which 
 * contains this and the next Activity and the Leg between them.
 */

public class ActEndReplanner implements ActEndEventHandler {
	
	protected PlanAlgorithm replanner;
	protected Person person;
	protected Act fromAct;
	protected Leg betweenLeg;
	protected Act toAct;
	protected double time;
		
	private static final Logger log = Logger.getLogger(ActEndReplanner.class);

	// used when starting the Replanner "by hand"
	public ActEndReplanner(Act fromAct, Vehicle vehicle, double time, PlanAlgorithm replanner)
	{
		this.fromAct = fromAct;
		this.person = vehicle.getDriver().getPerson();
		this.time = time;
		this.replanner = replanner;
		
		Plan plan = person.getSelectedPlan();
		this.betweenLeg = plan.getNextLeg(fromAct);
	
		if(betweenLeg != null)
		{
			toAct = (Act)plan.getNextActivity(betweenLeg);
		}
		else 
		{
			toAct = null;
			log.error("An agents next activity is null - this should not happen!");
		}

		this.replanner = (PlanAlgorithm)person.getCustomAttributes().get("Replanner");
		if (replanner == null) log.error("No Replanner found in Person!");
		
		// calculate new Route
		if(toAct != null && replanner != null)
		{	
			doReplanning();
		}
	}
	
	// used when acting as EventHandler
	public ActEndReplanner()
	{	
	}
	
	// used when acting as EventHandler
	public void handleEvent(ActEndEvent event) {
		// TODO Auto-generated method stub

		// If replanning flag is set in the Person
		boolean replanning = (Boolean)event.agent.getCustomAttributes().get("endActivityReplanning");
		if(replanning) 
		{
			this.time = event.time;
			this.person = event.agent;
			this.fromAct = event.act;
			
			Plan plan = person.getSelectedPlan();
			this.betweenLeg = plan.getNextLeg(fromAct);
			
			if(betweenLeg != null)
			{
				toAct = (Act)plan.getNextActivity(betweenLeg);
			}
			else 
			{
				toAct = null;
				log.error("An agents next activity is null - this should not happen!");
			}

			this.replanner = (PlanAlgorithm)person.getCustomAttributes().get("Replanner");
			if (replanner == null) log.error("No Replanner found in Person!");
			
//			log.info("ActEndReplanner....................." + event.agentId);
			
			// calculate new Route
			if(toAct != null && replanner != null)
			{	
				doReplanning();
			}
		}
	}

	
	public void reset(int iteration) {
		// TODO Auto-generated method stub	
	}
	
	/*
	 * Idea:
	 * MATSim Routers create Routes for complete plans.
	 * We just want to replan the Route from one Activity to another one, so we create
	 * a new Plan that contains only this Route. This Plan is replanned by sending it
	 * to the Router.
	 *
	 * Attention! The Replanner is started when the Activity of a Person ends and the Vehicle 
	 * is added to the Waiting List of its QueueLink. That means that a Person replans 
	 * his Route at time A but enters the QueueLink at time B.
	 * A short example: If all Persons of a network end their Activities at the same time 
	 * and have the same Start- and Endpoints of their Routes they will all use the same
	 * route (if they use the same router). If they would replan their routes when they really
	 * Enter the QueueLink this would not happen because the enter times would be different
	 * due to the limited number of possible vehicles on a link at a time. An implementation
	 * of such a functionality would be a problem due to the structure of MATSim...
	 */
//	protected void Routing(Act fromAct, Leg nextLeg)
	protected void doReplanning()
	{	
		// Replace the EndTime with the current time - do we really need this?
//		fromAct.setEndTime(time);
		
		// save currently selected plan
		Plan currentPlan = person.getSelectedPlan();
		
		// Create new Plan and select it.
		// This Plan contains only the just ended and the next Activity.
		// -> That's the only Part of the Route that we want to replan.
		Plan newPlan = new Plan(person);
		person.setSelectedPlan(newPlan);
		
		// Here we are at the moment.
		newPlan.addAct(fromAct);
		
		// Current Route between fromAct and toAct - this Route shall be replanned.
		newPlan.addLeg(betweenLeg);
		
		// We still want to go there...
		newPlan.addAct(toAct);
			
		/*
		 *  If it's a PersonPlansCalcRoute Object -> set the current Person.
		 *  The router may need their knowledge (activity room, ...).
		 */
		if (replanner instanceof KnowledgePlansCalcRoute)
		{
			((KnowledgePlansCalcRoute)replanner).setPerson(this.person);
			((KnowledgePlansCalcRoute)replanner).setTime(this.time);
		}
		
		// By doing the replanning the "betweenLeg" is replanned, so the changes are
		// included in the previously selected plan, too!
		replanner.run(newPlan);
		
		// reactivate previously selected, replanned plan
		person.setSelectedPlan(currentPlan);
	}
	
}