/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

import static org.matsim.contrib.freight.events.CarrierEventAttributes.*;

/**
 *  An event, that informs that a Freight {@link CarrierShipment} pickup-activity has started.
 *
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierShipmentPickupStartEvent extends AbstractCarrierEvent {

	public static final String EVENT_TYPE = "Freight shipment pickup starts";

	private final Id<CarrierShipment> shipmentId;
	private final double pickupDuration;
	private final int capacityDemand;


	public CarrierShipmentPickupStartEvent(double time, Id<Carrier> carrierId, CarrierShipment shipment, Id<Vehicle> vehicleId) {
		super(time, carrierId, shipment.getFrom(), vehicleId);
		this.shipmentId = shipment.getId();
		this.pickupDuration = shipment.getPickupServiceTime();
		this.capacityDemand = shipment.getSize();
	}


	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<CarrierShipment> getShipmentId() {
		return shipmentId;
	}


	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SHIPMENT_ID, this.shipmentId.toString());
		attr.put(ATTRIBUTE_PICKUP_DURATION, String.valueOf(this.pickupDuration));
		attr.put(ATTRIBUTE_CAPACITYDEMAND, String.valueOf(capacityDemand));
		return attr;
	}

	public static CarrierShipmentPickupStartEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		Id<Carrier> carrierId = Id.create(attributes.get(ATTRIBUTE_CARRIER_ID), Carrier.class);
		Id<CarrierShipment> shipmentId = Id.create(attributes.get(ATTRIBUTE_SHIPMENT_ID), CarrierShipment.class);
		Id<Link> shipmentFrom = Id.createLinkId(attributes.get(ATTRIBUTE_LINK));
		int shipmentSize = Integer.parseInt(attributes.get(ATTRIBUTE_CAPACITYDEMAND));
		CarrierShipment shipment = CarrierShipment.Builder.newInstance(shipmentId, shipmentFrom, null, shipmentSize)
				.setPickupServiceTime(Double.parseDouble(attributes.get(ATTRIBUTE_PICKUP_DURATION)))
				.build();
		Id<Vehicle> vehicleId = Id.createVehicleId(attributes.get(ATTRIBUTE_VEHICLE));
		return new CarrierShipmentPickupStartEvent(time, carrierId, shipment, vehicleId);
	}
}
