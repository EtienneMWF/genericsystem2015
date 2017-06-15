package org.genericsystem.cv.model;

import org.genericsystem.api.core.annotations.InstanceClass;
import org.genericsystem.api.core.annotations.SystemGeneric;
import org.genericsystem.common.Generic;
import org.genericsystem.cv.model.ImgFilter.ImgFilterInstance;

@SystemGeneric
@InstanceClass(ImgFilterInstance.class)
public class ImgFilter implements Generic {

	public static class ImgFilterInstance implements Generic {

	}

	public ImgFilterInstance addImgFilter(String name) {
		return (ImgFilterInstance) setInstance(name);
	}

	public ImgFilterInstance getImgFilter(String name) {
		return (ImgFilterInstance) getInstance(name);
	}

}
