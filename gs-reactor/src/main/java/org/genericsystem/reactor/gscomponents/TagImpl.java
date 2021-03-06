package org.genericsystem.reactor.gscomponents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.genericsystem.reactor.Context;
import org.genericsystem.reactor.HtmlDomNode;
import org.genericsystem.reactor.MetaBinding;
import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.TagNode;
import org.genericsystem.reactor.context.TagSwitcher;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public abstract class TagImpl implements Tag {

	private final Property<String> tagName = new SimpleStringProperty();
	private final Property<Class<? extends HtmlDomNode>> domNodeClass = new SimpleObjectProperty<>();
	private Property<MetaBinding<?>> metaBinding = new SimpleObjectProperty<>();
	private final List<Consumer<Context>> preFixedBindings = new ArrayList<>();
	private final List<Consumer<Context>> postFixedBindings = new ArrayList<>();
	protected ObservableList<TagSwitcher> switchers = FXCollections.observableArrayList();
	protected TagNode tagNode;
	private Tag parent;

	@Override
	public TagNode getTagNode() {
		return tagNode;
	}

	@Override
	public void setTagNode(TagNode tagNode) {
		this.tagNode = tagNode;
	}

	@Override
	public String getTag() {
		return tagName.getValue();
	}

	@Override
	public void setTag(String tagName) {
		this.tagName.setValue(tagName);
	}

	@Override
	public Class<? extends HtmlDomNode> getDomNodeClass() {
		return domNodeClass.getValue() != null ? domNodeClass.getValue() : HtmlDomNode.class;
	}

	@Override
	public void setDomNodeClass(Class<? extends HtmlDomNode> domNodeClass) {
		this.domNodeClass.setValue(domNodeClass);
	}

	@Override
	public void setParent(Tag parent) {
		this.parent = parent;
	}

	@Override
	public ObservableList<Tag> getObservableChildren() {
		return getTagNode().getObservableChildren();
	}

	@Override
	public <COMPONENT extends Tag> COMPONENT getParent() {
		return (COMPONENT) parent;
	}

	@Override
	public String toString() {
		return getTag() + " " + getClass().getName();
	}

	@Override
	public List<Consumer<Context>> getPreFixedBindings() {
		return preFixedBindings;
	}

	@Override
	public List<Consumer<Context>> getPostFixedBindings() {
		return postFixedBindings;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <BETWEEN> MetaBinding<BETWEEN> getMetaBinding() {
		return (MetaBinding<BETWEEN>) metaBinding.getValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Property<MetaBinding<?>> getMetaBindingProperty() {
		return metaBinding;
	}

	@Override
	public <BETWEEN> void setMetaBinding(MetaBinding<BETWEEN> metaBinding) {
		this.metaBinding.setValue(metaBinding);
	}

	@Override
	public void addSwitcher(TagSwitcher switcher) {
		switchers.add(switcher);
	}

	@Override
	public ObservableList<TagSwitcher> getObservableSwitchers() {
		return switchers;
	}
}