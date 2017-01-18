package org.genericsystem.quiz.utils;

import org.genericsystem.common.Generic;
import org.genericsystem.quiz.model.Quiz;
import org.genericsystem.reactor.Context;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.context.ContextAction;
import org.genericsystem.reactor.contextproperties.SelectionDefaults;
import org.genericsystem.security.model.User;

import javafx.beans.property.Property;

public class QuizContextAction {

	public final static String SELECTED_QUIZ = "selectedQuiz";
	public final static String SELECTED_USER = "selectedUser";

	public static class CLEAR_QUIZ implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<Generic> selectedQuiz = tag.getProperty(SELECTED_QUIZ, context);

			if (selectedQuiz == null) {
				tag.getRootTag().createNewProperty(SELECTED_QUIZ);
				return;
			}

			if (selectedQuiz.getValue() == null)
				return;

			selectedQuiz.setValue(null);
		}
	}

	public static class CLEAR_USER implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<Generic> selectedUser = tag.getProperty(SELECTED_USER, context);

			if (selectedUser == null) {
				tag.getRootTag().createNewProperty(SELECTED_USER);
				return;
			}

			if (selectedUser.getValue() == null)
				return;

			selectedUser.setValue(null);
		}
	}

	public static class CLEAR_QUIZCONTEXT_PROPERTIES implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			new CLEAR_USER().accept(context, tag);
			new CLEAR_QUIZ().accept(context, tag);
		}

	}

	public static class SELECT_QUIZ implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {

			Generic quiz = context.getGeneric();
			Property<Generic> selectedQuiz = tag.getProperty(SELECTED_QUIZ, context);

			// Compare le type du tag avec la classe "Quiz" du modèle
			if (!quiz.getRoot().find(Quiz.class).equals(quiz.getMeta()))
				return;

			if (selectedQuiz == null)
				tag.getRootTag().createNewInitializedProperty(SELECTED_QUIZ, context.getRootContext(), c -> quiz);
			else if (!quiz.equals(selectedQuiz.getValue()))
				selectedQuiz.setValue(quiz);
		}

	}

	public static class SAVE_QUIZ_RESULT implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {

			Generic quiz = ((SelectionDefaults) tag).getSelectionProperty(context).getValue().getGeneric();
			Generic sUser = context.find(User.class).getInstance("Anti-Seche");
			Generic loggedUser = tag.getLoggedUserProperty(context).getValue();

			ScoreUtils.setResult(context, quiz, sUser, loggedUser);
			ScoreUtils.getResult(context, quiz, loggedUser);
		}
	}

	// NAVIGATION ENTRE LES PAGES

	public static class CLEAR_PAGES implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			if (tag.getProperty(QuizTagSwitcher.PAGE, context) != null)
				tag.getProperty(QuizTagSwitcher.PAGE, context).setValue(null);
		}

	}

	// Possibilité de factoriser
	public static class CALL_HOME_PAGE implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<String> pageProperty = tag.getProperty(QuizTagSwitcher.PAGE, context);
			if (pageProperty == null)
				tag.createNewInitializedProperty(QuizTagSwitcher.PAGE, context, c -> QuizTagSwitcher.HOME_PAGE);
			else
				pageProperty.setValue(QuizTagSwitcher.HOME_PAGE);
		}
	}

	public static class CALL_RESULT_PAGE implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<String> pageProperty = tag.getProperty(QuizTagSwitcher.PAGE, context);
			if (pageProperty == null)
				tag.createNewInitializedProperty(QuizTagSwitcher.PAGE, context, c -> QuizTagSwitcher.RESULT_PAGE);
			else
				pageProperty.setValue(QuizTagSwitcher.RESULT_PAGE);
		}
	}

	public static class CALL_QUESTION_PAGE implements ContextAction {

		@Override
		public void accept(Context context, Tag tag) {
			Property<String> pageProperty = tag.getProperty(QuizTagSwitcher.PAGE, context);
			if (pageProperty == null)
				tag.createNewInitializedProperty(QuizTagSwitcher.PAGE, context, c -> QuizTagSwitcher.QUESTION_PAGE);
			else
				pageProperty.setValue(QuizTagSwitcher.QUESTION_PAGE);
		}

	}
}
