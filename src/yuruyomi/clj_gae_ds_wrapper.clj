(ns yuruyomi.clj-gae-ds-wrapper
  (:use
     simply
     am.ik.clj-gae-ds.core
     )
  )

; =entity->map
(defn entity->map [e]
  (fold
    (fn [x res] (assoc res (-> x first keyword) (second x)))
    {} (-> e entity-map seq)
    )
  )

; =params
(defn params [req & key-names]
  (cond
    (empty? key-names) nil
    :else (map #(get-in req [:params %]) key-names)
    )
  )
(defn param [req key-name] (first (params req key-name)))

; =get-props
(defn get-props [entity & keys]
  (cond
    (empty? keys) nil
    :else (map #(get-prop entity %) keys)
    )
  )

; =add-filters
(defn add-filters [query & conditions]
  {:pre [(zero? (mod (count conditions) 3))]}
  (foreach #(apply add-filter (cons query %)) (partition conditions 3))
  query
  )

