(ns yuruyomi.util.cache
  (:use simply)
  (:import [com.google.appengine.api.memcache MemcacheServiceFactory Expiration])
  )

;(defmacro with-memcache [[key expiration] & sex]
;  `(let [cache# (MemcacheServiceFactory/getMemcacheService)]
;     (if (.contains cache# ~key)
;       (.get cache# ~key)
;       (let [res# (do ~@sex)]
;         (.put cache# key res# (Expiration/byDeltaSeconds ~expiration))
;         res#
;         )
;       )
;     )
;  )

(defnk cache-fn [key f :expiration 0 & args]
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
  )

(defnk cache-val [key set-value :expiration 0]
  (cache-fn key (fn [] set-value) :expiration expiration)
  )

