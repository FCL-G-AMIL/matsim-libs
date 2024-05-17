package org.matsim.contrib.dvrp.passenger;

import static org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule.PassengerEngineType.DEFAULT;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

import java.util.Optional;

/**
 * dvrp needs {@link PassengerHandler} bound to something.  It is first bound to a {@link PassengerEngine} so it can also do stuff on its own.  Then,
 * {@link PassengerEngine} is bound to one of the existing implementations.
 */
public class PassengerEngineQSimModule extends AbstractDvrpModeQSimModule {
	public enum PassengerEngineType {
		DEFAULT, WITH_PREBOOKING, TELEPORTING_SPEED_UP, TELEPORTING_ESTIMATION
	}

	private final PassengerEngineType type;

	public PassengerEngineQSimModule(String mode) {
		this(mode, DEFAULT);
	}

	public PassengerEngineQSimModule(String mode, PassengerEngineType type) {
		super(mode);
		this.type = type;
	}

	@Override
	protected void configureQSim() {
		bindModal(PassengerHandler.class).to(modalKey(PassengerEngine.class));
		bindModal(PassengerGroupIdentifier.class).toInstance(agent -> Optional.empty());

		// (PassengerEngine is a more powerful interface.)

		addMobsimScopeEventHandlerBinding().to(modalKey(PassengerEngine.class));

		switch( type ){
			case DEFAULT -> addModalComponent( PassengerEngine.class, DefaultPassengerEngine.createProvider( getMode() ) );
			case WITH_PREBOOKING -> addModalComponent( PassengerEngine.class, PassengerEngineWithPrebooking.createProvider( getMode() ) );
			case TELEPORTING_SPEED_UP -> addModalComponent( PassengerEngine.class, TeleportingPassengerEngine.createProvider( getMode() ) );
			case TELEPORTING_ESTIMATION -> addModalComponent( PassengerEngine.class, TeleportingEstimationPassengerEngine.createProvider( getMode() ));
		}
	}
}
