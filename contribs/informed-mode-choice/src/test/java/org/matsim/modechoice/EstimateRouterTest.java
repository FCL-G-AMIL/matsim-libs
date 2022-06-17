package org.matsim.modechoice;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class EstimateRouterTest {

	private EstimateRouter router;
	private InformedModeChoiceConfigGroup group;

	private Controler controler;

	@Before
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig();

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		controler = MATSimApplication.prepare(TestScenario.class, config);

		Injector injector = controler.getInjector();
		router = injector.getInstance(EstimateRouter.class);

	}

	@Test
	public void routing() {


		Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();

		PlanModel planModel = router.routeModes(persons.get(TestScenario.Agents.get(1)).getSelectedPlan(), group.getModes());

		assertThat(planModel)
				.isNotNull();


	}
}