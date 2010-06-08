(ns yuruyomi.model.setting
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  (:require [clojure.contrib.str-utils2 :as su2])
  )

(def *yuruyomi-entity* "yuruyomi-core")

; private ---
(defn- find-setting [key]
  (find-entity *yuruyomi-entity* :key key)
  )
;(defn- find-yuruyomi-entity [key]
;  (-> (q *yuruyomi-entity*) (add-filter "key" = key) query-seq)
;  )
;(defn- get-yuruyomi-entity [key & default]
;  (let [res (find-yuruyomi-entity key)]
;    (if (empty? res)
;      (if (empty? default) nil (first default))
;      (get-prop (first res) :value)
;      )
;    )
;  )
(defn- get-setting [key & default]
  (let [res (find-setting key)]
    (if (empty? res)
      (if (empty? default) nil (first default))
      (get-prop (first res) :value)
      )
    )
  )

(defn- save-setting [key value]
  (ds-put (map-entity *yuruyomi-entity* :key key :value value))
  )

(defn- update-setting [entity new-value]
  (set-prop entity :value new-value)
  (ds-put entity)
  )

; public ---
; =get-max-id
(defn get-max-id [] (get-setting "max-id" ""))

; =clear-max-id
(defn clear-max-id []
  (let [res (find-setting "max-id")]
    (when (! empty? res)
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
;      (ds-put (map-entity *yuruyomi-entity* :key "max-id" :value (str max-id)))
;      (let [x (first res)]
;        (set-prop x :value (str max-id))
;        (ds-put x)
;        )
      )
    )
  )


