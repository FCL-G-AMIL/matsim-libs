package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The MultinomialLogitSelector collects a set of candidates with given
 * utilities and then selects on according to the multinomial logit model.
 * All utilities are normalized beforehand, so that they are scale and location invariant.
 * For each candidate (i) a utility is calculated as:
 *
 * <code>P(i) = exp( Ui ) / Sum( U1 + U2 + ... + Un )</code>
 * <p>
 * With a scale parameter of zero this class is equivalent tobest choice.
 *
 * @author sebhoerl
 * @author rakow
 */
public class MultinomialLogitSelector implements Selector<PlanCandidate> {


	private final static Logger log = LogManager.getLogger(MultinomialLogitSelector.class);
	private final double scale;
	private final Random rnd;

	/**
	 *
	 * @param scale if scale is 0, always the best option will be picked.
	 */
	public MultinomialLogitSelector(double scale, Random rnd) {
		this.scale = scale;
		this.rnd = rnd;
	}

	@Nullable
	@Override
	public PlanCandidate select(Collection<PlanCandidate> candidates) {

		if (candidates.isEmpty())
			return null;

		if (scale <= 0) {
			return candidates.stream().sorted().findFirst().orElse(null);
		}

		// III) Create a probability distribution over candidates
		DoubleList density = new DoubleArrayList(candidates.size());
		List<PlanCandidate> pcs = new ArrayList<>(candidates);

		double min = candidates.stream().mapToDouble(PlanCandidate::getUtility).min().orElseThrow();
		double scale = candidates.stream().mapToDouble(PlanCandidate::getUtility).max().orElseThrow() - min;

		for (PlanCandidate candidate : pcs) {
			double utility = (candidate.getUtility() - min) / scale;
			density.add(Math.exp(utility / this.scale));
		}

		// IV) Build a cumulative density of the distribution
		DoubleList cumulativeDensity = new DoubleArrayList(density.size());
		double totalDensity = 0.0;

		for (int i = 0; i < density.size(); i++) {
			totalDensity += density.getDouble(i);
			cumulativeDensity.add(totalDensity);
		}

		// V) Perform a selection using the CDF
		double pointer = rnd.nextDouble() * totalDensity;

		int selection = (int) cumulativeDensity.doubleStream().filter(f -> f < pointer).count();
		return pcs.get(selection);
	}

}
