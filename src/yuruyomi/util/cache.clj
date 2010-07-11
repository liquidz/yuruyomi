(ns yuruyomi.util.cache
  (:use simply)
  (:require [clojure.contrib.str-utils2 :as su2])
  (:import [com.google.appengine.api.memcache MemcacheServiceFactory Expiration])
  )

(defn- memcache-service
  ([] (memcache-service nil))
  ([ms] (if (nil? ms) (MemcacheServiceFactory/getMemcacheService) ms))
  )

(defnk cached? [key :ms nil]
  (.contains (memcache-service ms) key)
  )

(defnk get-cached-value [key :ms nil :default nil]
  (try
    (let [ms (memcache-service ms)]
      (if (cached? key :ms ms)
        (.get ms key)
        default
        )
      )
    (catch Exception _ default)
    )
  )

(defnk cache-val [key value :expiration 0 :default nil]
  (if (su2/blank? value)
    default
    (try
      (let [ms (MemcacheServiceFactory/getMemcacheService) ]
        (if (cached? key :ms ms)
          (.get ms key)
          (do
            (if (pos? expiration)
              (.put ms key value (Expiration/byDeltaSeconds expiration))
              (.put ms key value)
              )
            value
            )
          )
        )
      (catch Exception _ default)
      )
    )
  )

(defnk cache-fn [key f :expiration 0 :default nil & args]
  (let [ms (memcache-service)
        val (get-cached-value key :ms ms :default nil)]
    (if (! nil? val)
      val
      (let [res (apply f args)]
        (cache-val key res :default default :expiration expiration)
        res
        )
      )
    )
  )



