(ns yuruyomi.util.cache
  (:use simply)
  (:import [com.google.appengine.api.memcache MemcacheServiceFactory Expiration])
  )

(defnk cache-fn [key f :expiration 0 :default nil & args]
  (try
    (let [c (MemcacheServiceFactory/getMemcacheService)]
      (if (.contains c key)
        (.get c key)
        (let [res (apply f args)]
          (if (pos? expiration)
            (.put c key res (Expiration/byDeltaSeconds expiration))
            (.put c key res)
            )
          res
          )
        )
      )
    (catch Exception _ default)
    )
  )

(defnk cache-val [key set-value :expiration 0 :default nil]
  (cache-fn key (fn [] set-value) :expiration expiration :default default)
  )

