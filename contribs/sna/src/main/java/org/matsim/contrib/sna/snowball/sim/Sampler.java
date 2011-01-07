/* *********************************************************************** *
 * project: org.matsim.*
 * Sampler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.snowball.sim;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.EdgeDecorator;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.GraphProjectionBuilder;
import org.matsim.contrib.sna.graph.GraphProjectionFactory;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.graph.analysis.VertexFilter;
import org.matsim.contrib.sna.snowball.SampledEdgeDecorator;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.core.utils.collections.Tuple;


/**
 * Representation of a snowball simulation.
 * 
 * @author illenberger
 * 
 */
public class Sampler<G extends Graph, V extends Vertex, E extends Edge> {

	private static final Logger logger = Logger.getLogger(Sampler.class);

	private TaggedGraph taggedGraph;

	private SampledGraphProjection<G, V, E> sampledGraph;

	private SampledGraphProjectionBuilder<G, V, E> builder;

	private VertexFilter<V> seedGenerator;

	private VertexFilter<V> responseGenerator;

	private SamplerListener listener;

	private int iteration;

	private int numSampledVertices;

	/**
	 * Sets the vertex filter to determine the seed vertices.
	 * 
	 * @param generator
	 *            A vertex filter.
	 */
	public void setSeedGenerator(VertexFilter<V> generator) {
		this.seedGenerator = generator;
	}

	/**
	 * Sets the vertex filter to determine responding vertices.
	 * 
	 * @param generator
	 *            A vertex filter.
	 */
	public void setResponseGenerator(VertexFilter<V> generator) {
		this.responseGenerator = generator;
	}

	/**
	 * Sets the sampler listener.
	 * 
	 * @param listener
	 *            A sampler listener.
	 */
	public void setListener(SamplerListener listener) {
		this.listener = listener;
	}

	public void setBuilder(SampledGraphProjectionBuilder<G, V, E> builder) {
		this.builder = builder;
	}
	
	/**
	 * Returns the current snowball iteration.
	 * 
	 * @return the current snowball iteration.
	 */
	public int getIteration() {
		return iteration;
	}

	/**
	 * Returns the number of sampler vertices.
	 * 
	 * @return the number of sampler vertices.
	 */
	public int getNumSampledVertices() {
		return numSampledVertices;
	}

	/**
	 * Returns the sampled graph.
	 * 
	 * @return the sampled graph.
	 */
	public SampledGraphProjection<G, V, E> getSampledGraph() {
		return sampledGraph;
	}

	/**
	 * Starts the snowball sampling. The sampler will draw the seed vertices
	 * from the seed generator and expand to all vertices indicated as
	 * responding by the response generator until all reachable vertices have
	 * been sampled or the sampler listener terminates the sampling process.
	 * 
	 * @param graph The graph to sample.
	 */
	public void run(G graph) {
		Set<TaggedVertex> recruits = init(graph);
		/*
		 * loop until no recruits are available
		 */
		while (!recruits.isEmpty()) {
			logger.info(String.format("Sampling iteration %1$s.", iteration));

			Set<TaggedVertex> newRecruits = new LinkedHashSet<TaggedVertex>();
			for (TaggedVertex vertex : recruits) {
				if (!listener.beforeSampling(this, vertex.getProjection()))
					return;
				/*
				 * check if the vertex is responsive
				 */
				if (vertex.isResponsive()) {
					/*
					 * expand and collect new recruits
					 */
					expand(vertex, newRecruits);
				}
				/*
				 * notify listener and terminate if applicable
				 */
				if (!listener.afterSampling(this, vertex.getProjection()))
					return;
			}
			recruits = newRecruits;
			iteration++;
		}

		listener.endSampling(this);

		logger.info("Done.");
	}

	@SuppressWarnings("unchecked")
	private Set<TaggedVertex> init(G graph) {
		iteration = 0;
		numSampledVertices = 0;
		/*
		 * 
		 */
		if (listener == null)
			listener = new DefaultListener();
		/*
		 * create an internally tagged graph for the sampler
		 */
		TaggedGraphBuilder tBuilder = new TaggedGraphBuilder();
		taggedGraph = tBuilder.decorateGraph(graph);
		/*
		 * create a sampled graph
		 */
		if(builder == null)
			builder = new SampledGraphProjectionBuilder<G, V, E>();
		sampledGraph = builder.createGraph(graph);
		/*
		 * tag the responsive vertices
		 */
		if (responseGenerator == null)
			responseGenerator = new AllResponding();

		Set<V> responsive = responseGenerator.apply((Set<V>) graph.getVertices());
		for (V vertex : responsive) {
			taggedGraph.getVertex(vertex).setResponsive(true);
		}
		/*
		 * draw the seed vertices
		 */
		Set<V> seeds = (Set<V>) seedGenerator.apply(responsive);
		Set<TaggedVertex> taggedSeeds = new LinkedHashSet<TaggedVertex>();
		for (V vertex : seeds) {
			TaggedVertex taggedVertex = taggedGraph.getVertex(vertex);
			SampledVertexDecorator<V> sampledVertex = builder.addVertex(sampledGraph, vertex);
			sampledVertex.detect(-1);
			sampledVertex.setSeed(sampledVertex);
			taggedVertex.setProjection(sampledVertex);
			taggedSeeds.add(taggedVertex);

		}

		return taggedSeeds;
	}

	private void expand(TaggedVertex vertex, Set<TaggedVertex> recruits) {
		/*
		 * sample vertex
		 */
		vertex.getProjection().sample(iteration);
		numSampledVertices++;
		/*
		 * expand to neighbors
		 */
		for (TaggedEdge edge : vertex.getEdges()) {
			if (edge.getProjection() == null) {
				/*
				 * edge has not been sampled
				 */
				TaggedVertex neighbour = edge.getOpposite(vertex);
				SampledVertexDecorator<V> sampledNeighbour;
				if (neighbour.getProjection() == null) {
					/*
					 * neighbor has not been sampled
					 */
					sampledNeighbour = builder.addVertex(sampledGraph, neighbour.getDelegate());
					sampledNeighbour.detect(iteration);
					sampledNeighbour.setSeed(vertex.getProjection().getSeed());
					neighbour.setProjection(sampledNeighbour);
					/*
					 * recruit
					 */
					recruits.add(neighbour);
				} else {
					sampledNeighbour = neighbour.getProjection();
				}
				/*
				 * sample edge
				 */
				SampledEdgeDecorator<E> sampledEdge = builder.addEdge(sampledGraph, vertex.getProjection(),
						sampledNeighbour, edge.getDelegate());
				edge.setProjection(sampledEdge);
			}
		}
	}

	private class TaggedGraph extends GraphProjection<G, V, E> {

		public TaggedGraph(G delegate) {
			super(delegate);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<? extends TaggedEdge> getEdges() {
			return (Set<? extends TaggedEdge>) super.getEdges();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<? extends TaggedVertex> getVertices() {
			return (Set<? extends TaggedVertex>) super.getVertices();
		}

		@Override
		public TaggedVertex getVertex(V v) {
			return (TaggedVertex) super.getVertex(v);
		}

	}

	private class TaggedVertex extends VertexDecorator<V> {

		private SampledVertexDecorator<V> projection;

		private boolean responsive;

		protected TaggedVertex(V delegate) {
			super(delegate);
		}

		public void setProjection(SampledVertexDecorator<V> projection) {
			this.projection = projection;
		}

		public SampledVertexDecorator<V> getProjection() {
			return projection;
		}

		public void setResponsive(boolean responive) {
			this.responsive = responive;
		}

		public boolean isResponsive() {
			return responsive;
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<? extends TaggedEdge> getEdges() {
			return (List<? extends TaggedEdge>) super.getEdges();
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<? extends TaggedVertex> getNeighbours() {
			return (List<? extends TaggedVertex>) super.getNeighbours();
		}

	}

	private class TaggedEdge extends EdgeDecorator<E> {

		private SampledEdgeDecorator<E> projection;

		protected TaggedEdge(E delegate) {
			super(delegate);
		}

		public void setProjection(SampledEdgeDecorator<E> projection) {
			this.projection = projection;
		}

		public SampledEdgeDecorator<E> getProjection() {
			return projection;
		}

		@Override
		public TaggedVertex getOpposite(Vertex v) {
			return (TaggedVertex) super.getOpposite(v);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Tuple<? extends TaggedVertex, ? extends TaggedVertex> getVertices() {
			return (Tuple<? extends TaggedVertex, ? extends TaggedVertex>) super.getVertices();
		}
	}

	private class TaggedGraphFactory implements GraphProjectionFactory<G, V, E, TaggedGraph, TaggedVertex, TaggedEdge> {

		@Override
		public TaggedEdge createEdge(E delegate) {
			return new TaggedEdge(delegate);
		}

		@Override
		public TaggedGraph createGraph(G delegate) {
			return new TaggedGraph(delegate);
		}

		@Override
		public TaggedVertex createVertex(V delegate) {
			return new TaggedVertex(delegate);
		}

	}

	private class TaggedGraphBuilder extends GraphProjectionBuilder<G, V, E, TaggedGraph, TaggedVertex, TaggedEdge> {

		public TaggedGraphBuilder() {
			super(new TaggedGraphFactory());
		}

	}

	private class AllResponding implements VertexFilter<V> {

		@Override
		public Set<V> apply(Set<V> vertices) {
			return vertices;
		}

	}

	private static class DefaultListener implements SamplerListener {

		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
		}

	}
}
