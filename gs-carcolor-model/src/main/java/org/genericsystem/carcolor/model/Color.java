package org.genericsystem.carcolor.model;

import org.genericsystem.api.core.annotations.Meta;
import org.genericsystem.api.core.annotations.SystemGeneric;
import org.genericsystem.api.core.annotations.value.StringValue;
import org.genericsystem.reactor.annotation.InstanceColorize;

@SystemGeneric
@InstanceColorize
public class Color {

	@SystemGeneric
	@Meta(Color.class)
	@StringValue("White")
	public static class White {
	}

	@SystemGeneric
	@Meta(Color.class)
	@StringValue("Red")
	public static class Red {
	}

	@SystemGeneric
	@Meta(Color.class)
	@StringValue("Blue")
	public static class Blue {
	}

	@SystemGeneric
	@Meta(Color.class)
	@StringValue("Yellow")
	public static class Yellow {
	}
}
