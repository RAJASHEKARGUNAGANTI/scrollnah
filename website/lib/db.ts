// MongoDB connection, shared between requests (Vercel reuses the same server process for a while).
import { MongoClient, type Db } from "mongodb";

declare global {
  // Kept on globalThis so hot reloads in `npm run dev` don't open a new connection every time.
  var _scrollnahDb: Promise<Db> | undefined;
}

const TWO_YEARS = 2 * 365 * 24 * 60 * 60;

/** The database, or null when MONGODB_URI isn't set yet (the forms then say "not switched on yet"). */
export function getDb(): Promise<Db> | null {
  const uri = process.env.MONGODB_URI;
  if (!uri) return null;
  if (!globalThis._scrollnahDb) {
    // Give up after 8 s instead of the default 30 s, so a wrong address fails with a clear error quickly.
    globalThis._scrollnahDb = new MongoClient(uri, { maxPoolSize: 5, serverSelectionTimeoutMS: 8000 })
      .connect()
      .then(async (client) => {
        const db = client.db(process.env.MONGODB_DB || "scrollnah");
        // Indexes (safe to run again). TTL indexes make MongoDB delete old documents by itself:
        // feedback after 2 years (as the privacy page promises), spam-limit entries after 1 hour.
        await Promise.all([
          db.collection("feedback").createIndex({ createdAt: 1 }, { expireAfterSeconds: TWO_YEARS }),
          db.collection("rate_limits").createIndex({ at: 1 }, { expireAfterSeconds: 60 * 60 }),
          db.collection("rate_limits").createIndex({ key: 1 }),
        ]);
        return db;
      })
      .catch((err) => {
        globalThis._scrollnahDb = undefined; // try again on the next request
        throw err;
      });
  }
  return globalThis._scrollnahDb;
}
