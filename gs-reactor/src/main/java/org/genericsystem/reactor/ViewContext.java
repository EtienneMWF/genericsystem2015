package org.genericsystem.reactor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.genericsystem.reactor.Tag.HtmlDomNode;

/**
 * @author Nicolas Feybesse
 *
 * @param <N>
 */
public class ViewContext<M extends Model> {

	private ViewContext<?> parent;
	private Tag<M> element;
	private HtmlDomNode node;
	private Model modelContext;

	private ViewContext(int indexInChildren, Model modelContext, Tag<M> element, String rootId) {
		init(null, modelContext, element, element.new HtmlDomNode(rootId));
		node.sendAdd(0);
		init(indexInChildren);
	}

	private ViewContext(int indexInChildren, ViewContext<?> parent, Model modelContext, Tag<M> element) {
		init(parent, modelContext, element, element.createNode(parent.getNode().getId()));
		init(indexInChildren);
	}

	private void init(ViewContext<?> parent, Model modelContext, Tag<M> element, HtmlDomNode node) {
		this.parent = parent;
		this.element = element;
		assert node != null;
		this.node = node;
		this.modelContext = modelContext;
	}

	private void init(int indexInChildren) {
		modelContext.register(this);
		if (parent != null)
			insertChild(indexInChildren);
		for (BiConsumer<Model, Tag<M>.HtmlDomNode> binding : element.getPreFixedBindings())
			binding.accept(modelContext, getNode());
		for (Tag<?> childElement : element.getChildren())
			if (childElement.getMetaBinding() != null)
				childElement.getMetaBinding().accept(childElement, this);
			else
				createViewContextChild(null, modelContext, childElement);
		for (BiConsumer<Model, Tag<M>.HtmlDomNode> binding : element.getPostFixedBindings())
			binding.accept(modelContext, getNode());
	}

	@SuppressWarnings("unchecked")
	public <MODEL extends Model> MODEL getModelContext() {
		return (MODEL) modelContext;
	}

	public ViewContext<?> createViewContextChild(Integer index, Model childModelContext, Tag<?> element) {
		int indexInChildren = computeIndex(index, element);
		return new ViewContext<>(indexInChildren, this, childModelContext, element);
	}

	protected RootViewContext<?> getRootViewContext() {
		return parent.getRootViewContext();
	}

	@SuppressWarnings({ "unchecked" })
	public <NODE extends HtmlDomNode> NODE getNode() {
		return (NODE) node;
	}

	private Map<Tag<?>, Integer> sizeBySubElement = new IdentityHashMap<Tag<?>, Integer>() {
		private static final long serialVersionUID = 6725720602283055930L;

		@Override
		public Integer get(Object key) {
			Integer size = super.get(key);
			if (size == null)
				put((Tag<?>) key, size = 0);
			return size;
		};
	};

	void insertChild(int index) {
		parent.incrementSize(element);
		node.sendAdd(index);
		getRootViewContext().add(node.getId(), node);
	}

	private boolean destroyed = false;

	void destroy() {
		// System.out.println("Attempt to destroy : " + getNode().getId());
		assert !destroyed : "Node : " + getNode().getId();
		destroyed = true;
		getRootViewContext().remove(node.getId());
		parent.decrementSize(element);
	}

	private void incrementSize(Tag<?> child) {
		sizeBySubElement.put(child, sizeBySubElement.get(child) + 1);
	}

	private void decrementSize(Tag<?> child) {
		int size = sizeBySubElement.get(child) - 1;
		assert size >= 0;
		if (size == 0)
			sizeBySubElement.remove(child);// remove map if empty
		else
			sizeBySubElement.put(child, size);
	}

	private int computeIndex(Integer nullable, Tag<?> childElement) {
		int indexInChildren = nullable == null ? sizeBySubElement.get(childElement) : nullable;
		for (Tag<?> child : element.getChildren()) {
			if (child == childElement)
				return indexInChildren;
			indexInChildren += sizeBySubElement.get(child);
		}
		return indexInChildren;
	}

	public static class RootViewContext<M extends Model> extends ViewContext<M> {
		private Map<String, HtmlDomNode> nodeById;

		public RootViewContext(M rootModelContext, Tag<M> template, String rootId) {
			super(0, rootModelContext, template, rootId);
		}

		@Override
		protected RootViewContext<?> getRootViewContext() {
			return this;
		}

		private Map<String, HtmlDomNode> getMap() {
			return nodeById != null ? nodeById : (nodeById = new HashMap<>());
		}

		public HtmlDomNode getNodeById(String id) {
			return getMap().get(id);
		}

		public void add(String id, HtmlDomNode domNode) {
			getMap().put(id, domNode);
		}

		public void remove(String id) {
			getMap().remove(id);
		}
	}

	public Tag<M> getTag() {
		return element;
	}
}
