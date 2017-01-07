package org.genericsystem.quiz.utils;

import org.genericsystem.reactor.Context;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.context.TagSwitcher;
import org.genericsystem.reactor.contextproperties.SelectionDefaults;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;

public class QuizTagSwitcher {

	public static class ACTIVE_QUIZ implements TagSwitcher {
		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			Property<Context> selectionProperty = ((SelectionDefaults) tag).getSelectionProperty(context);
			return Bindings.createBooleanBinding(() -> selectionProperty.getValue() != null, selectionProperty);
		}
	}

	public static class NO_ACTIVE_QUIZ implements TagSwitcher {
		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			Property<Context> selectionProperty = ((SelectionDefaults) tag).getSelectionProperty(context);
			return Bindings.createBooleanBinding(() -> selectionProperty.getValue() == null, selectionProperty);
		}
	}

	public static class QUIZ_END implements TagSwitcher {

		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			return tag.getProperty("QuizDone", context);
		}
	}

	public static class QUIZ_RUNNING implements TagSwitcher {

		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			Property<Boolean> selectionProperty = tag.getProperty("QuizDone", context);
			return Bindings.createBooleanBinding(() -> {
				System.out.println("Etat de Quiz Done : " + selectionProperty.getValue());
				return selectionProperty.getValue() == false;
			}, selectionProperty);
		}
	}

	// ************* NAVIGATION ENTRE LES PAGES **********************

	public static class HOME_PAGE implements TagSwitcher {

		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			return tag.getProperty("HomePage", context);
		}
	}

	public static class QUESTION_PAGE implements TagSwitcher {

		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			return tag.getProperty("QuestionPage", context);
		}
	}

	public static class RESULT_PAGE implements TagSwitcher {

		@Override
		public ObservableValue<Boolean> apply(Context context, Tag tag) {
			return tag.getProperty("ResultPage", context);
		}
	}

}