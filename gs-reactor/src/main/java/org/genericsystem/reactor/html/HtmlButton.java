package org.genericsystem.reactor.html;

import java.util.function.Consumer;

import org.genericsystem.reactor.HtmlElement;
import org.genericsystem.reactor.HtmlElement.ActionHtmlNode;
import org.genericsystem.reactor.Model;

/**
 * @author Nicolas Feybesse
 *
 */
public class HtmlButton<M extends Model> extends HtmlElement<M, ActionHtmlNode> {

	public HtmlButton(HtmlElement<?, ?> parent) {
		super(parent, "button", ActionHtmlNode.class);
	}

	@Override
	protected ActionHtmlNode createNode(Object parent) {
		return new ActionHtmlNode();
	}

	public HtmlButton<M> bindAction(Consumer<M> applyOnModel) {
		addActionBinding(ActionHtmlNode::getActionProperty, applyOnModel);
		return this;
	}
}
