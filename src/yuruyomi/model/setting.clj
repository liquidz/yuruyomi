(ns yuruyomi.model.setting
  (:use
     simply.core
     am.ik.clj-gae-ds.core
     yuruyomi.clj-gae-ds-wrapper
     )
  (:require 
     [clojure.contrib.string :as st]
     [clojure.contrib.logging :as log]
     )
  )

(def *yuruyomi-entity* "yuruyomi-core")

; private ---

(defn- string->long [s] (Long/parseLong s))

(defn- find-setting [key]
  (find-entity *yuruyomi-entity* :key key)
  )
(defn- get-setting [key & default]
  (let [res (find-setting key)]
    (if (empty? res)
      (if (empty? default) nil (first default))
      (get-prop (first res) :value)
      )
    )
  )

(defn- save-setting [key value]
  (try
    (ds-put (map-entity *yuruyomi-entity* :key key :value value))
    (catch Exception e (str "setting save: " key " = " value ", " (.getMessage e)))
    )
  )

(defn- update-setting [entity new-value]
  (try
    (set-prop entity :value new-value)
    (ds-put entity)
    (catch Exception e
      (log/warn (str "setting update: " (get-prop entity :key) " = " new-value ", " (.getMessage e))))
    )
  )

; public ---
; =get-max-id
(defn get-max-id [] 
  (let [res (get-setting "max-id" "")]
    (if (st/blank? res) -1 (string->long res))
    )
  )

; =clear-max-id
(defn clear-max-id []
  (let [res (find-setting "max-id")]
    (when-not (empty? res)
      (-> res first get-key ds-delete)
      )
    )
  )

; =update-max-id
(defn update-max-id [max-id]
  (let [res (find-setting "max-id")]
    (if (empty? res)
      (save-setting "max-id" (str max-id))
      (update-setting (first res) (str max-id))
      )
    )
  )


