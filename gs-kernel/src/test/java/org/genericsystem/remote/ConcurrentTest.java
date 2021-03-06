package org.genericsystem.remote;

import org.genericsystem.api.core.exceptions.ConcurrencyControlException;
import org.genericsystem.common.AbstractCache;
import org.genericsystem.common.Generic;
import org.genericsystem.remote.ClientEngine;
import org.testng.annotations.Test;

@Test
public class ConcurrentTest extends AbstractTest {

	public void test() {
		ClientEngine engine = new ClientEngine();
		AbstractCache cache = engine.getCurrentCache();
		AbstractCache cache2 = engine.newCache().start();
		Generic car = engine.addInstance("Car");

		assert cache2.isAlive(car);
		assert !cache.isAlive(car);

		cache2.flush();

		assert cache2.isAlive(car);
		cache.start();
		assert !cache.isAlive(car);
		cache.shiftTs();
		assert cache.isAlive(car);
	}

	public void testConcurrencyControlException() {
		ClientEngine engine = new ClientEngine();
		AbstractCache cache = engine.getCurrentCache().start();
		final Generic car = engine.addInstance("Car");
		cache.flush();
		ClientEngine engine2 = new ClientEngine();
		AbstractCache cache2 = engine2.newCache().start();
		Generic car2 = engine2.getInstance("Car");
		engine.getCurrentCache().start();
		car.remove();
		assert !engine.getCurrentCache().isAlive(car);
		assert !engine.getInstances().contains(car);
		engine2.getCurrentCache().start();
		assert engine2.getCurrentCache().isAlive(car2);
		assert engine2.getInstances().contains(car2);
		engine.getCurrentCache().start();
		try {
			engine.getCurrentCache().tryFlush();
			throw new IllegalStateException();
		} catch (ConcurrencyControlException ex) {
			// ignore
		}
	}

	public void testNonFlushedModificationsStillAliveInCache() {
		ClientEngine engine = new ClientEngine();
		Generic car = engine.addInstance("Car");
		AbstractCache cache = engine.getCurrentCache();

		assert cache.isAlive(car);
		assert engine.getInstances().contains(car);
	}

	public void testFlushedModificationsAvailableInNewCacheOk() {
		ClientEngine engine = new ClientEngine();
		AbstractCache cache = engine.getCurrentCache();
		Generic car = engine.addInstance("Car");
		cache.flush();

		assert cache.isAlive(car);
		assert engine.getInstances().contains(car);

		AbstractCache cache2 = engine.newCache().start();

		assert cache2.isAlive(car);
		assert engine.getInstances().contains(car);
	}

	public void testNonFlushedModificationsAreNotAvailableInNewCacheOk() {
		ClientEngine engine = new ClientEngine();
		AbstractCache cache = engine.getCurrentCache();
		Generic car = engine.addInstance("Car");

		assert cache.isAlive(car);
		assert engine.getInstances().contains(car);

		AbstractCache cache2 = engine.newCache().start();
		assert !cache2.isAlive(car);
		assert !engine.getInstances().contains(car);
	}
	//
	// // TODO: to CacheTest
	// public void testRemoveIntegrityConstraintViolation() {
	// Engine engine = GenericSystem.newInMemoryEngine();
	// final Cache cache1 = engine.newCache().start();
	// final Type car = cache1.addType("Car");
	// Generic bmw = car.addInstance("bmw");
	// cache1.flush();
	// assert car.getInstances().contains(bmw);
	//
	// new RollbackCatcher() {
	// @Override
	// public void intercept() {
	// car.remove();
	// }
	// }.assertIsCausedBy(ReferentialIntegrityConstraintViolationException.class);
	// }
	//
	// public void testConcurentRemoveKO() {
	// Engine engine = GenericSystem.newInMemoryEngine();
	// Cache cache = engine.newCache().start();
	// final Generic car = cache.addType("Car");
	// cache.flush();
	//
	// Cache cache2 = engine.newCache().start();
	// assert cache2.isAlive(car);
	// assert engine.getInheritings().contains(car);
	//
	// cache.start();
	// car.remove();
	// assert !cache.isAlive(car);
	// assert !engine.getInheritings().contains(car);
	//
	// cache2.start();
	// assert cache2.isAlive(car);
	// assert engine.getInheritings().contains(car);
	//
	// cache.start();
	// cache.flush();
	//
	// cache2.start();
	//
	// new RollbackCatcher() {
	//
	// @Override
	// public void intercept() {
	// car.remove();
	// }
	//
	// }.assertIsCausedBy(OptimisticLockConstraintViolationException.class);
	// }
	//
	// // TODO: move to CachTest
	// public void testRemoveFlushConcurrent() {
	// Engine engine = GenericSystem.newInMemoryEngine();
	// final CacheImpl cache1 = (CacheImpl) engine.newCache().start();
	// final Generic car = cache1.addType("Car");
	// cache1.flush();
	// // cache1.deactivate();
	//
	// Cache cache2 = engine.newCache().start();
	//
	// cache1.start();
	// car.remove();
	//
	// cache2.start();
	// cache2.flush();
	// // cache2.deactivate();
	//
	// // cache1.activate();
	//
	// cache1.start();
	// // cache1.pickNewTs();
	// new RollbackCatcher() {
	// @Override
	// public void intercept() {
	// car.remove();
	// }
	// }.assertIsCausedBy(AliveConstraintViolationException.class);
	// // cache1.deactivate();
	// }
	//
	// public void testConcurentRemoveKO2() {
	// Engine engine = GenericSystem.newInMemoryEngine();
	// Cache cache = engine.newCache().start();
	// final Generic car = cache.addType("Car");
	// cache.flush();
	//
	// assert cache.isAlive(car);
	// assert engine.getInheritings().contains(car);
	//
	// Cache cache2 = engine.newCache().start();
	// car.remove();
	// cache2.flush();
	//
	// assert !cache2.isAlive(car);
	// assert !engine.getInheritings().contains(car);
	//
	// cache.start();
	//
	// new RollbackCatcher() {
	//
	// @Override
	// public void intercept() {
	// car.remove();
	// }
	//
	// }.assertIsCausedBy(OptimisticLockConstraintViolationException.class);
	// }
	//
	// public void testConcurentRemoveKO3() {
	// Engine engine = GenericSystem.newInMemoryEngine();
	// final CacheImpl cache = (CacheImpl) engine.newCache().start();
	// final Generic car = cache.addType("Car");
	// cache.flush();
	//
	// assert cache.isAlive(car);
	// assert engine.getInheritings().contains(car);
	//
	// CacheImpl cache2 = (CacheImpl) engine.newCache().start();
	//
	// assert cache2.getTs() > cache.getTs();
	// assert cache2.isAlive(car);
	// assert engine.getInheritings().contains(car);
	//
	// car.remove();
	//
	// assert !cache2.isAlive(car);
	// assert !engine.getInheritings().contains(car);
	//
	// cache2.flush();
	//
	// assert cache2.getTs() > cache.getTs();
	// assert !cache2.isAlive(car);
	// assert !engine.getInheritings().contains(car);
	//
	// cache.start();
	//
	// new RollbackCatcher() {
	//
	// @Override
	// public void intercept() {
	// car.remove();
	// }
	//
	// }.assertIsCausedBy(OptimisticLockConstraintViolationException.class);
	// }
	//
	// public void testRemoveConcurrentMVCC() {
	// Engine engine = GenericSystem.newInMemoryEngine();
	// Cache cache1 = engine.newCache().start();
	// Generic car = cache1.addType("Car");
	// cache1.flush();
	// // cache1.deactivate();
	//
	// CacheImpl cache2 = (CacheImpl) engine.newCache();
	// assert cache2.isAlive(car);
	// // cache2.deactivate();
	//
	// assert ((CacheImpl) cache1).getTs() < cache2.getTs();
	// assert cache1.isAlive(car);
	//
	// // cache1.activate();
	// car.remove();
	// cache1.flush();
	// assert !cache1.isAlive(car);
	// assert ((CacheImpl) cache1).getTs() > cache2.getTs();
	// // cache1.deactivate();
	// }
}
