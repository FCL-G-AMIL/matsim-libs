package org.matsim.modechoice;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.estimators.DailyConstantFixedCosts;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.PtTripEstimator;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * A test scenario based on kelheim example.
 */
public class TestScenario extends MATSimApplication {

	/**
	 * Hand-picked agents that have trajectories fully within scenario area.
	 */
	public static final List<Id<Person>> Agents = List.of(
			Id.createPersonId("17187"),
			Id.createPersonId("10548"),
			Id.createPersonId("37842"),
			Id.createPersonId("40079"),
			Id.createPersonId("11074"),
			Id.createPersonId("13864")
	);

	public TestScenario(@Nullable Config config) {
		super(config);
	}

	public static Config loadConfig() {

		File f;
		try {
			f = new File(ExamplesUtils.getTestScenarioURL("kelheim/config.xml").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		Config config = ConfigUtils.loadConfig(f.toString());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		return config;
	}

	@Override
	protected Config prepareConfig(Config config) {

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		imc.setModes(Set.of("car", "ride", "bike", "walk", "pt"));

		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {

		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(DailyConstantFixedCosts.class, "car")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "ride", "bike", "walk")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
				.withTripEstimator(PtTripEstimator.class, ModeOptions.AlwaysAvailable.class, "pt");

		controler.addOverridingModule(builder.build());

	}
}
