package org.gs.events.model;

import org.genericsystem.api.core.annotations.Components;
import org.genericsystem.api.core.annotations.Dependencies;
import org.genericsystem.api.core.annotations.Supers;
import org.genericsystem.api.core.annotations.SystemGeneric;
import org.genericsystem.api.core.annotations.constraints.InstanceValueClassConstraint;
import org.gs.events.model.Date.Day;
import org.gs.events.model.Date.Month;
import org.gs.events.model.Date.Year;

@SystemGeneric
@Dependencies({ Year.class, Month.class, Day.class })
public class Date {

	@SystemGeneric
	@InstanceValueClassConstraint(Integer.class)
	@Supers(Date.class)
	public class Year {
	}

	@SystemGeneric
	@InstanceValueClassConstraint(Integer.class)
	@Components(Year.class)
	@Supers(Date.class)
	public class Month {
	}

	@SystemGeneric
	@InstanceValueClassConstraint(Integer.class)
	@Components(Month.class)
	@Supers(Date.class)
	public class Day {
	}

}
