package org.genericsystem.cache;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.genericsystem.kernel.Root;
import org.genericsystem.kernel.Statics;

public abstract class AbstractGSServer {

	private Map<String, Root> roots;

	public abstract JsonObject config();

	@SuppressWarnings("unchecked")
	public void start() {
		try {
			roots = new HashMap<>();
			if (config().getJsonArray("engines").isEmpty()) {
				Root root = new Root(Statics.ENGINE_VALUE, null, new Class<?>[] {});
				roots.put("/" + Statics.ENGINE_VALUE, root);
				System.out.println("Starts engine : " + "/" + Statics.ENGINE_VALUE);
			} else {
				List<JsonObject> list = config().getJsonArray("classes").getList();
				Class<?>[] classArray = new Class<?>[list.size()];
				for (int i = 0; i < list.size(); i++) {
					try {
						classArray[i] = Class.forName(list.get(i).getString("className"));
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				for (JsonObject engineJson : (List<JsonObject>) config().getJsonArray("engines").getList()) {

					String engineValue = engineJson.getString("engineValue");
					engineValue = engineValue == null ? Statics.ENGINE_VALUE : engineValue;
					Root root = new Root(engineValue, engineJson.getString("engineRepositoryPath"), classArray);
					roots.put("/" + engineValue, root);
					System.out.println("Starts engine : " + "/" + engineValue);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void stop() {
		System.out.println("Stopping engines...");
		if (config().getJsonArray("engines").isEmpty()) {
			Root root = roots.get("/" + Statics.ENGINE_VALUE);
			root.close();
			roots.remove("/" + Statics.ENGINE_VALUE);
			System.out.println("Stops engine : " + "/" + Statics.ENGINE_VALUE);
		} else
			for (JsonObject engineJson : (List<JsonObject>) config().getJsonArray("engines").getList()) {
				String engineValue = engineJson.getString("engineValue");
				engineValue = engineValue == null ? Statics.ENGINE_VALUE : engineValue;
				Root root = roots.get("/" + engineValue);
				root.close();
				roots.remove("/" + engineValue);
				System.out.println("Stops engine : " + "/" + engineValue);
			}
	}

	protected Map<String, Root> getRoots() {
		return roots;
	}

	protected Handler<Buffer> getHandler(Root root, Consumer<Buffer> sender, Consumer<Exception> exceptionSender) {
		return buffer -> {
			System.out.println(Thread.currentThread());
			GSBuffer gsBuffer = new GSBuffer(buffer);
			int id = gsBuffer.getInt();
			int methodId = gsBuffer.getInt();
			GSBuffer replyBuffer = new GSBuffer(Buffer.buffer());
			replyBuffer.appendInt(id).appendInt(methodId);
			switch (methodId) {
			case AbstractGSClient.PICK_NEW_TS: {
				replyBuffer.appendLong(root.pickNewTs());
				break;
			}
			case AbstractGSClient.GET_DEPENDENCIES: {
				replyBuffer.appendGSLongArray(root.getDependencies(gsBuffer.getLong(), gsBuffer.getLong()));
				break;
			}
			case AbstractGSClient.GET_VERTEX: {
				replyBuffer.appendGSVertex(root.getVertex(gsBuffer.getLong()));
				break;
			}
			case AbstractGSClient.APPLY: {
				try {
					root.apply(gsBuffer.getLong(), gsBuffer.getGSLongArray(), gsBuffer.getGSVertexArray());
					replyBuffer.appendLong(0);
				} catch (Exception e) {
					e.printStackTrace();
					exceptionSender.accept(e);
					return;
				}
				break;
			}
			default:
				throw new IllegalStateException("unable to find method:" + methodId + " " + "id :" + id);
			}
			sender.accept(replyBuffer);
		};
	}

}
