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

(defn- make-query [kind & options]
  (let [op (apply array-map options)
        limit (:limit op)
        page (:page op)
        offset (:offset op)
        offset2 (if (and (nil? offset) (! nil? page) (! nil? limit))
                  (* limit (dec page))
                  offset)
        ks (-> op (dissoc :offset :limit :page) keys)
        q (query kind)
        fo (apply fetch-options
                 (concat
                   (if (nil? offset2) () (list :offset offset2))
                   (if (nil? limit) () (list :limit limit))
                   )
                 )
        ]
    (foreach
      #(when (! = "" (% op)) (add-filter q (-> % name str) = (% op)))
      (if (nil? ks) () ks)
      )
    [q fo]
    )
  )


; =find-entity
(defn find-entity [kind & options]
  (let [[q fo] (apply make-query (cons kind options))]
    (query-seq q fo)
    )
  )

(defn count-entity [kind & options]
  (let [[q fo] (apply make-query (cons kind options))]
    (count-entities q)
    )
  )


