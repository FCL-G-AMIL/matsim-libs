package org.matsim.contrib.drt.extension.shifts.run;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.zonal.DrtModeZonalSystemModule;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
import org.matsim.contrib.drt.extension.shifts.shift.ShiftsModule;
import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.DrtModeFeedforwardRebalanceModule;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.DrtModeMinCostFlowRebalancingModule;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.DrtModePlusOneRebalanceModule;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.drt.routing.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.URL;
import java.util.List;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftDrtModeModule extends AbstractDvrpModeModule {

    private final DrtConfigGroup drtCfg;
	private ShiftDrtConfigGroup shiftCfg;

	public ShiftDrtModeModule(DrtConfigGroup drtCfg, ShiftDrtConfigGroup shiftCfg) {
        super(drtCfg.getMode());
        this.drtCfg = drtCfg;
		this.shiftCfg = shiftCfg;
	}

    @Override
    public void install() {
        DvrpModes.registerDvrpMode(binder(), getMode());
        install(new DvrpModeRoutingNetworkModule(getMode(), drtCfg.isUseModeFilteredSubnetwork()));
		bindModal(TravelTime.class).to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
        bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		install(new ShiftsModule(getMode(), drtCfg, shiftCfg));

        install(new FleetModule(getMode(), drtCfg.getVehiclesFileUrl(getConfig().getContext()),
                drtCfg.isChangeStartLinkToLastLinkInSchedule()));

        if (drtCfg.getRebalancingParams().isPresent()) {
            RebalancingParams rebalancingParams = drtCfg.getRebalancingParams().get();
            install(new DrtModeZonalSystemModule(drtCfg));

            if (rebalancingParams.getRebalancingStrategyParams() instanceof MinCostFlowRebalancingStrategyParams) {
                install(new DrtModeMinCostFlowRebalancingModule(drtCfg));
            } else if (rebalancingParams.getRebalancingStrategyParams() instanceof PlusOneRebalancingStrategyParams) {
                install(new DrtModePlusOneRebalanceModule(drtCfg));
            } else if (rebalancingParams.getRebalancingStrategyParams() instanceof FeedforwardRebalancingStrategyParams) {
                install(new DrtModeFeedforwardRebalanceModule(drtCfg));
            } else {
                throw new RuntimeException(
                        "Unsupported rebalancingStrategyParams: " + rebalancingParams.getRebalancingStrategyParams());
            }
        } else {
            bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
        }

        //this is a customised version of DvrpModeRoutingModule.install()
        addRoutingModuleBinding(getMode()).toProvider(new DvrpRoutingModuleProvider(getMode()));// not singleton
        modalMapBinder(DvrpRoutingModuleProvider.Stage.class, RoutingModule.class).addBinding(
                DvrpRoutingModuleProvider.Stage.MAIN)
                .toProvider(new DvrpModeRoutingModule.DefaultMainLegRouterProvider(getMode()));// not singleton
        bindModal(DefaultMainLegRouter.RouteCreator.class).toProvider(
                new DrtRouteCreatorProvider(drtCfg));// not singleton

        bindModal(DrtStopNetwork.class).toProvider(new DrtStopNetworkProvider(getConfig(), drtCfg)).asEagerSingleton();

        if (drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.door2door) {
            bindModal(AccessEgressFacilityFinder.class).toProvider(
                    modalProvider(getter -> new DecideOnLinkAccessEgressFacilityFinder(getter.getModal(Network.class))))
                    .asEagerSingleton();
        } else {
            /*bindModal(DvrpRoutingModule.AccessEgressFacilityFinder.class).toProvider(modalProvider(
                    getter -> new ClosestAccessEgressFacilityFinder(drtCfg.getMaxWalkDistance(),
                            getter.get(Network.class),
                            QuadTrees.createQuadTree(getter.getModal(DrtStopNetwork.class).getDrtStops().values()))))
                    .asEagerSingleton();*/

			// MOIA DRT: Override standard version with attribute-based implementation that
			// falls back to using "all stops" for "all trips" if neither attributes are
			// defined for the stops nor for the persons
			bindModal(AccessEgressFacilityFinder.class).toProvider(modalProvider(getter -> {
				DrtStopNetwork stopNetwork = getter.getModal(DrtStopNetwork.class);
				Network network = getter.get(Network.class);

				return AttributeBasedStopFinder.create(drtCfg.getMaxWalkDistance(), network, stopNetwork.getDrtStops().values());
			})).asEagerSingleton();
        }

        bindModal(DrtRouteUpdater.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
            @Inject
            private Population population;

            @Inject
            private Config config;

            @Override
            public DefaultDrtRouteUpdater get() {
				var travelTime = getModalInstance(TravelTime.class);
                Network network = getModalInstance(Network.class);
                return new DefaultDrtRouteUpdater(drtCfg, network, travelTime,
                        getModalInstance(TravelDisutilityFactory.class), population, config);
            }
        }).asEagerSingleton();

        addControlerListenerBinding().to(modalKey(DrtRouteUpdater.class));

        drtCfg.getDrtFareParams()
                .ifPresent(params -> addEventHandlerBinding().toInstance(new DrtFareHandler(getMode(), params)));

        drtCfg.getDrtSpeedUpParams().ifPresent(drtSpeedUpParams -> {
            bindModal(DrtSpeedUp.class).toProvider(modalProvider(
                    getter -> new DrtSpeedUp(getMode(), drtSpeedUpParams, getConfig().controler(),
                            getter.get(Network.class), getter.getModal(FleetSpecification.class),
                            getter.getModal(DrtEventSequenceCollector.class)))).asEagerSingleton();
            addControlerListenerBinding().to(modalKey(DrtSpeedUp.class));
        });

    }

    private static class DrtRouteCreatorProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtRouteCreator> {
        private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

        private final DrtConfigGroup drtCfg;

        private DrtRouteCreatorProvider(DrtConfigGroup drtCfg) {
            super(drtCfg.getMode(), DvrpModes::mode);
            this.drtCfg = drtCfg;
            leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(drtCfg.getNumberOfThreads());
        }

        @Override
        public DrtRouteCreator get() {
			var travelTime = getModalInstance(TravelTime.class);
            return new DrtRouteCreator(drtCfg, getModalInstance(Network.class), leastCostPathCalculatorFactory,
                    travelTime, getModalInstance(TravelDisutilityFactory.class));
        }
    }

    private static class DrtStopNetworkProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtStopNetwork> {

        private final DrtConfigGroup drtCfg;
        private final Config config;

        private DrtStopNetworkProvider(Config config, DrtConfigGroup drtCfg) {
            super(drtCfg.getMode(), DvrpModes::mode);
            this.drtCfg = drtCfg;
            this.config = config;
        }

        @Override
        public DrtStopNetwork get() {
            switch (drtCfg.getOperationalScheme()) {
                case door2door:
                    return ImmutableMap::of;
                case stopbased:
                    return createDrtStopNetworkFromTransitSchedule(config, drtCfg);
                case serviceAreaBased:
                    return createDrtStopNetworkFromServiceArea(config, drtCfg, getModalInstance(Network.class));
                default:
                    throw new RuntimeException("Unsupported operational scheme: " + drtCfg.getOperationalScheme());
            }
        }
    }

    private static DrtStopNetwork createDrtStopNetworkFromServiceArea(Config config, DrtConfigGroup drtCfg,
                                                                      Network drtNetwork) {
        final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
                drtCfg.getDrtServiceAreaShapeFileURL(config.getContext()));
        ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = drtNetwork.getLinks()
                .values()
                .stream()
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(),
                        preparedGeometries))
                .map(DrtStopFacilityImpl::createFromLink)
                .collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
        return () -> drtStops;
    }

    private static DrtStopNetwork createDrtStopNetworkFromTransitSchedule(Config config, DrtConfigGroup drtCfg) {
        URL url = drtCfg.getTransitStopsFileUrl(config.getContext());
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readURL(url);
        ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = scenario.getTransitSchedule()
                .getFacilities()
                .values()
                .stream()
                .map(DrtStopFacilityImpl::createFromFacility)
                .collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
        return () -> drtStops;
    }
}