package org.genericsystem.reactor.html;

import io.vertx.core.http.ServerWebSocket;

import org.genericsystem.reactor.Model;
import org.genericsystem.reactor.ViewContext.RootViewContext;
import org.genericsystem.reactor.appserver.PersistentApplication.App;

/**
 * @author Nicolas Feybesse
 *
 */
public abstract class HtmlApp<M extends Model> extends HtmlSection<M> implements App<M> {

	private final ServerWebSocket webSocket;
	private RootViewContext<M> rootViewContext;

	public HtmlApp(ServerWebSocket webSocket) {
		super(null);
		this.webSocket = webSocket;
	}

	@Override
	public HtmlApp<M> init(M rootModelContext, String rootId) {
		rootViewContext = new RootViewContext<M>(rootModelContext, this, rootId);
		return this;
	}

	@Override
	public ServerWebSocket getWebSocket() {
		return webSocket;
	}

	@Override
	public HtmlDomNode getNodeById(String id) {
		return rootViewContext.getNodeById(id);
	}

	@Override
	protected HtmlDomNode createNode(String parentId) {
		throw new UnsupportedOperationException();
	}
}
