package org.genericsystem.cv.retriever;

import org.genericsystem.reinforcer.tools.GSRect;

public interface OverlapConstraint {

	default void createNode(GSRect rect, Field parent) {
		if (checkOverlapConstraint(rect))
			createNodeImpl(rect, parent);
	}

	default void updateNode(GSRect rect, Field field, int width, int height) {
		if (checkOverlapConstraint(rect, field))
			updateNodeImpl(rect, field, width, height);
	}

	void removeNode(Field field);

	void createNodeImpl(GSRect rect, Field parent);

	void updateNodeImpl(GSRect rect, Field field, int width, int height);

	boolean checkOverlapConstraint(GSRect rect);

	boolean checkOverlapConstraint(GSRect rect, Field target);
}