/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.messages;

import com.mongodb.client.model.*;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.system.socket.SocketPusher;
import live.page.web.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MessagesUtils {


	public static Json getMessage(Users user, String id, String paging) {

		List<Bson> pipeline = new ArrayList<>();

		Aggregator grouper = new Aggregator("subject", "recipients", "messages");

		pipeline.add(Aggregates.match(Filters.and(Filters.eq("_id", id), Filters.eq("recipients", user.getId()))));
		pipeline.add(Aggregates.lookup("Users", "recipients", "_id", "recipients"));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("date", new Json("$arrayElemAt", Arrays.asList("$messages.date", 0)))
				.put("last", new Json("$arrayElemAt", Arrays.asList("$messages.date", -1)))
				.put("count", new Json("$size", "$messages"))
				.put("recipients", new Json("$filter", new Json("input", "$recipients").put("as", "recipients").put("cond",
						new Json("$ne", Arrays.asList("$$recipients._id", user.getId()))
				)))
		));
		pipeline.add(Aggregates.unwind("$recipients"));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("recipients", new Json("_id", true).put("name", true)

						.put("avatar",
								new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$type", "$recipients.avatar"), "string")),
										new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$recipients.avatar")),
										Settings.getLogo()
								))
						)


				))
		);

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("recipients", "$recipients"))));


		pipeline.add(Aggregates.unwind("$messages", new UnwindOptions().includeArrayIndex("index")));


		pipeline.add(Aggregates.lookup("Users", "messages.user", "_id", "messages.user"));
		pipeline.add(Aggregates.lookup("BlobFiles", "messages.file", "_id", "messages.file"));

		pipeline.add(Aggregates.unwind("$messages.file", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.unwind("$messages.user", new UnwindOptions()));

		pipeline.add(Aggregates.project(
				grouper.getProjection()
						.put("messages",
								new Json()
										.put("user", new Json("id", "$messages.user._id").put("name", "$messages.user.name").put("avatar",
												new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$type", "$messages.user.avatar"), "string")),
														new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$messages.user.avatar")),
														Settings.getLogo()
												))
										))

										.put("date", "$messages.date")
										.put("unread",
												new Json("$cond", Arrays.asList(new Json("$gt", Arrays.asList(
														new Json("$size", new Json("$filter", new Json("input", "$messages.unread").put("cond",
																new Json("$in", Arrays.asList(user.getId(), "$messages.unread"))
														))), 0)), true, false))
										)
										.put("text", "$messages.text")

										.put("file", new Json("$cond", Arrays.asList(
												new Json("$eq", Arrays.asList("$messages.file._id", new BsonUndefined())), null,
												new Json()
														.put("id", "$messages.file._id")
														.put("name", "$messages.file.name")
														.put("type", "$messages.file.type")
														.put("src", new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$messages.file._id")))))
										)
										.put("position", "$index")
										.put("id", new Json("$concat", Arrays.asList("$_id", "_", new Json("$substr", Arrays.asList("$index", 0, -1)))))
						)

				)
		);

		pipeline.add(Aggregates.sort(Sorts.ascending("index")));
		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("messages", "$messages"))));


		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));


		return Db.aggregate("Messages", pipeline).first();
	}

	public static Json getMessages(Users user, String paging, String sort, int limit, boolean archives) {


		List<Bson> pipeline = new ArrayList<>();

		Paginer paginer = new Paginer(paging, sort == null ? "-last" : sort, limit);
		Aggregator grouper = new Aggregator("date", "last", "subject", "text", "recipients", "count", "unread");

		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("recipients", user.getId()));
		Bson pager = paginer.getFilters();
		if (pager != null) {
			filters.add(pager);
		}

		pipeline.add(Aggregates.match(Filters.and(filters)));
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());

		pipeline.add(Aggregates.unwind("$messages", new UnwindOptions().includeArrayIndex("index")));
		pipeline.add(Aggregates.sort(Sorts.ascending("index")));
		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("text", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$messages.text", null)), "", "$messages.text"))),
				Accumulators.push("messages", "$messages")
		)));


		pipeline.add(Aggregates.lookup("Users", "recipients", "_id", "recipients"));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("count", new Json("$size", "$messages"))
				.put("unread", new Json("$size", new Json("$filter", new Json("input", "$messages").put("as", "messages").put("cond",
						new Json("$in", Arrays.asList(user.getId(), "$$messages.unread"))
				))))
				.put("recipients", new Json("$filter", new Json("input", "$recipients").put("as", "recipients").put("cond",
						new Json("$ne", Arrays.asList("$$recipients._id", user.getId()))
				)))
				.put("text",
						new Json("$reduce", new Json("input", "$text").put("initialValue", "").put("in", new Json("$concat", Arrays.asList("$$value", "...", "$$this"))))
				)
		));
		int max = 250;
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("text", new Json("$substr", Arrays.asList("$text", 3, new Json("$subtract", Arrays.asList(new Json("$strLenCP", "$text"), 3)))))
		));

		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("text",
						new Json("$cond", Arrays.asList(new Json("$gt", Arrays.asList(new Json("$strLenCP", "$text"), max)),
								new Json("$substr", Arrays.asList("$text",
										new Json("$subtract", Arrays.asList(new Json("$strLenCP", "$text"), max)),
										max
								)),
								"$text")))
		));

		pipeline.add(Aggregates.unwind("$recipients"));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("recipients", new Json("_id", true).put("name", true)
						.put("avatar",
								new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList(new Json("$type", "$recipients.avatar"), "string")),
										new Json("$concat", Arrays.asList(Settings.getCDNHttp() + "/files/", "$recipients.avatar")),
										Settings.getLogo()
								))

						)
				))
		);

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(
				Accumulators.push("recipients", "$recipients")
		)));


		pipeline.add(paginer.getLastSort());

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));


		return paginer.getResult("Messages", pipeline);
	}

	public static Json sendMessage(String id, Users user, String text, String file, String subject, List<String> recipients) {

		if (subject != null) {
			subject = Fx.normalizePost(subject);
			subject = Fx.truncate(subject, 55);
		}
		text = Fx.normalizePost(text);

		Date date = new Date();


		List<String> unreads = (id == null) ? new ArrayList<>(recipients) : Db.find("Messages", Filters.eq("_id", id)).first().getList("recipients");

		unreads.remove(user.getId());

		Json message = new Json("user", user.getId()).put("date", date).put("unread", unreads).put("text", text).put("file", file);

		Json messages = null;

		if (id == null) {
			if (recipients == null || recipients.size() == 0) {
				return new Json("error", "INVALID_DATA");
			}
			if (subject == null || subject.length() == 0) {
				subject = Fx.truncate(text, 50);
			}
			recipients.add(user.getId());
			messages = new Json("recipients", recipients).put("user", user.getId()).put("date", date).put("last", date).put("subject", subject).add("messages", message);
			Db.save("Messages", messages);

		} else {
			messages = Db.findOneAndUpdate("Messages",
					Filters.and(Filters.eq("_id", id), Filters.eq("recipients", user.getId())),
					new Json()
							.put("$push", new Json("messages", message))
							.put("$set", new Json("last", date))
					, new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
			);
		}

		if (messages == null) {
			return new Json("error", "INVALID_DATA");
		}

		Json data = new Json();
		data.put("count", messages.getListJson("messages").size());
		int position = messages.getListJson("messages").size() - 1;
		message.put("position", position);
		data.put("id", messages.getId());

		message.put("user", new Json("id", user.getId()).put("name", user.getString("name")).put("avatar",
				(user.getString("avatar", null) == null) ? Settings.getLogo() : Settings.getCDNHttp() + "/files/" + user.getString("avatar")

		));
		if (file != null) {
			Json filedb = Db.find("BlobFiles", Filters.and(Filters.eq("_id", file), Filters.eq("user", user.getId()))).first();
			if (filedb == null) {
				return new Json("error", "INVALID_DATA");
			}
			message.put("file", new Json("id", filedb.getId()).put("name", filedb.getString("name")).put("type", filedb.getString("type")).put("src", Settings.getCDNHttp() + "/files/" + filedb.getId()));
		}

		data.put("message", message);
		recipients = messages.getList("recipients");
		message.put("unread", true);
		message.put("id", messages.getId() + "_" + position);
		data.put("subject", messages.getString("subject"));
		for (String recipient : recipients) {
			SocketPusher.send("user", recipient,
					new Json("action", "message")
							.put("message", data)
							.put("count", countUnreads(recipient))
			);
		}

		message.put("unread", false);
		return data;
	}

	public static void readMessage(String user_id, String idmessage, boolean read) {
		if (idmessage != null) {
			if (read) {
				Db.updateOne("Messages", Filters.eq("_id", idmessage.split("_")[0]),
						new Json("$pull", new Json("messages." + Integer.valueOf(idmessage.split("_")[1]) + ".unread", user_id))
				);
			} else {
				Db.updateOne("Messages", Filters.and(Filters.ne("messages." + Integer.valueOf(idmessage.split("_")[1]) + ".unread", user_id), Filters.eq("_id", idmessage.split("_")[0])),
						new Json("$push", new Json("messages." + Integer.valueOf(idmessage.split("_")[1]) + ".unread", user_id))
				);
			}
		} else {
			//markall has read
			while (Db.updateMany("Messages", Filters.eq("messages.unread", user_id),
					new Json("$pull", new Json("messages.$.unread", user_id))
			).getModifiedCount() > 0) ;

		}
		pushCount(user_id);

	}

	public static int countUnreads(String user_id) {
		Json unread = Db.aggregate("Messages", Arrays.asList(
				Aggregates.match(Filters.eq("messages.unread", user_id)),
				Aggregates.project(new Json().put("unread", "$messages.unread")),
				Aggregates.unwind("$unread"),
				Aggregates.match(Filters.eq("unread", user_id)),
				Aggregates.group("", Accumulators.sum("count", 1))
		)).first();
		if (unread == null) {
			return 0;
		}
		return unread.getInteger("count", 0);
	}

	public static void pushCount(String user_id) {
		SocketPusher.send("user", user_id, new Json("action", "message").put("count", countUnreads(user_id)));
	}
}
