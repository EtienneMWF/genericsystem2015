package org.genericsystem.todoApp.binding;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.genericsystem.todoApp.ModelContext;

public abstract class Binding<B> {

	private Binder<B> binder;

	public Binding(Binder<B> binder) {
		this.binder = binder;
	}

	public void init(BindingContext context) {
		B initParam = buildInitParam(context);
		binder.init(initParam, context);
	}

	protected abstract B buildInitParam(BindingContext context);

	public static <U, V, T> FunctionBinding<U, V, ObservableList<T>> forEach(Function<U, ObservableList<T>> function) {
		return Binding.<U, V, ObservableList<T>> bind(function, Binder.foreachBinder());
	}

	public static <U, V> FunctionBinding<U, V, StringProperty> bindText(Function<U, StringProperty> function) {
		return Binding.<U, V, StringProperty> bind(function, Binder.textBinder());
	}

	public static <U, V> FunctionBinding<U, V, StringProperty> bindInputText(Function<U, StringProperty> function) {
		return Binding.<U, V, StringProperty> bind(function, Binder.inputTextBinder());
	}

	public static <U, V, T> ConsumerBinding<U, V, T> bindAction(Consumer<U> function) {
		return Binding.<U, V, T> bind(function, Binder.actionBinder());
	}

	public static <U, V, T> ConsumerBinding<U, V, T> bindAction(BiConsumer<U, V> function, Class<V> clazz) {
		return Binding.<U, V, T> bind(function, clazz, Binder.actionBinder());
	}

	private static <U, V, T> FunctionBinding<U, V, T> bind(Function<U, T> function, Binder<Function<V, T>> binder) {
		return new FunctionBinding<U, V, T>((u, v) -> function.apply(u), binder);
	}

	private static <U, V, T> FunctionBinding<U, V, T> bind(BiFunction<U, V, T> function, Binder<Function<V, T>> binder) {
		return new FunctionBinding<U, V, T>((u, v) -> function.apply(u, v), binder);
	}

	private static <U, V, T> ConsumerBinding<U, V, T> bind(Consumer<U> function, Binder<Consumer<V>> binder) {
		return new ConsumerBinding<U, V, T>((u, v) -> {
			function.accept(u);
			return null;
		}, binder);
	}

	private static <U, V, T> ConsumerBinding<U, V, T> bind(BiConsumer<U, V> function, Class<V> clazz2, Binder<Consumer<V>> binder) {
		return new ConsumerBinding<U, V, T>((u, v) -> {
			function.accept(u, v);
			return null;
		}, binder);
	}

	private static class FunctionBinding<U, V, T> extends Binding<Function<V, T>> {
		private final BiFunction<U, V, T> method;

		public FunctionBinding(BiFunction<U, V, T> method, Binder<Function<V, T>> binder) {
			super(binder);
			this.method = method;
		}

		@Override
		protected Function<V, T> buildInitParam(BindingContext context) {
			return (v) -> {
				ModelContext modelContext = context.getModelContext();
				while (modelContext != null) {
					try {
						return method.apply((U) modelContext.getModel(), v);
					} catch (ClassCastException ignore) {
					}
					modelContext = modelContext.getParent();
				}
				throw new IllegalStateException("Unable to resolve a method reference");
			};
		}

	}

	private static class ConsumerBinding<U, V, T> extends Binding<Consumer<V>> {
		private final BiFunction<U, V, T> method;

		public ConsumerBinding(BiFunction<U, V, T> method, Binder<Consumer<V>> binder) {
			super(binder);
			this.method = method;
		}

		@Override
		protected Consumer<V> buildInitParam(BindingContext context) {
			return (v) -> {
				ModelContext modelContext = context.getModelContext();
				while (modelContext != null) {
					try {
						method.apply((U) modelContext.getModel(), v);
						return;
					} catch (ClassCastException ignore) {
					}
					modelContext = modelContext.getParent();
				}
				throw new IllegalStateException("Unable to resolve a method reference");
			};
		}

	}

}
