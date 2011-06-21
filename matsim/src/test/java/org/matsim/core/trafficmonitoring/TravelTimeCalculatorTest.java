/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorTest.java
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

package org.matsim.core.trafficmonitoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class TravelTimeCalculatorTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(TravelTimeCalculatorTest.class);

	public final void testTravelTimeCalculator_Array_Optimistic() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_Array_Optimistic_LinearInterpolation() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		aggregator.setTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic_LinearInterpolation() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		aggregator.setTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Pessimistic() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new PessimisticTravelTimeAggregator(binSize, numSlots);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Pessimistic_LinearInterpolation() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic_linearinterpolation.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new PessimisticTravelTimeAggregator(binSize, numSlots);
		aggregator.setTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	private final void doTravelTimeCalculatorTest(final ScenarioImpl scenario, final TravelTimeDataFactory ttDataFactory,
			final AbstractTravelTimeAggregator aggregator, final int timeBinSize,
			final String compareFile, final boolean generateNewData) throws IOException {
		String networkFile = getClassInputDirectory() + "link10_network.xml";
		String eventsFile = getClassInputDirectory() + "link10_events.txt";

		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		new MatsimEventsReader(events).readFile(eventsFile);
		events.printEventsCount();

		EventsManager events2 = EventsUtils.createEventsManager();

		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 30*3600, scenario.getConfig().travelTimeCalculator());
		ttcalc.setTravelTimeAggregator(aggregator);
		ttcalc.setTravelTimeDataFactory(ttDataFactory);
		events2.addHandler(ttcalc);
		for (Event e : collector.getEvents()) {
			events2.processEvent(e);
		}

		// read comparison data
		BufferedReader infile = new BufferedReader(new FileReader(compareFile));
		String line;
		String[] compareData = new String[4*24];
		try {
			for (int i = 0; i < 4*24; i++) {
				line = infile.readLine();
				compareData[i] = line;
			}
		}
		finally {
			try {
				infile.close();
			} catch (IOException e) {
				log.error("could not close stream.", e);
			}
		}

		// prepare comparison
		Link link10 = network.getLinks().get(new IdImpl("10"));

		if (generateNewData) {
			BufferedWriter outfile = null;
			try {
				outfile = new BufferedWriter(new FileWriter(compareFile));
				for (int i = 0; i < 4*24; i++) {
					double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
					outfile.write(Double.toString(ttime) + "\n");
				}
			}
			finally {
				if (outfile != null) {
					try {
						outfile.close();
					} catch (IOException ignored) {}
				}
			}
			fail("A new file containg data for comparison was created. No comparison was made.");
		}

		// do comparison
		for (int i = 0; i < 4*24; i++) {
			double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}

	/**
	 * @author mrieser
	 */
	public void testLongTravelTimeInEmptySlot() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Link link1 = network.createAndAddLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		PersonImpl person = new PersonImpl(new IdImpl(1));

		// generate some events that suggest a really long travel time
		double linkEnterTime1 = 7.0 * 3600 + 10;
		double linkTravelTime1 = 50.0 * 60; // 50minutes!
		double linkEnterTime2 = 7.75 * 3600 + 10;
		double linkTravelTime2 = 10.0 * 60; // 10minutes!

		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime1, person.getId(), link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEventImpl(linkEnterTime1 + linkTravelTime1, person.getId(), link1.getId()));
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime2, person.getId(), link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEventImpl(linkEnterTime2 + linkTravelTime2, person.getId(), link1.getId()));

		assertEquals(50 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize
	}

	/**
	 * Tests that calculating LinkTravelTimes works also without reading in a complete scenario including population.
	 *
	 * @author mrieser
	 *
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void testReadFromFile_LargeScenarioCase() throws SAXException, ParserConfigurationException, IOException {
		/* Assume, you have a big events file from a huge scenario and you want to do data-mining...
		 * Then you likely want to calculate link travel times. This requires the network, but NOT
		 * the population. Thus, using "new Events(new EventsBuilderImpl(scenario))" is not appropriate
		 * and may not even be usable if the population is too big to read it in on a laptop for
		 * post-processing. So, it must be possible to only read the network an the events and still
		 * calculate link travel times.
		 */
		String eventsFilename = getClassInputDirectory() + "link10_events.txt";
		String networkFile = "test/scenarios/equil/network.xml";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).parse(networkFile);

		EventsManager events = EventsUtils.createEventsManager(); // DO NOT USE EventsBuilderImpl() here, as we do not have a population!

		TravelTimeCalculator ttCalc = new TravelTimeCalculator(network, config.travelTimeCalculator());
		events.addHandler(ttCalc);

		new MatsimEventsReader(events).readFile(eventsFilename);

		Link link10 = network.getLinks().get(new IdImpl("10"));

		assertEquals("wrong link travel time at 06:00.", 110.0, ttCalc.getLinkTravelTime(link10, 6.0 * 3600), EPSILON);
		assertEquals("wrong link travel time at 06:15.", 359.9712023038157, ttCalc.getLinkTravelTime(link10, 6.25 * 3600), EPSILON);
	}

	/**
	 * @author mrieser / senozon
	 */
	public void testGetLinkTravelTime_ignorePtVehiclesAtStop() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(new IdImpl(1), n1, n2);
		network.addLink(link1);

		Id agId1 = new IdImpl(1510);
		Id agId2 = new IdImpl("pt2011");
		Id vehId = new IdImpl(1980);

		ttc.handleEvent(new LinkEnterEventImpl(100, agId1, link1.getId()));
		ttc.handleEvent(new TransitDriverStartsEvent(140, agId2, vehId, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep1")));
		ttc.handleEvent(new LinkEnterEventImpl(150, agId2, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(200, agId1, link1.getId()));
		ttc.handleEvent(new VehicleArrivesAtFacilityEventImpl(240, vehId, new IdImpl("stop"), 0));
		ttc.handleEvent(new LinkLeaveEventImpl(350, agId2, link1.getId()));

		Assert.assertEquals("The time of transit vehicles at stop should not be counted", 100.0, ttc.getLinkTravelTime(link1, 200), 1e-8);
	}

	/**
	 * @author mrieser / senozon
	 */
	public void testGetLinkTravelTime_usePtVehiclesWithoutStop() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(new IdImpl(1), n1, n2);
		network.addLink(link1);

		Id agId1 = new IdImpl(1510);
		Id agId2 = new IdImpl("pt2011");
		Id vehId = new IdImpl(1980);

		ttc.handleEvent(new LinkEnterEventImpl(100, agId1, link1.getId()));
		ttc.handleEvent(new TransitDriverStartsEvent(140, agId2, vehId, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep1")));
		ttc.handleEvent(new LinkEnterEventImpl(150, agId2, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(200, agId1, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(300, agId2, link1.getId()));

		Assert.assertEquals("The time of transit vehicles at stop should not be counted", 125.0, ttc.getLinkTravelTime(link1, 200), 1e-8);

	}

	/**
	 * Enable filtering but set an empty string as modes to analyze.
	 * Expect that all link travel times are ignored.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_NoAnalyzedModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("");
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(new IdImpl(1), n1, n2);
		network.addLink(link1);

		Id agId1 = new IdImpl(1510);
		
		ttc.handleEvent(new AgentDepartureEventImpl(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEventImpl(100, agId1, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(200, agId1, link1.getId()));

		Assert.assertEquals("No transport mode has been registered to be analyzed, therefore no vehicle/agent should be counted", 1.0, ttc.getLinkTravelTime(link1, 200), 1e-8);
	}
	
	/**
	 * Enable filtering and analyze only car legs.
	 * Expect that walk legs are ignored.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_CarAnalyzedModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("car");
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(new IdImpl(1), n1, n2);
		network.addLink(link1);

		Id agId1 = new IdImpl(1510);
		Id agId2 = new IdImpl(1511);
		
		ttc.handleEvent(new AgentDepartureEventImpl(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEventImpl(100, agId1, link1.getId()));
		ttc.handleEvent(new AgentDepartureEventImpl(110, agId2, link1.getId(), TransportMode.walk));
		ttc.handleEvent(new LinkEnterEventImpl(110, agId2, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(200, agId1, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(410, agId2, link1.getId()));

		Assert.assertEquals("Only transport mode has been registered to be analyzed, therefore no walk agent should be counted", 100.0, ttc.getLinkTravelTime(link1, 200), 1e-8);
	}
	
	/**
	 * Disable filtering but also set analyzed modes to an empty string.
	 * Expect that still all modes are counted.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_NoFilterModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("");
		config.setFilterModes(false);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(new IdImpl(1), n1, n2);
		network.addLink(link1);

		Id agId1 = new IdImpl(1510);
		Id agId2 = new IdImpl(1511);
		
		ttc.handleEvent(new AgentDepartureEventImpl(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEventImpl(100, agId1, link1.getId()));
		ttc.handleEvent(new AgentDepartureEventImpl(110, agId2, link1.getId(), TransportMode.walk));
		ttc.handleEvent(new LinkEnterEventImpl(110, agId2, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(200, agId1, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(410, agId2, link1.getId()));

		Assert.assertEquals("Filtering analyzed transport modes is disabled, therefore count all modes", 200.0, ttc.getLinkTravelTime(link1, 200), 1e-8);
	}
	
	/**
	 * Enable filtering but do not set transport modes which should be counted.
	 * Expect that the default value (=car) will be used for the modes to be counted.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_FilterDefaultModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node n2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(new IdImpl(1), n1, n2);
		network.addLink(link1);

		Id agId1 = new IdImpl(1510);
		Id agId2 = new IdImpl(1511);
		
		ttc.handleEvent(new AgentDepartureEventImpl(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEventImpl(100, agId1, link1.getId()));
		ttc.handleEvent(new AgentDepartureEventImpl(110, agId2, link1.getId(), TransportMode.walk));
		ttc.handleEvent(new LinkEnterEventImpl(110, agId2, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(200, agId1, link1.getId()));
		ttc.handleEvent(new LinkLeaveEventImpl(410, agId2, link1.getId()));

		Assert.assertEquals("Filtering analyzed transport modes is enabled, but no modes set. Therefore, use default (=car)", 100.0, ttc.getLinkTravelTime(link1, 200), 1e-8);
	}
}
