package org.genericsystem.distributed.cacheonserver;

import org.genericsystem.api.core.exceptions.AliveConstraintViolationException;
import org.genericsystem.api.core.exceptions.ReferentialIntegrityConstraintViolationException;
import org.genericsystem.common.Generic;
import org.genericsystem.distributed.cacheonserver.LightClientCache;
import org.genericsystem.distributed.cacheonserver.LightClientEngine;
import org.testng.annotations.Test;

@Test
public class NotRemovableOneCacheTest extends AbstractTest {

	public void test001_aliveEx() {
		LightClientEngine engine = new LightClientEngine();
		LightClientCache cache = engine.getCurrentCache();
		Generic car = engine.addInstance("Car");
		Generic color = car.addAttribute("Color");
		Generic myBmw = car.addInstance("myBmw");
		Generic myBmwRed = myBmw.addHolder(color, "red");
		cache.clear();
		catchAndCheckCause(() -> myBmwRed.remove(), AliveConstraintViolationException.class);
	}

	public void test002_aliveEx() {
		LightClientEngine engine = new LightClientEngine();
		LightClientCache cache = engine.getCurrentCache();
		Generic car = engine.addInstance("Car");
		assert car.isAlive();
		Generic color = car.addAttribute("Color");
		Generic myBmw = car.addInstance("myBmw");
		Generic myBmwRed = myBmw.addHolder(color, "red");
		assert myBmwRed.isAlive();
		cache.flush();
		assert myBmwRed.isAlive();
		myBmwRed.remove();
		catchAndCheckCause(() -> myBmwRed.remove(), AliveConstraintViolationException.class);
	}

	public void test002_referenceEx() {
		LightClientEngine engine = new LightClientEngine();
		LightClientCache cache = engine.getCurrentCache();
		Generic car = engine.addInstance("Car");
		cache.flush();
		Generic color = car.addAttribute("Color");
		Generic myBmw = car.addInstance("myBmw");
		catchAndCheckCause(() -> car.remove(), ReferentialIntegrityConstraintViolationException.class);
	}

	public void test003_referenceEx() {
		LightClientEngine engine = new LightClientEngine();
		LightClientCache cache = engine.getCurrentCache();
		Generic car = engine.addInstance("Car");
		Generic color = car.addAttribute("Color");
		Generic myBmw = car.addInstance("myBmw");
		Generic myBmwRed = myBmw.addHolder(color, "red");
		cache.flush();
		catchAndCheckCause(() -> color.remove(), ReferentialIntegrityConstraintViolationException.class);
	}
}