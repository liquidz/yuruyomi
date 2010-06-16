(ns yuruyomi.clj-gae-ds-wrapper
  (:use
     simply
     am.ik.clj-gae-ds.core
     )
  (:require [clojure.contrib.str-utils2 :as su2])
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
        sort-key (:sort op)
        sort-asc-key (:sort-asc op)
        ks (-> op (dissoc :offset :limit :page :sort :sort-asc) keys)
        q (query kind)
        fo (if (and (! nil? limit) (! nil? offset2))
             (apply fetch-options
                    (concat
                      (if (nil? offset2) () (list :offset offset2))
                      (if (nil? limit) () (list :limit limit))
                      )
                    )
             nil)
        ]
    (foreach
      #(when (! = "" (% op))
         (let [key (-> % name str)]
           (cond
             (.endsWith key "-not") (add-filter q (su2/take key (- (count key) 4)) not= (% op))
             :else (add-filter q key = (% op))
             )
           )
         )
      (if (nil? ks) () ks)
      )
    (when (! nil? sort-key) (add-sort q sort-key :desc))
    (when (! nil? sort-asc-key) (add-sort q sort-asc-key :asc))
    [q fo]
    )
  )


; =find-entity
;(defn find-entity [kind & options]
(defn find-entity [& args]
  (let [[q fo] (apply make-query args)]
    (if (nil? fo)
      (query-seq q)
      (query-seq q fo)
      )
    )
  )

; =count-entity
;(defn count-entity [kind & options]
(defn count-entity [& args]
  (let [[q fo] (apply make-query args)]
    (count-entities q)
    )
  )


