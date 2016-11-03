package org.genericsystem.reactor.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;

import org.genericsystem.reactor.AnnotationsManager;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.annotations.SetText.SetTextProcessor;
import org.genericsystem.reactor.annotations.SetText.SetTexts;
import org.genericsystem.reactor.gscomponents.TagImpl;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Repeatable(SetTexts.class)
@Process(SetTextProcessor.class)
public @interface SetText {
	Class<? extends TagImpl>[] path() default {};

	String[] value();

	int[] pos() default {};

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	public @interface SetTexts {
		SetText[] value();
	}

	public static class SetTextProcessor implements BiConsumer<Annotation, Tag> {

		@Override
		public void accept(Annotation annotation, Tag tag) {
			try {
				Class<?>[] path = (Class<?>[]) annotation.annotationType().getDeclaredMethod("path").invoke(annotation);
				String[] texts = ((SetText) annotation).value();
				if (texts.length == 1)
					tag.setText(texts[0]);
				else
					tag.setText(texts[AnnotationsManager.position(tag, path[path.length - 1])]);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
