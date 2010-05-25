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

(defn delete-map-key [m & ks]
  (let [res (remove #(some (fn [k] (= k %)) ks) (keys m))]
    (apply array-map (interleave res (map #(% m) res)))
    )
  )

(defn find-entity [kind & options]
  (let [op (apply array-map options)
        offset (:offset op)
        limit (:limit op)
        q (query kind)
        ]
    (foreach
      #(when (! = "" (% op)) (add-filter q (-> % name str) = (% op)))
      (-> op (delete-map-key :offset :limit) keys)
      )
    (query-seq q)
    )
  )

