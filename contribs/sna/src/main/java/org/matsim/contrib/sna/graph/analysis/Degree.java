/* *********************************************************************** *
 * project: org.matsim.*
 * Degree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.graph.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * A class that provides functionality to analyze degree related
 * graph-properties.
 * 
 * @author illenberger
 * 
 */
public class Degree implements VertexProperty {

	private static Degree instance;
	
	public static Degree getInstance() {
		if(instance == null)
			instance = new Degree();
		return instance;
	}
	
	/**
	 * Returns a descriptive statistics object containing the elements of <tt>vertices</tt>. The
	 * graph is treated as undirected.
	 * 
	 * @param vertices
	 *            a collection of vertices.
	 *            
	 * @return a descriptive statistics object.
	 */
	public DescriptiveStatistics distribution(Set<? extends Vertex> vertices) {
		DescriptiveStatistics distribution = new DescriptiveStatistics();
		for (Vertex v : vertices)
			distribution.addValue(v.getEdges().size());

		return distribution;
	}

	/**
	 * Returns a Vertex-double-map containing the degree of each vertex.
	 * 
	 * @param vertices
	 *            a set of vertices
	 * @return a Vertex-double-map containing the degree of each vertex.
	 */
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>();
		for (Vertex v : vertices)
			values.put(v, v.getEdges().size());

		return values;
	}

	/**
	 * Calculates the degree correlation of graph <tt>graph</tt>.<br>
	 * See: M. E. J. Newman. Assortative mixing in networks. Physical Review
	 * Letters, 89(20), 2002.
	 * 
	 * @param g
	 *            the graph the degree correlation is to be calculated.
	 * @return the degree correlation.
	 */
	public double assortativity(Graph graph) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;

		for (Edge e : graph.getEdges()) {
			Vertex v1 = e.getVertices().getFirst();
			Vertex v2 = e.getVertices().getSecond();
			int d_v1 = v1.getEdges().size();
			int d_v2 = v2.getEdges().size();

			sum += 0.5 * (d_v1 + d_v2);
			squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
			product += d_v1 * d_v2;
		}

		double norm = 1 / (double) graph.getEdges().size();
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
	}

}
