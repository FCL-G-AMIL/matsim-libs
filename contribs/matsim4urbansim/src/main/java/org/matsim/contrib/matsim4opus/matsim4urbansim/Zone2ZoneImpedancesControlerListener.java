/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.matsim4opus.matsim4urbansim;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.matsim4opus.config.ConfigurationUtils;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.ZoneUtil;
import org.matsim.contrib.matsim4opus.improvedpseudopt.PtMatrix;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import org.matsim.contrib.matsim4opus.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.helperObjects.ZoneObject;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.LeastCostPathTree;

/**
 * This controller version is designed for the sustaincity mile stone (Month 18).
 * 
 * improvements / changes march'12
 * - This controler works for UrbanSim Zone and Parcel Applications
 * 
 * improvements / changes aug'12
 * - added calculation of free speed car and bike travel times
 * 
 * improvements jan'13
 * - added pt for accessibility calculation
 * - added vehicle travel distance output
 * 
 * improvements feb'13
 * - trip matrix is now sensitive for time of day
 * 
 * improvements april'13
 * - trips are scaled up to 100%
 * - taking disutilites directly from MATSim (controler.createTravelCostCalculator()), this 
 * also activates road pricing ...
 * 
 * @author nagel
 * @author thomas
 *
 */
public class Zone2ZoneImpedancesControlerListener implements ShutdownListener {
	private static final Logger log = Logger.getLogger(Zone2ZoneImpedancesControlerListener.class);

	public static final String FILE_NAME = "travel_data.csv";
	
	private MATSim4UrbanSimInterface main;
	private ActivityFacilitiesImpl zones;
	private ActivityFacilitiesImpl parcels;
	private String travelDataPath;
	private PtMatrix ptMatrix;		// this could be zero of no input files for pseudo pt are given ...
	private Benchmark benchmark;

	/**
	 * constructor	
	 * @param zones 
	 * @param parcels
	 */
	public Zone2ZoneImpedancesControlerListener( final MATSim4UrbanSimInterface main, final ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels, PtMatrix ptMatrix, Benchmark benchmark) {
		
		assert(main != null);
		this.main = main;
		assert(zones != null);
		this.zones = zones;
		assert(parcels != null);
		this.parcels = parcels;
		this.travelDataPath = InternalConstants.MATSIM_4_OPUS_TEMP + FILE_NAME;
		this.ptMatrix = ptMatrix;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
	}
	
	/**
	 *	calculating and dumping the following outcomes:
	 *  - zone2zone impedances including travel times, travel costs and am walk time
	 *  - logsum computation of workplace accessibility
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );

		int benchmarkID = this.benchmark.addMeasure("zone-to-zone impedances");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		
//		AccessibilityParameterConfigModule moduleAPCM = ConfigurationModule.getAccessibilityParameterConfigModule((ScenarioImpl)sc);
//		boolean usingCarParameterFromMATSim = moduleAPCM.usingCarParameterFromMATSim();
//		double betaCarTT 	   	= moduleAPCM.getBetaCarTravelTime();
//		double betaCarTD		= moduleAPCM.getBetaCarTravelDistance();
//		double betaCarTMC		= moduleAPCM.getBetaCarTravelMonetaryCost();
		
		double samplingRate = ConfigurationUtils.getUrbanSimParameterConfigModule((ScenarioImpl)sc).getPopulationSampleRate();
		double inverseOfSamplingRate = 1/samplingRate;
		
		double walkSpeedMeterPerMinute = sc.getConfig().plansCalcRoute().getWalkSpeed() * 60.; // corresponds to 5 km/h
		double bikeSpeedMeterPerMinute = sc.getConfig().plansCalcRoute().getBikeSpeed() * 60.; // corresponds to 15 km/h 

//		// init least cost path tree in order to calculate travel times and travel costs
//		TravelTime ttc = controler.getLinkTravelTimes();
//		// get the free-speed car travel times (in seconds)
//		LeastCostPathTreeExtended lcptExtFreeSpeedCarTrvelTime = null;
//		// get the congested car travel time (in seconds)
//		LeastCostPathTreeExtended  lcptExtCongestedCarTravelTime = null;
//		// get travel distance (in meter)
//		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
//
//		NetworkImpl network = (NetworkImpl) controler.getNetwork() ;
//		double depatureTime = this.main.getTimeOfDay(); // 8.*3600;
//		
//		// od-trip matrix (zonal based)
//		Matrix originDestinationMatrix = new Matrix("tripMatrix", "Zone to Zone origin destination trip matrix");
//		
//		// get road pricing scheme
//		RoadPricingSchemeImpl scheme = controler.getScenario().getScenarioElement(RoadPricingSchemeImpl.class);
//
//		// init extended Least Cost Path Tree for car (free speed and congested) mode
//		if(usingCarParameterFromMATSim){
//
//			lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended( ttc, controler.createTravelCostCalculator(), controler);
//			
//			if(scheme == null)	// no road pricing
//				lcptExtFreeSpeedCarTrvelTime = new LeastCostPathTreeExtended( ttc, new FreespeedTravelTimeAndDisutility(controler.getConfig().planCalcScore()), controler );
//			else{				// if road pricing is activated
//				TravelDisutility costCalculatorFreeSpeed = new FreespeedTravelTimeAndDisutility(controler.getConfig().planCalcScore());
//				lcptExtFreeSpeedCarTrvelTime  = new LeastCostPathTreeExtended( ttc, new TravelDisutilityIncludingToll(costCalculatorFreeSpeed, scheme), controler );
//			}
//		}
//		else{					
//			TravelDisutilityFactory tdFactory = controler.getTravelDisutilityFactory();
//			PlanCalcScoreConfigGroup cnScoringGroup = new PlanCalcScoreConfigGroup();
//			cnScoringGroup.setMarginalUtilityOfMoney( betaCarTMC );
//			cnScoringGroup.setTraveling_utils_hr( betaCarTT );	
//			cnScoringGroup.setMonetaryDistanceCostRateCar( betaCarTD );
//			cnScoringGroup.setConstantCar(controler.getConfig().planCalcScore().getConstantCar());
//			
//			if(scheme == null){// no road pricing
//				lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended( ttc, tdFactory.createTravelDisutility(ttc, cnScoringGroup), controler);
//				lcptExtFreeSpeedCarTrvelTime  = new LeastCostPathTreeExtended( ttc, new FreespeedTravelTimeAndDisutility(cnScoringGroup), controler);
//			}
//			else{ 				// if road pricing is activated
//				TravelDisutility costCalculatorCongested = tdFactory.createTravelDisutility(ttc, cnScoringGroup); 
//				TravelDisutility costCalculatorFreeSpeed = new FreespeedTravelTimeAndDisutility(controler.getConfig().planCalcScore());
//				
//				lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended( ttc, new TravelDisutilityIncludingToll(costCalculatorCongested, scheme), controler);
//				lcptExtFreeSpeedCarTrvelTime  = new LeastCostPathTreeExtended( ttc, new TravelDisutilityIncludingToll(costCalculatorFreeSpeed, scheme), controler );
//			}
//		}
		
		// get the free-speed car travel times (in seconds)
		TravelTime ttf = new FreeSpeedTravelTime() ;
		TravelDisutility tdFree = controler.getTravelDisutilityFactory().createTravelDisutility(ttf, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended lcptExtFreeSpeedCarTrvelTime = new LeastCostPathTreeExtended( ttf, tdFree, controler.getScenario().getScenarioElement(RoadPricingSchemeImpl.class) ) ;
		
		// get the congested car travel time (in seconds)
		TravelTime ttc = controler.getLinkTravelTimes(); // congested
		TravelDisutility tdCongested = controler.getTravelDisutilityFactory().createTravelDisutility(ttc, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended  lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended(ttc, tdCongested, controler.getScenario().getScenarioElement(RoadPricingSchemeImpl.class) ) ;

		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttf, new TravelDistanceCalculator());
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork() ;
		double depatureTime = this.main.getTimeOfDay(); // configurable time of day, by default this is 8.*3600;
		
		// od-trip matrix (zonal based)
		Matrix originDestinationMatrix = new Matrix("tripMatrix", "Zone to Zone origin destination trip matrix");
		
		try {
			BufferedWriter travelDataWriter = initZone2ZoneImpedaceWriter(); // creating zone-to-zone impedance matrix with header
			
			computeZoneToZoneTrips(sc, originDestinationMatrix, depatureTime);

			log.info("Computing and writing zone2zone impedance matrix ..." );

			// init array with zone informations
			ZoneObject[] zones = ZoneUtil.mapZoneCentroid2NearestNode(this.zones, network);
			// init progress bar
			ProgressBar bar = new ProgressBar( zones.length );
			log.info("Processing " + zones.length + " UrbanSim zones ...");

			// main for loop, dumping out zone2zone impedances (travel times)
			for(int fromZoneIndex = 0; fromZoneIndex < zones.length; fromZoneIndex++){
				
				// progress bar
				bar.update();
				
				// get nearest network node and zone id for origin zone
				Node fromNode = zones[fromZoneIndex].getNearestNode();
				Id originZoneID = zones[fromZoneIndex].getZoneID();
				
				// run dijksrtra for current node as origin
				lcptExtCongestedCarTravelTime.calculateExtended(network, fromNode, depatureTime);
				lcptExtFreeSpeedCarTrvelTime.calculateExtended(network, fromNode, depatureTime);
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				for(int toZoneIndex = 0; toZoneIndex < zones.length; toZoneIndex++){
					
					// get nearest network node and zone id for destination zone
					Node toNode = zones[toZoneIndex].getNearestNode();
					Id destinationZoneID = zones[toZoneIndex].getZoneID();
					
					// free speed car travel times in minutes
					double arrivalTimeFS = lcptExtFreeSpeedCarTrvelTime.getTree().get( toNode.getId() ).getTime();
					double freeSpeedTravelTime_min = (arrivalTimeFS - depatureTime) / 60.;
					// we guess that any value less than 1.2 leads to errors on the UrbanSim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					if(freeSpeedTravelTime_min < 1.2)
						freeSpeedTravelTime_min = 1.2;
					
					// get generalized travel cost 
					double travelDisutility_util = lcptExtCongestedCarTravelTime.getTree().get( toNode.getId() ).getCost();
					// we guess that any value less than 1.2 leads to errors on the UrbanSim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					// if(travelDisutility_util < 1.2)
					// 	travelDisutility_util = 1.2;
					
					// get congested arrival time
					double arrivalTimeCC = lcptExtCongestedCarTravelTime.getTree().get( toNode.getId() ).getTime();
					// congested car travel times in minutes
					double congestedTravelTime_min = (arrivalTimeCC - depatureTime) / 60.;
					// we guess that any value less than 1.2 leads to errors on the UrbanSim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					if(congestedTravelTime_min < 1.2)
						congestedTravelTime_min = 1.2;

					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( toNode.getId() ).getCost();
					double bikeTravelTime_min = travelDistance_meter / bikeSpeedMeterPerMinute;
					// we guess that any value less than 1.2 leads to errors on the UrbanSim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					if(bikeTravelTime_min < 1.2)
						bikeTravelTime_min = 1.2;
					double walkTravelTime_min = travelDistance_meter / walkSpeedMeterPerMinute;
					// we guess that any value less than 1.2 leads to errors on the UrbanSim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					if(walkTravelTime_min < 12.)
						walkTravelTime_min = 12.;
					
					// pt travel times in minutes
					double ptTravelTime_min = -1.;
					if(this.ptMatrix != null){
						ptTravelTime_min = this.ptMatrix.getTotalTravelTime_seconds(fromNode.getCoord(), toNode.getCoord()) / 60.;
						// we guess that any value less than 1.2 leads to errors on the UrbanSim side
						// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
						if(ptTravelTime_min < 1.2)
							ptTravelTime_min = 1.2;
					}

					// query trips in OD Matrix
					double trips = 0.0;
					Entry e = originDestinationMatrix.getEntry( originZoneID, destinationZoneID );
					if(e != null) // trips are scaled to 100%
						trips = e.getValue() * inverseOfSamplingRate;
					
					// IMPORTANT: Do adapt the travel_data header in "initZone2ZoneImpedaceWriter"
					// 			  when changing anything at this call.
					travelDataWriter.write ( originZoneID.toString()			//origin zone id
										+ "," + destinationZoneID.toString()	//destination zone id
										+ "," + freeSpeedTravelTime_min			//free speed travel times
										+ "," + travelDisutility_util 			//congested generalized cost
										+ "," + congestedTravelTime_min 		//congested travel times
										+ "," + bikeTravelTime_min				//bike travel times
										+ "," + walkTravelTime_min				//walk travel times
										+ "," + ptTravelTime_min				//pt travel times
										+ "," + travelDistance_meter			//network distance
										+ "," + trips);							//vehicle trips (scaled to 100%)
					travelDataWriter.newLine();
				}
			}

			// finish progress bar
			System.out.println();
			// flush and close writers
			travelDataWriter.flush();
			travelDataWriter.close();
			log.info("... done with writing travel_data.csv" );
			
			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Zone-to-zone impedance measure with " 
						 + zones.length + " zones took "
						 + this.benchmark.getDurationInSeconds(benchmarkID)
						 + " seconds ("
						 + this.benchmark.getDurationInSeconds(benchmarkID) / 60. 
						 + " minutes).");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("... done with notifyShutdown.") ;
	}

	/**
	 * Returns a BufferedWriter writing a zone-to-zone impedance matrix for UrbanSim
	 * 
	 * @return BufferedWriter
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private BufferedWriter initZone2ZoneImpedaceWriter()
			throws FileNotFoundException, IOException {
		BufferedWriter travelDataWriter = IOUtils.getBufferedWriter( travelDataPath );
		
		// Travel Data Header
		travelDataWriter.write ( "from_zone_id:i4,to_zone_id:i4,vehicle_free_speed_travel_time:f4,single_vehicle_to_work_travel_cost:f4,am_single_vehicle_to_work_travel_time:f4,bike_time_in_minutes:f4,walk_time_in_minutes:f4,pt_time_in_minutes:f4,vehicle_travel_distance_in_meter:f4,am_pk_period_drive_alone_vehicle_trips:f4" ) ; 
		// Logger.getLogger(this.getClass()).warn( "add new fields (this message is shown until all travel data attributes are updated)" );
		travelDataWriter.newLine();
		return travelDataWriter;
	}
	
	/**
	 * goes through all person and their plans and stores their trips into a matrix
	 * 
	 * @param sc
	 * @param originDestinationMatrix
	 */
	private void computeZoneToZoneTrips(Scenario sc, Matrix originDestinationMatrix, double timeOfDay){
		log.info("Computing zone2zone trip numbers ...") ;
		// yyyy might make even more sense to do this via events.  kai, feb'11
		Entry matrixEntry = null;
		
		Map<Id, ActivityFacility> allFacilities; 
		if(parcels == null)
			allFacilities = zones.getFacilities();	// used for UrbanSim Zone Models
		else
			allFacilities = parcels.getFacilities();// used for UrbanSim Parcel Models
		
		for ( Person person : sc.getPopulation().getPersons().values() ) {

			Plan plan = person.getSelectedPlan() ;
			
			boolean isFirstPlanActivity = true;
			String lastZoneId = null;
			
			if(plan.getPlanElements().size() <= 1) // check if activities available (then size of plan elements > 1)
				continue;
			
			boolean enterTripInODMatrix = true;
			
			for ( PlanElement pe : plan.getPlanElements() ) {
				
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe;
					Id id = act.getFacilityId();
					if( id == null) // that person plan doesn't contain any activity, continue with next person
						continue;
					
					ActivityFacility fac = allFacilities.get(id);
					if(fac == null)
						continue;
					String zone_ID = ((Id) fac.getCustomAttributes().get(InternalConstants.ZONE_ID)).toString();

					if (isFirstPlanActivity)
						isFirstPlanActivity = false; 
					else if (enterTripInODMatrix){
						matrixEntry = originDestinationMatrix.getEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID));
						if(matrixEntry != null){
							double trips = matrixEntry.getValue() + 1.;
							originDestinationMatrix.setEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID), trips);
						}
						else	
							originDestinationMatrix.createEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID), 1.);
							// see PtPlanToPlanStepBasedOnEvents.addPersonToVehicleContainer (or zone coordinate addition)
					}
					lastZoneId = zone_ID; // stores the first activity (e. g. "home")
				}
//				else if(pe instanceof Leg){ // tnicolai: enable this part to get time dependent trips
//					
//					Leg leg = (Leg)pe;
//					
//					double departureTime = leg.getDepartureTime();
//					double arrivalTime = departureTime + leg.getTravelTime();
//					
//					enterTripInODMatrix = (timeOfDay >= departureTime && timeOfDay <= arrivalTime);
//				}
			}
		}
		// tnicolai: debugging
//		double originCounter = 0.;
//		double destinationCounter = 0.;
//		for(Id fromId : originDestinationMatrix.getFromLocations().keySet()){
//			log.info("From Zone: " + fromId.toString());
//			for(Entry e : originDestinationMatrix.getFromLocEntries(fromId)){
//				originCounter += e.getValue();
//				log.info("To Zone: " + e.getToLocation() + " value = " + e.getValue());
//			}
//		}
//		
//		for(Id ToId : originDestinationMatrix.getToLocations().keySet()){
//			log.info("To Zone: " + ToId.toString());
//			for(Entry e : originDestinationMatrix.getToLocEntries(ToId)){
//				destinationCounter += e.getValue();
//				log.info("From Zone: " + e.getFromLocation() + " value = " + e.getValue());
//			}
//		}
//		log.info("Home-to-work trips: " + originCounter);
//		log.info("Work-to-Home trips: " + destinationCounter);
//		log.info("Total trips: " + (originCounter + destinationCounter));
		log.info("DONE with computing zone2zone trip numbers ...") ;
	}
}
