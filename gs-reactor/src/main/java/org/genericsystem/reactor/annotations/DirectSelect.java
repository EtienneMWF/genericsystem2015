package org.genericsystem.reactor.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiConsumer;

import org.genericsystem.reactor.Context;
import org.genericsystem.reactor.ExtendedAnnotationsManager.IGenericAnnotationProcessor;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.annotations.DirectSelect.DirectSelectGenericProcessor;
import org.genericsystem.reactor.annotations.DirectSelect.DirectSelects;
import org.genericsystem.reactor.annotations.DirectSelect.Processor;
import org.genericsystem.reactor.gscomponents.ExtendedRootTag.GTag;
import org.genericsystem.reactor.gscomponents.ExtendedRootTag.GTagAnnotation;
import org.genericsystem.reactor.gscomponents.ExtendedRootTag.GTagAnnotationContent;
import org.genericsystem.reactor.gscomponents.TagImpl;

/**
 * @author Nicolas Feybesse
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Repeatable(DirectSelects.class)
@Process(Processor.class)
@GenericProcess(DirectSelectGenericProcessor.class)
public @interface DirectSelect {
	Class<? extends TagImpl>[] path() default {};

	Class<?>[] value();

	int[] pos() default {};

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	public @interface DirectSelects {
		DirectSelect[] value();
	}

	public static class Processor implements BiConsumer<Annotation, Tag> {

		@Override
		public void accept(Annotation annotation, Tag tag) {
			tag.getRootTag().processDirectSelect(tag, ((DirectSelect) annotation).path(), ((DirectSelect) annotation).value());
		}
	}

	public static interface AnnotationProcessorListenerInDomNode extends IGenericAnnotationProcessor {

		@Override
		default void onRemove(Tag tag, Context context, GTagAnnotation gTagAnnotation, GTagAnnotationContent annotationContent) {
			// Nothing to do, listener defined in parent HtmlDomNode.
		}

		@Override
		default void onAdd(Tag tag, Context context, GTagAnnotation gTagAnnotation, GTagAnnotationContent annotationContent) {
			// Nothing to do, listener defined in parent HtmlDomNode.
		}
	}

	public static interface MetaBindingAnnotationProcessor extends AnnotationProcessorListenerInDomNode {

		@Override
		default void onRemove(Tag tag, GTagAnnotation gTagAnnotation, GTagAnnotationContent annotationContent) {
			((TagImpl) tag).setMetaBinding(null);
		}
	}

	public static class DirectSelectGenericProcessor implements MetaBindingAnnotationProcessor {

		@Override
		public void setAnnotation(GTag gTag, Annotation annotation) {
			gTag.setArrayValueAnnotation(DirectSelect.class, null, ((DirectSelect) annotation).value(), ((DirectSelect) annotation).path(), ((DirectSelect) annotation).pos());
		}

		@Override
		public void onAdd(Tag tag, GTagAnnotation gTagAnnotation, GTagAnnotationContent annotationContent) {
			tag.getRootTag().processDirectSelect(tag, gTagAnnotation.getValue().getPath(), annotationContent.getClassArrayContent());
		}
	}
}