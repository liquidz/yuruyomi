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
        offset (:offset op)
        limit (:limit op)
        count? (:count? op)
        ks (-> op (dissoc :offset :limit :count?) keys)
        q (query kind)
        fo (apply fetch-options
                 (concat
                   (if (nil? offset) () (list :offset offset))
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


;  (let [op (apply array-map options)
;        offset (:offset op)
;        limit (:limit op)
;        count? (:count? op)
;        ks (-> op (dissoc :offset :limit :count?) keys)
;        q (query kind)
;        fo (apply fetch-options
;                 (concat
;                   (if (nil? offset) () (list :offset offset))
;                   (if (nil? limit) () (list :limit limit))
;                   )
;                 )
;        ]
;    (foreach
;      #(when (! = "" (% op)) (add-filter q (-> % name str) = (% op)))
;      ;(-> op (delete-map-key :offset :limit) keys)
;      (if (nil? ks) () ks)
;      )
;    (println "count? = " count?)
;    (if (and (! nil? count?) count?)
;      (count-entities q)
;      (query-seq q fo)
;      )
;    )
;  )


