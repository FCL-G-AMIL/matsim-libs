/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.SAXException;

/**
 * @author sfuerbas
 * @author dgrether
 */
public class SfAirScheduleBuilder {

	private static final Logger log = Logger.getLogger(SfAirScheduleBuilder.class);

	public final static String[] EURO_COUNTRIES = { "AD", "AL", "AM", "AT", "AX", "AZ", "BA", "BE",
			"BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FO", "FR", "GB", "GI", "GE",
			"GG", "GR", "HR", "HU", "IE", "IM", "IS", "IT", "JE", "KZ", "LI", "LT", "LU", "LV", "MC",
			"MD", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ", "SK",
			"SM", "TR", "UA", "VA" };

	public final static String[] GERMAN_COUNTRIES = { "DE" };

	public static final String AIRPORTS_OUTPUT_FILE = "airports.txt";

	public static final String OAG_FLIGHTS_OUTPUT_FILENAME = "oag_flights.txt";

	public static final String CITY_PAIRS_OUTPUT_FILENAME = "city_pairs.txt";
	
	public static final String UTC_OFFSET_FILE = "utc_offsets.txt";

	protected Map<String, Coord> airports = new HashMap<String, Coord>();
	protected Map<String, Coord> airportsInOag = new HashMap<String, Coord>();
	protected Map<String, Double> routes = new HashMap<String, Double>();
	protected Map<String, Double> cityPairDistance = new HashMap<String, Double>();
	private Map<String, Double> utcOffset = new HashMap<String, Double>();

	
	public void filter(String inputOsmFilename, String inputOagFilename, String outputDirectory, String utcOffsetInputfile) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		this.filter(inputOsmFilename, inputOagFilename, outputDirectory, null, utcOffsetInputfile);
	}

	
	@SuppressWarnings("rawtypes")
	public void filter(String inputAirportListFile, String inputOagFile, String outputDirectory,
			String[] countries, String utcOffsetInputfile) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		String outputOagFile = outputDirectory + OAG_FLIGHTS_OUTPUT_FILENAME;

		BufferedReader brAirports = new BufferedReader(new FileReader(new File(inputAirportListFile)));
		while (brAirports.ready()) {
			String line = brAirports.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0];
			String xCoord = entries[1];
			String yCoord = entries[2];
			this.airports.put(airportCode, new CoordImpl(xCoord,yCoord));
		}
		
		brAirports.close();
		
		BufferedReader brUtc = new BufferedReader(new FileReader(new File(utcOffsetInputfile)));
		while (brUtc.ready()) {
			String line = brUtc.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0];
			double offset = Double.parseDouble(entries[1]);
			this.utcOffset.put(airportCode, offset);
		}
		
		brUtc.close();
		
		int counter = 0;
		
		int opscount = 0;
		
//		new UTC Offset functionality, work in progress!
		
//		Möglichkeit ohne UTC File abbauen
		
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagFile)));
		BufferedWriter bwOag = new BufferedWriter(new FileWriter(new File(outputOagFile)));

		Map<String, String> flights = new HashMap<String, String>();
		int lines = 0;

		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");

			if (lines > 0) {
				for (int jj = 0; jj < 81; jj++) {
					lineEntries[jj] = lineEntries[jj].replaceAll("\"", "");
				}

				String originCountry = lineEntries[6];
				String destinationCountry = lineEntries[9];
				String originAirport = lineEntries[4];
				
				if ((countries==null && this.utcOffset.containsKey(originAirport)) 
						|| (checkOriginCountry(originCountry, countries) && checkDestinationCountry(destinationCountry, countries) 
								&& this.utcOffset.containsKey(originAirport))) {
			
					//filter codeshare flights (see WW_DBF_With_Frequency.DOC from OAG input data)
					// either "operating marker" set, "shared airline designator" not set or "duplicate" not set
					if (lineEntries[47].contains("O") || lineEntries[43].equalsIgnoreCase("") || lineEntries[49].equalsIgnoreCase("")) 
					{	
						String carrier = lineEntries[0];
						String flightNumber = lineEntries[1].replaceAll(" ", "0");
						String flightDesignator = carrier + flightNumber;

						String destinationAirport = lineEntries[7];
						String route = originAirport + "_" + destinationAirport;
						double flightDistance = Integer.parseInt(lineEntries[42]) * 1.609344; // statute miles to kilometers
						
						String hours = lineEntries[13].substring(0, 3);
						String minutes = lineEntries[13].substring(3);
						double durationMinutes = Double.parseDouble(minutes) * 60; // convert flight dur minutes into seconds
						double durationHours = Double.parseDouble(hours) * 3600;
						double duration = durationHours + durationMinutes;
						double departureInSec = Double.parseDouble(lineEntries[10].substring(2)) * 60
								+ Double.parseDouble(lineEntries[10].substring(0, 2)) * 3600;
						
//						Getting UTC Offset from separate file which need to be created with SfUtcOffset
						double utcOffset = this.utcOffset.get(originAirport);
						departureInSec = departureInSec - utcOffset;
//						System.out.println("Airport: "+originAirport+" UTC offset was calculated as: "+utcOffset);

						if (departureInSec < 0)
							departureInSec += 86400.0; // shifting flights with departure on previous day in UTC time +24 hours
						double stops = Double.parseDouble(lineEntries[15]);
						String fullRouting = lineEntries[40];
						
						String aircraftType = lineEntries[21];
						int seatsAvail = Integer.parseInt(lineEntries[23]);

//						use this line to filter desired airports: currently all flights to/from FRA, MUC, DUS
//						for fixed city pairs use: originAirport.equalsIgnoreCase("FRA") && destinationAirport.equalsIgnoreCase("MUC") and vice versa
//						if ((originAirport.equalsIgnoreCase("FRA") 
//								|| destinationAirport.equalsIgnoreCase("MUC")
//								|| originAirport.equalsIgnoreCase("MUC")
//								&& destinationAirport.equalsIgnoreCase("MUC"))
//								|| (originAirport.equalsIgnoreCase("MUC")
//								&& destinationAirport.equalsIgnoreCase("FRA"))) {
						
						//some error correction code
						if ( 
//								lineEntries[14].contains("2") && //filter for Tuesday flights only
								!flights.containsKey(flightDesignator)
//								&& seatsAvail > 0 //filter for flights with 1 PAX or more only
//								&& !originAirport.equalsIgnoreCase(destinationAirport)
								&& this.airports.containsKey(originAirport)
								&& this.airports.containsKey(destinationAirport)
//								&& !aircraftType.equalsIgnoreCase("BUS") //filter busses
//								&& !aircraftType.equalsIgnoreCase("TRN") //filter trains
//								&& (stops < 1)
//								&& (fullRouting.length() <= 6)
								) {

							if (!this.routes.containsKey(route)) {
								this.routes.put(route, duration);
							}

							this.cityPairDistance.put(route, flightDistance);

							if ((flightDistance * 1000 / duration) <= 40.)
								log.debug("too low speed :" + flightDesignator);

							bwOag.write(route + "\t" + // TransitRoute
									route + "_" + carrier + "\t" + // TransitLine
									flightDesignator + "\t" + // vehicleId
									departureInSec + "\t" + // departure time in seconds
									duration + "\t" + // journey time in seconds
									aircraftType + "\t" + // aircraft type
									seatsAvail + "\t" + // seats avail
									flightDistance); // distance in km
							flights.put(flightDesignator, "");
							bwOag.newLine();
							counter++;
							this.airportsInOag.put(originAirport, this.airports.get(originAirport));
							this.airportsInOag
									.put(destinationAirport, this.airports.get(destinationAirport));
						}
					}
				}
//				}
			}
			lines++;
		}

		bwOag.flush();
		bwOag.close();

		// produce some more output

		String outputAirportFile = outputDirectory + AIRPORTS_OUTPUT_FILE;
//		String outputMissingAirportsFile = outputDirectory + missingAirportsOutputFilename;
		String cityPairsFile = outputDirectory + CITY_PAIRS_OUTPUT_FILENAME;

		BufferedWriter bwOsm = new BufferedWriter(new FileWriter(new File(outputAirportFile)));
//		BufferedWriter bwMissing = new BufferedWriter(new FileWriter(
//				new File(outputMissingAirportsFile)));
		BufferedWriter bwcityPairs = new BufferedWriter(new FileWriter(new File(cityPairsFile)));

		Iterator it = this.airportsInOag.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			bwOsm.write(pairs.getKey().toString() + "\t" + this.airportsInOag.get(pairs.getKey()).getX()
					+ "\t" + this.airportsInOag.get(pairs.getKey()).getY());
			bwOsm.newLine();
		}
//
//		Iterator it2 = this.missingAirports.entrySet().iterator();
//		while (it2.hasNext()) {
//			Map.Entry pairs = (Map.Entry) it2.next();
//			bwMissing.write(pairs.getKey().toString());
//			bwMissing.newLine();
//		}

		Iterator it3 = this.routes.entrySet().iterator();
		while (it3.hasNext()) {
			Map.Entry pairs = (Map.Entry) it3.next();
			bwcityPairs.write(pairs.getKey().toString() + "\t"
					+ this.cityPairDistance.get(pairs.getKey().toString()) + "\t"
					+ this.routes.get(pairs.getKey().toString()));
			bwcityPairs.newLine();
		}

		log.info("Anzahl der Airports: " + this.airportsInOag.size());
		log.info("Anzahl der City Pairs: " + this.routes.size());
		log.info("Anzahl der Flüge: " + counter);

		bwOsm.flush();
		bwOsm.close();
//		bwMissing.flush();
//		bwMissing.close();
		bwcityPairs.flush();
		bwcityPairs.close();
		br.close();

	}
	
	private static boolean checkOriginCountry(String originCountry, String[] countries) {
		boolean check=false;
		if (countries != null) {
			for (int ii = 0; ii < countries.length; ii++) {
				if (originCountry.equalsIgnoreCase(countries[ii]))
					check=true;
				else check=false;
				}
		}
			else check=true;
		return check;
	}
	
	private static boolean checkDestinationCountry(String destinationCountry, String[] countries) {
		boolean check=false;
		if (countries != null) {
			for (int ii = 0; ii < countries.length; ii++) {
				if (destinationCountry.equalsIgnoreCase(countries[ii]))
					check=true;
				else check=false;
				}
		}
			else check=true;
		return check;
	}
	
	
	private void getUtcOffsetMap(String inputfile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(inputfile)));
		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[2];
			lineEntries = oneLine.split("\t");
			this.utcOffset.put(lineEntries[0], Double.parseDouble(lineEntries[1]));
		}
		br.close();
	}

	private double getOffsetUTC(String originCountry) {

		if (originCountry.equalsIgnoreCase("AD"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AL"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AM"))
			return 5;
		else if (originCountry.equalsIgnoreCase("AT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AX"))
			return 3;
		else if (originCountry.equalsIgnoreCase("AZ"))
			return 5;
		else if (originCountry.equalsIgnoreCase("BA"))
			return 2;
		else if (originCountry.equalsIgnoreCase("BE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("BG"))
			return 3;
		else if (originCountry.equalsIgnoreCase("BY"))
			return 3;
		else if (originCountry.equalsIgnoreCase("CH"))
			return 2;
		else if (originCountry.equalsIgnoreCase("CY"))
			return 3;
		else if (originCountry.equalsIgnoreCase("CZ"))
			return 2;
		else if (originCountry.equalsIgnoreCase("DE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("DK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("EE"))
			return 3;
		else if (originCountry.equalsIgnoreCase("ES"))
			return 2;
		else if (originCountry.equalsIgnoreCase("FI"))
			return 3;
		else if (originCountry.equalsIgnoreCase("FO"))
			return 1;
		else if (originCountry.equalsIgnoreCase("FR"))
			return 2;
		else if (originCountry.equalsIgnoreCase("GB"))
			return 1;
		else if (originCountry.equalsIgnoreCase("GI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("GE"))
			return 4;
		else if (originCountry.equalsIgnoreCase("GG"))
			return 1;
		else if (originCountry.equalsIgnoreCase("GR"))
			return 3;
		else if (originCountry.equalsIgnoreCase("HR"))
			return 2;
		else if (originCountry.equalsIgnoreCase("HU"))
			return 2;
		else if (originCountry.equalsIgnoreCase("IE"))
			return 1;
		else if (originCountry.equalsIgnoreCase("IM"))
			return 1;
		else if (originCountry.equalsIgnoreCase("IS"))
			return 0;
		else if (originCountry.equalsIgnoreCase("IT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("JE"))
			return 1;
		else if (originCountry.equalsIgnoreCase("KZ"))
			return 6;
		else if (originCountry.equalsIgnoreCase("LI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("LT"))
			return 3;
		else if (originCountry.equalsIgnoreCase("LU"))
			return 2;
		else if (originCountry.equalsIgnoreCase("LV"))
			return 3;
		else if (originCountry.equalsIgnoreCase("MC"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MD"))
			return 3;
		else if (originCountry.equalsIgnoreCase("ME"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("NL"))
			return 2;
		else if (originCountry.equalsIgnoreCase("NO"))
			return 2;
		else if (originCountry.equalsIgnoreCase("PL"))
			return 2;
		// Azores are UTC, while mainland and Madeira are UTC+1
		else if (originCountry.equalsIgnoreCase("PT"))
			return 1;
		else if (originCountry.equalsIgnoreCase("RO"))
			return 3;
		else if (originCountry.equalsIgnoreCase("RS"))
			return 0;
		// Russia with Moscow time zone offset UTC+4
		else if (originCountry.equalsIgnoreCase("RU"))
			return 4;
		else if (originCountry.equalsIgnoreCase("SE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SJ"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SM"))
			return 2;
		else if (originCountry.equalsIgnoreCase("TR"))
			return 3;
		else if (originCountry.equalsIgnoreCase("UA"))
			return 3;
		else if (originCountry.equalsIgnoreCase("VA"))
			return 2;

		throw new RuntimeException("No UTC offset for country " + originCountry
				+ " found in lookup table. Please add offset first!");
	}

	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException, InterruptedException {

		SfAirScheduleBuilder builder = new SfAirScheduleBuilder();

		String osmFile = "/home/dgrether/shared-svn/projects/throughFlightData/osm_daten/2010-12-28_aeroway_nodes.osm";
		String oagFile = "/media/data/work/repos/"
				+ "shared-svn/projects/throughFlightData/oag_rohdaten/OAGSEP09.CSV";
		String outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";

		builder.filter(osmFile, oagFile, outputDirectory, EURO_COUNTRIES, UTC_OFFSET_FILE);

		// GERMAN AIR TRAFFIC ONLY BELOW

		outputDirectory = "/media/data/work/repos/"
				+ "shared-svn/studies/countries/de/flight/sf_oag_flight_model/";
		builder = new SfAirScheduleBuilder();
		builder.filter(osmFile, oagFile, outputDirectory, GERMAN_COUNTRIES, UTC_OFFSET_FILE);

	}


}
