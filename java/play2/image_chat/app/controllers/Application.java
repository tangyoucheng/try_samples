package controllers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import play.*;
import play.mvc.*;
import play.libs.Akka;
import play.libs.F.*;

import views.html.*;

import akka.util.FiniteDuration;
import org.codehaus.jackson.JsonNode;

public class Application extends Controller {
	private static List<WebSocket.Out<String>> clientList = new CopyOnWriteArrayList<>();

	public static Result index() {
		return ok(index.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result send() {
		final JsonNode json = request().body().asJson();

		Akka.system().scheduler().scheduleOnce(
			new FiniteDuration(0, "seconds"),
			new Runnable() {
				public void run() {
					sendMessage(json.toString());
				}
			}
		);

		System.out.println(json);


		return ok();
	}

	public static WebSocket<String> connect() {
		return new WebSocket<String>() {
			public void onReady(WebSocket.In<String> wsin,
					final WebSocket.Out<String> wsout) {

				clientList.add(wsout);

				wsin.onClose(new Callback0() {
					public void invoke() throws Throwable {
						System.out.println("**** wsin close");
						clientList.remove(wsout);
						wsout.close();
					}
				});
			}
		};
	}

	private static void sendMessage(String msg) {
		for (WebSocket.Out<String> wsout: clientList) {
			wsout.write(msg);
		}
	}
}