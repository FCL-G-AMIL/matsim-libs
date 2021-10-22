
/* *********************************************************************** *
 * project: org.matsim.*
 * Attributable.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.utils.objectattributes.attributable;

import org.matsim.api.core.v01.events.Event;

/**
 * Same as {@link Attributable} except that the method has a different name.  To avoid conflict with {@link Event#getAttributes()}.
 *
 * @author kainagel
 */
public interface ObjectAttributable{

	Attributes getObjectAttributes();
}