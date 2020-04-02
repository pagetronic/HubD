db.getCollection('Stats').aggregate([
    {"$match": {"gone": {"$ne": null}}},
    {
        "$group": {
            "_id": {"ip": "$ip", "ua": "$ua"},
            "id": {"$first": "$_id"},
            "unique": {"$first": {"ip": "$ip", "ua": "$ua"}},
            "view": {"$sum": 1},
            "boundrate": {"$first": {"$subtract": ["$gone", "$date"]}}
        }
    },
    {"$match": {"boundrate": {"$lt": 60000}}},
    {"$group": {"_id": null, "unique": {"$sum": 1}, "view": {"$sum": "$view"}, "boundrate": {"$avg": "$boundrate"}}},
    {
        "$project": {
            "_id": false, "unique": "$unique", "view": "$view",
            "boundrate": {
                "$floor": {
                    "$divide": [
                        "$boundrate",
                        1000
                    ]
                }
            }
        }
    }
])