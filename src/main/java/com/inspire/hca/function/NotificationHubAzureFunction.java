package com.inspire.hca.function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.windowsazure.messaging.Notification;
import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.NotificationHubsException;

public class NotificationHubAzureFunction {

	@FunctionName("HCANotificationQueue")
	public void functionHandler(
			@QueueTrigger(name = "req", queueName = "hcanotificationqueue", connection = "conn") String queueItem,
			final ExecutionContext executionContext) throws NotificationHubsException {

		executionContext.getLogger().info("Queue trigger input: " + queueItem);

		NotificationHub hub = new NotificationHub(System.getenv("NOTIFICATION_HUB_CONNECTION_STRING"),
				System.getenv("NOTIFICATION_HUB_PATH"));

		JSONObject queueItemJson = new JSONObject(queueItem);
		String notification = queueItemJson.get("notification").toString();
		JsonObject notificationJson = new JsonParser().parse(notification).getAsJsonObject();

		String gcmMessage = "{\"data\":{\"message\":" + notification + "}}";
		String appleMessage = "{\"aps\":{\"alert\":{\"body\":" + notificationJson.get("text").toString() + "}, \"sound\":\"default\"}, \"userInfo\":" + notification + "}";

		Notification gcmNotificiation = Notification.createGcmNotification(gcmMessage);
		Notification appleNotificiation = Notification.createAppleNotification(appleMessage);

		JsonArray jsonArray = new JsonParser().parse(queueItemJson.get("tags").toString()).getAsJsonArray();

		List<String> tagList = new ArrayList<>();
		for (JsonElement e : jsonArray) {
			tagList.add(e.getAsString());
		}

		Set<String> tagSet = new HashSet<>();
		tagSet.addAll(tagList);

		hub.sendNotification(gcmNotificiation, tagSet);
		hub.sendNotification(appleNotificiation, tagSet);
	}
}