package live.page.hubd.system.db.utils;

import live.page.hubd.system.json.Json;

public class Updater {
    final Json updater = new Json();

    public Updater set(String key, Object value) {
        Json set = updater.getJson("$set");
        if (set == null) {
            set = new Json();
        }
        set.put(key, value);
        updater.put("$set", set);
        return this;
    }


    public Updater push(String key, Object value) {
        Json push = updater.getJson("$push");
        if (push == null) {
            push = new Json();
        }
        push.put(key, value);
        updater.put("$push", push);
        return this;

    }

    public Updater pull(String key, Object value) {
        Json pull = updater.getJson("$pull");
        if (pull == null) {
            pull = new Json();
        }
        pull.put(key, value);
        updater.put("$pull", pull);
        return this;

    }

    public Updater addToSet(String key, Object value) {
        Json addToSet = updater.getJson("$addToSet");
        if (addToSet == null) {
            addToSet = new Json();
        }
        addToSet.put(key, value);
        updater.put("$addToSet", addToSet);
        return this;

    }

    public Updater setOnInsert(String key, Object value) {
        Json setOnInsert = updater.getJson("$setOnInsert");
        if (setOnInsert == null) {
            setOnInsert = new Json();
        }
        setOnInsert.put(key, value);
        updater.put("$setOnInsert", setOnInsert);
        return this;

    }

    public Updater inc(String key, int value) {
        Json inc = updater.getJson("$inc");
        if (inc == null) {
            inc = new Json();
        }
        inc.put(key, value);
        updater.put("$inc", inc);
        return this;
    }

    public boolean isEmpty() {
        return updater.isEmpty();
    }

    public Json get() {
        return updater;
    }

}
