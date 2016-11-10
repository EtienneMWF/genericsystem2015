package org.genericsystem.reactor.gscomponents;

import org.genericsystem.reactor.modelproperties.SelectionDefaults;
import org.genericsystem.reactor.modelproperties.UserRoleDefaults;

import java.lang.annotation.Annotation;

import org.genericsystem.reactor.AnnotationsManager;
import org.genericsystem.reactor.Context;
import org.genericsystem.reactor.HtmlDomNode.RootHtmlDomNode;
import org.genericsystem.reactor.RootTag;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.TagNode;
import org.genericsystem.reactor.annotations.CustomAnnotations;

import io.vertx.core.http.ServerWebSocket;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class RootTagImpl extends FlexDiv implements RootTag, SelectionDefaults, UserRoleDefaults {

	private AnnotationsManager annotationsManager;

	public RootTagImpl() {
		createSelectionProperty();
		createLoggedUserProperty();
		createAdminModeProperty();
		annotationsManager = new AnnotationsManager();
		Annotation annotations = getClass().getAnnotation(CustomAnnotations.class);
		if (annotations != null)
			for (Class<? extends Annotation> annotation : ((CustomAnnotations) annotations).value())
				annotationsManager.registerAnnotation(annotation);
		initRoot();
	}

	protected void initRoot() {
		tagNode = buildTagNode(this);
		annotationsManager.processAnnotations(this);
		init();
	}

	@Override
	public AnnotationsManager getAnnotationsManager() {
		return annotationsManager;
	}

	@Override
	public RootHtmlDomNode init(Context rootModelContext, String rootId, ServerWebSocket webSocket) {
		return new RootHtmlDomNode(rootModelContext, this, rootId, webSocket);
	}

	@Override
	public TagNode buildTagNode(Tag child) {
		return new TagNode() {

			private final ObservableList<Tag> children = FXCollections.observableArrayList();

			@Override
			public ObservableList<Tag> getObservableChildren() {
				return children;
			}
		};
	};

	@Override
	public final <COMPONENT extends Tag> COMPONENT getParent() {
		return null;
	}

	@Override
	public RootTag getRootTag() {
		return this;
	}
}
