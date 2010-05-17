(ns yuruyomi.model.setting
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  )

(def *yuruyomi-entity* "yuruyomi-core")

; private ---
(defn- find-yuruyomi-entity [key]
  (-> (q *yuruyomi-entity*) (add-filter "key" = key) query-seq)
  )
(defn- get-yuruyomi-entity [key & default]
  (let [res (find-yuruyomi-entity key)]
    (if (empty? res)
      (if (empty? default) nil (first default))
      (get-prop (first res) :value)
      )
    )
  )

; public ---
; =get-max-id
(defn get-max-id [] (get-yuruyomi-entity "max-id" ""))

; =clear-max-id
(defn clear-max-id []
  (let [res (find-yuruyomi-entity "max-id")]
    (when (! empty? res)
      (-> res first get-key ds-delete)
      )
    )
  )

; =update-max-id
(defn update-max-id [max-id]
  (let [res (find-yuruyomi-entity "max-id")]
    (if (empty? res)
      (ds-put (map-entity *yuruyomi-entity* :key "max-id" :value (str max-id)))
      (let [x (first res)]
        (set-prop x :value (str max-id))
        (ds-put x)
        )
      )
    )
  )

