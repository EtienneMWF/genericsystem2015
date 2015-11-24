package org.genericsystem.distributed.cacheonclient;

import java.util.concurrent.CompletableFuture;

import javafx.beans.value.ObservableValue;

import org.genericsystem.api.core.Snapshot;
import org.genericsystem.common.Generic;
import org.genericsystem.common.IDifferential;
import org.genericsystem.distributed.cacheonclient.observables.ObservableSnapshot;

/**
 * @author Nicolas Feybesse
 *
 */
public interface AsyncIDifferential extends IDifferential<Generic> {
	public ObservableValue<CompletableFuture<Snapshot<Generic>>> getDependenciesPromise(Generic generic);

	public ObservableSnapshot<Generic> getDependenciesObservableSnapshot(Generic generic);
}
