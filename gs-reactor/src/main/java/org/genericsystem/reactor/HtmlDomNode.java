package org.genericsystem.reactor;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;

import org.genericsystem.defaults.tools.ObservableListWrapperExtended;
import org.genericsystem.defaults.tools.TransformationObservableList;
import org.genericsystem.reactor.Tag.RootTag;
import org.genericsystem.reactor.model.TagSelector;

public class HtmlDomNode {

	static int count = 0;
	protected static final String MSG_TYPE = "msgType";
	protected static final String ADD = "A";
	protected static final String UPDATE = "U";
	static final String REMOVE = "R";
	static final String UPDATE_TEXT = "UT";
	private static final String UPDATE_SELECTION = "US";
	static final String ADD_STYLECLASS = "AC";
	static final String REMOVE_STYLECLASS = "RC";
	static final String ADD_STYLE = "AS";
	static final String REMOVE_STYLE = "RS";
	static final String ADD_ATTRIBUTE = "AA";
	static final String REMOVE_ATTRIBUTE = "RA";

	static final String PARENT_ID = "parentId";
	public static final String ID = "nodeId";
	static final String NEXT_ID = "nextId";
	static final String STYLE_PROPERTY = "styleProperty";
	static final String STYLE_VALUE = "styleValue";
	static final String ATTRIBUTE_NAME = "attributeName";
	static final String ATTRIBUTE_VALUE = "attributeValue";
	static final String STYLECLASS = "styleClass";
	protected static final String TEXT_CONTENT = "textContent";
	static final String TAG_HTML = "tagHtml";
	protected static final String ELT_TYPE = "eltType";
	protected static final String SELECTED_INDEX = "selectedIndex";

	private final String id;
	private HtmlDomNode parent;
	private Tag tag;
	private Context context;

	public HtmlDomNode(HtmlDomNode parent, Context context, Tag tag) {
		this.id = String.format("%010d", Integer.parseInt(this.hashCode() + "")).substring(0, 10);
		this.parent = parent;
		this.tag = tag;
		this.context = context;
	}

	private ListChangeListener<Tag> listener;

	<BETWEEN> ListChangeListener<Tag> getListChangeListener() {
		return listener != null ? listener : (listener = change -> {
			System.out.println("Listener : " + change + " on tag " + tag + " on node : " + this);
			while (change.next()) {
				if (change.wasRemoved()) {
					for (Tag childTag : change.getRemoved()) {
						recursiveRemove(context, childTag);
						sizeBySubTag.remove(childTag);
					}
				}
				if (change.wasAdded()) {
					int index = change.getFrom();
					for (Tag childTag : change.getAddedSubList()) {
						MetaBinding<BETWEEN> metaBinding = childTag.<BETWEEN> getMetaBinding();
						if (metaBinding != null) {
							if (context.getSubContexts(childTag) == null)
								context.setSubContexts(childTag, new TransformationObservableList<BETWEEN, Context>(metaBinding.buildBetweenChildren(context), (i, between) -> {
									Context childModel = metaBinding.buildModel(context, between);
									childTag.createNode(this, childModel).init(computeIndex(i, childTag));
									return childModel;
								}, Context::destroy));
						} else if (context.getHtmlDomNode(childTag) == null)
							childTag.createNode(this, context).init(index++);
					}
				}
			}
		});
	}

	private void recursiveRemove(Context context, Tag tag) {
		if (tag.getMetaBinding() == null) {
			for (Tag childTag : context.getRootContext().getObservableChildren(tag))
				recursiveRemove(context, childTag);
			if (context.getHtmlDomNode(tag) != null)
				context.getHtmlDomNode(tag).sendRemove();
			context.removeProperties(tag);
			context.removeHtmlDomNode(tag);
		} else if (context.getSubContexts(tag) != null) {
			for (Context subContext : context.getSubContexts(tag)) {
				if (subContext.getHtmlDomNode(tag) != null)
					subContext.getHtmlDomNode(tag).sendRemove();
				subContext.removeProperties(tag);
			}
			context.getSubContexts(tag).removeAll();// destroy subcontexts
			context.removeSubContexts(tag);// remove tag ref
		}
	}

	protected <BETWEEN> void init(int index) {
		context.register(this);
		if (parent != null)
			insertChild(index);
		for (Consumer<Context> binding : tag.getPreFixedBindings())
			binding.accept(context);

		assert (!context.containsProperty(tag, "extractorsMap"));
		tag.createNewInitializedProperty("extractorsMap", context, c -> new HashMap<Tag, ObservableValue<Boolean>[]>());
		Map<Tag, ObservableValue<Boolean>[]> extractors = tag.<Map<Tag, ObservableValue<Boolean>[]>> getProperty("extractorsMap", context).getValue();
		ObservableList<Tag> extObs = new ObservableListWrapperExtended<Tag>(context.getRootContext().getObservableChildren(tag), child -> {
			ObservableValue<Boolean>[] result;
			TagSelector selector = child.getTagSelector();
			if (selector == null)
				result = new ObservableValue[] { new SimpleBooleanProperty(true) };
			else
				result = new ObservableValue[] { selector.apply(context, child) };
			Object prev = extractors.put(child, result);
			return result;
		});
		ObservableList<Tag> children = new FilteredList<Tag>(extObs, child -> Boolean.TRUE.equals(extractors.get(child)[0].getValue()));
		for (Tag childTag : children) {
			MetaBinding<BETWEEN> metaBinding = childTag.<BETWEEN> getMetaBinding();
			if (metaBinding != null) {
				context.setSubContexts(childTag, new TransformationObservableList<BETWEEN, Context>(metaBinding.buildBetweenChildren(context), (i, between) -> {
					Context childContext = metaBinding.buildModel(context, between);
					childTag.createNode(this, childContext).init(computeIndex(i, childTag));
					if (childContext.isOpaque())
						childTag.addStyleClass(childContext, "opaque");
					return childContext;
				}, Context::destroy));
			} else
				childTag.createNode(this, context).init(computeIndex(0, childTag));
		}
		children.addListener(getListChangeListener());
		for (Consumer<Context> binding : tag.getPostFixedBindings())
			binding.accept(context);
	}

	private int computeIndex(int indexInChildren, Tag childElement) {
		for (Tag child : context.getRootContext().getObservableChildren(tag)) {
			if (child == childElement)
				return indexInChildren;
			indexInChildren += sizeBySubTag.get(child);
		}
		return indexInChildren;
	}

	public Context getModelContext() {
		return context;
	}

	protected RootHtmlDomNode getRootHtmlDomNode() {
		return parent.getRootHtmlDomNode();
	}

	private Map<Tag, Integer> sizeBySubTag = new IdentityHashMap<Tag, Integer>() {
		private static final long serialVersionUID = 6725720602283055930L;

		@Override
		public Integer get(Object key) {
			Integer size = super.get(key);
			if (size == null)
				put((Tag) key, size = 0);
			return size;
		};
	};

	void insertChild(int index) {
		parent.incrementSize(tag);
		sendAdd(index);
		getRootHtmlDomNode().add(getId(), this);
	}

	private boolean destroyed = false;

	void destroy() {
		// System.out.println("Attempt to destroy : " + getNode().getId());
		assert !destroyed : "Node : " + getId();
		destroyed = true;
		getRootHtmlDomNode().remove(getId());
		parent.decrementSize(tag);
	}

	private void incrementSize(Tag child) {
		sizeBySubTag.put(child, sizeBySubTag.get(child) + 1);
	}

	private void decrementSize(Tag child) {
		int size = sizeBySubTag.get(child) - 1;
		assert size >= 0;
		if (size == 0)
			sizeBySubTag.remove(child);// remove map if empty
		else
			sizeBySubTag.put(child, size);
	}

	public ServerWebSocket getWebSocket() {
		return parent.getWebSocket();
	}

	private final MapChangeListener<String, String> stylesListener = change -> {
		if (!change.wasAdded() || change.getValueAdded() == null || change.getValueAdded().equals(""))
			sendMessage(new JsonObject().put(MSG_TYPE, REMOVE_STYLE).put(ID, getId()).put(STYLE_PROPERTY, change.getKey()));
		else if (change.wasAdded())
			sendMessage(new JsonObject().put(MSG_TYPE, ADD_STYLE).put(ID, getId()).put(STYLE_PROPERTY, change.getKey()).put(STYLE_VALUE, change.getValueAdded()));
	};

	private final MapChangeListener<String, String> attributesListener = change -> {
		if (!change.wasAdded() || change.getValueAdded() == null || change.getValueAdded().equals(""))
			sendMessage(new JsonObject().put(MSG_TYPE, REMOVE_ATTRIBUTE).put(ID, getId()).put(ATTRIBUTE_NAME, change.getKey()));
		else if (change.wasAdded())
			sendMessage(new JsonObject().put(MSG_TYPE, ADD_ATTRIBUTE).put(ID, getId()).put(ATTRIBUTE_NAME, change.getKey()).put(ATTRIBUTE_VALUE, change.getValueAdded()));
	};

	private final SetChangeListener<String> styleClassesListener = change -> {
		if (change.wasAdded())
			sendMessage(new JsonObject().put(MSG_TYPE, ADD_STYLECLASS).put(ID, getId()).put(STYLECLASS, change.getElementAdded()));
		else
			sendMessage(new JsonObject().put(MSG_TYPE, REMOVE_STYLECLASS).put(ID, getId()).put(STYLECLASS, change.getElementRemoved()));
	};

	private final ChangeListener<String> textListener = (o, old, newValue) -> sendMessage(new JsonObject().put(MSG_TYPE, UPDATE_TEXT).put(ID, getId()).put(TEXT_CONTENT, newValue != null ? newValue : ""));

	private final ChangeListener<Number> indexListener = (o, old, newValue) -> {
		// System.out.println(new JsonObject().put(MSG_TYPE,
		// UPDATE_SELECTION).put(ID, getId()).put(SELECTED_INDEX, newValue !=
		// null ? newValue : 0)
		// .encodePrettily());
		sendMessage(new JsonObject().put(MSG_TYPE, UPDATE_SELECTION).put(ID, getId()).put(SELECTED_INDEX, newValue != null ? newValue : 0));
	};

	public ChangeListener<Number> getIndexListener() {
		return indexListener;
	}

	public ChangeListener<String> getTextListener() {
		return textListener;
	}

	public MapChangeListener<String, String> getStylesListener() {
		return stylesListener;
	}

	public MapChangeListener<String, String> getAttributesListener() {
		return attributesListener;
	}

	public SetChangeListener<String> getStyleClassesListener() {
		return styleClassesListener;
	}

	public void sendAdd(int index) {
		JsonObject jsonObj = new JsonObject().put(MSG_TYPE, ADD);
		jsonObj.put(PARENT_ID, getParentId());
		jsonObj.put(ID, id);
		jsonObj.put(TAG_HTML, getTag().getTag());
		jsonObj.put(NEXT_ID, index);
		fillJson(jsonObj);
		// System.out.println(jsonObj.encodePrettily());
		sendMessage(jsonObj);
	}

	public JsonObject fillJson(JsonObject jsonObj) {
		return null;
	}

	public void sendRemove() {
		sendMessage(new JsonObject().put(MSG_TYPE, REMOVE).put(ID, id));
		// System.out.println(new JsonObject().put(MSG_TYPE, REMOVE).put(ID,
		// id).encodePrettily());
	}

	public void sendMessage(JsonObject jsonObj) {
		jsonObj.put("count", count++);
		// if (jsonObj.getString(MSG_TYPE).equals(ADD) ||
		// jsonObj.getString(MSG_TYPE).equals(REMOVE))
		// System.out.println(jsonObj.encodePrettily());
		getWebSocket().writeFinalTextFrame(jsonObj.encode());
	}

	public String getId() {
		return id;
	}

	public String getParentId() {
		return parent.getId();
	}

	public Tag getTag() {
		return tag;
	}

	public void handleMessage(JsonObject json) {

	}

	public static class RootHtmlDomNode extends HtmlDomNode {
		private final Map<String, HtmlDomNode> nodeById = new HashMap<>();
		private final ServerWebSocket webSocket;
		private final String rootId;

		public RootHtmlDomNode(Context rootModelContext, RootTag rootTag, String rootId, ServerWebSocket webSocket) {
			super(null, rootModelContext, rootTag);
			this.rootId = rootId;
			this.webSocket = webSocket;
			sendAdd(0);
			init(0);
		}

		@Override
		public ServerWebSocket getWebSocket() {
			return webSocket;
		}

		@Override
		protected RootHtmlDomNode getRootHtmlDomNode() {
			return this;
		}

		@Override
		public String getParentId() {
			return rootId;
		}

		private Map<String, HtmlDomNode> getMap() {
			return nodeById;
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
}