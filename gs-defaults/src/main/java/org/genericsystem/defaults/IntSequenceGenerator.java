package org.genericsystem.defaults;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.genericsystem.api.core.annotations.constraints.InstanceValueGenerator.ValueGenerator;

/**
 * @author Nicolas Feybesse
 *
 * @param <T>
 */
public class IntSequenceGenerator<T extends DefaultGeneric<T>> implements ValueGenerator<T> {

	@Override
	public Serializable generateInstanceValue(T meta, List<T> supers, Serializable value, List<T> components) {
		return incrementedValue(meta);
	}

	protected int incrementedValue(T meta) {
		T sequence = meta.getRoot().getSequence();
		T sequenceHolder = meta.getHolders(sequence).first();
		int newValue = sequenceHolder != null ? (Integer) sequenceHolder.getValue() + 1 : 0;
		meta.setHolder(sequence, newValue);
		return newValue;
	}

	public static class StringSequenceGenerator<T extends DefaultGeneric<T>> extends IntSequenceGenerator<T> {
		@Override
		public Serializable generateInstanceValue(T meta, List<T> supers, Serializable value, List<T> components) {
			Serializable newValue = incrementedValue(meta);
			return meta.getValue() instanceof Class<?> ? ((Class<?>) meta.getValue()).getSimpleName() + "-" + newValue : Objects.toString(meta.getValue() + "-" + newValue);
		}
	}

}
