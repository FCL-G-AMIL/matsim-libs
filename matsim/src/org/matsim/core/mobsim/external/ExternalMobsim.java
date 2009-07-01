/* *********************************************************************** *
 * project: org.matsim.*
 * ExternalMobsim.java
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

package org.matsim.core.mobsim.external;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.PopulationWriterHandler;
import org.matsim.core.utils.misc.ExeRunner;

public class ExternalMobsim {

	private static final String CONFIG_MODULE = "simulation";

	protected Events events;
	protected Population population;

	protected String plansFileName = null;
	protected String eventsFileName = null;
	protected String configFileName = null;

	protected String executable = null;

	private static final Logger log = Logger.getLogger(ExternalMobsim.class);

	public ExternalMobsim(final Population population, final Events events) {
		this.population = population;
		this.events = events;
		init();
	}

	protected void init() {
		this.plansFileName = "ext_plans.xml";
		this.eventsFileName = "ext_events.txt";
		this.configFileName = "ext_config.xml";

		this.executable = Gbl.getConfig().getParam(CONFIG_MODULE, "externalExe");
	}

	public void run() {
		String iterationPlansFile = Controler.getIterationFilename(this.plansFileName);
		String iterationEventsFile = Controler.getIterationFilename(this.eventsFileName);
		String iterationConfigFile = Controler.getIterationFilename(this.configFileName);

		try {
			writePlans(iterationPlansFile);
			writeConfig(iterationPlansFile, iterationEventsFile, iterationConfigFile);
			runExe(iterationConfigFile);
			readEvents(iterationEventsFile);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	protected void writeConfig(final String iterationPlansFile, final String iterationEventsFile, final String iterationConfigFile) throws FileNotFoundException, IOException {
		log.info("writing config for external mobsim");
		Config simConfig = Gbl.getConfig();
		Config extConfig = new Config();
		// network
		Module module = extConfig.createModule("network");
		module.addParam("inputNetworkFile", simConfig.network().getInputFile());
		module.addParam("localInputDTD", "dtd/matsim_v1.dtd");
		// plans
		module = extConfig.createModule("plans");
		module.addParam("inputPlansFile", iterationPlansFile);
		module.addParam("inputVersion", "matsimXMLv4");
		// events
		module = extConfig.createModule("events");
		module.addParam("outputFile", iterationEventsFile);
		module.addParam("outputFormat", "matsimTXTv1");
		// deqsim
		module = extConfig.createModule(CONFIG_MODULE);
		module.addParam("startTime", simConfig.getParam(CONFIG_MODULE, "startTime"));
		module.addParam("endTime", simConfig.getParam(CONFIG_MODULE, "endTime"));

		PrintWriter writer = null;
		File configfile = new File(iterationConfigFile);
		writer = new PrintWriter(configfile);
		ConfigWriter configwriter = new ConfigWriter(extConfig, writer);
		configwriter.write();
		writer.close();
	}

	protected void writePlans(final String iterationPlansFile) throws FileNotFoundException, IOException {
		log.info("writing plans for external mobsim");
		String version = "v4";
		PopulationImpl pop = new PopulationImpl();
		pop.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(pop, iterationPlansFile, version);
		PopulationWriterHandler handler = plansWriter.getHandler();
		plansWriter.writeStartPlans();
		BufferedWriter writer = plansWriter.getWriter();
		for (PersonImpl person : this.population.getPersons().values()) {
			PlanImpl plan = person.getSelectedPlan();
			if (plan != null) {
				/* we have to re-implement a custom writer here, because we only want to
				 * write a single plan (the selected one) and not all plans of the person.
				 */
				handler.startPerson(person, writer);
				handler.startPlan(plan, writer);

				// act/leg
				for (int jj = 0; jj < plan.getPlanElements().size(); jj++) {
					if (jj % 2 == 0) {
						ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(jj);
						handler.startAct(act, writer);
						handler.endAct(writer);
					} else {
						LegImpl leg = (LegImpl)plan.getPlanElements().get(jj);
						handler.startLeg(leg, writer);
						// route
						if (leg.getRoute() != null) {
							NetworkRoute r = (NetworkRoute) leg.getRoute();
							handler.startRoute(r, writer);
							handler.endRoute(writer);
						}
						handler.endLeg(writer);
					}
				}
				handler.endPlan(writer);
				handler.endPerson(writer);
				handler.writeSeparator(writer);
				writer.flush();
			}
		}
		handler.endPlans(writer);
		writer.flush();
		writer.close();
	}

	@SuppressWarnings("unused") /* do now show warnings that this method does not throw any exceptions,
	as classes inheriting from this class may throw exceptions in their implementation of this method. */
	protected void runExe(final String iterationConfigFile) throws FileNotFoundException, IOException {
		String cmd = this.executable + " " + iterationConfigFile;
		log.info("running command: " + cmd);
		Gbl.printMemoryUsage();
		String logfileName = Controler.getIterationFilename("mobsim.log");
		int timeout = Gbl.getConfig().simulation().getExternalTimeOut();
		int exitcode = ExeRunner.run(cmd, logfileName, timeout);
		if (exitcode != 0) {
			Gbl.errorMsg("There was a problem running the external mobsim. exit code: " + exitcode);
		}
	}

	@SuppressWarnings("unused") /* do now show warnings that this method does not throw any exceptions,
	as classes inheriting from this class may throw exceptions in their implementation of this method. */
	protected void readEvents(final String iterationEventsFile) throws FileNotFoundException, IOException {
		log.info("reading events from external mobsim");
		new MatsimEventsReader(this.events).readFile(iterationEventsFile);
	}

}
