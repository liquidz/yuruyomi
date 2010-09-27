(ns yuruyomi.clj-gae-ds-wrapper
  (:use
     ;[simply :only [fold foreach !]]
     [simply core]
     [am.ik.clj-gae-ds.core :only [entity-map get-prop fetch-options add-filter 
                                   add-sort query query-seq count-entities
                                   ds-get create-key ds-delete get-key get-id]]

     )
  (:require [clojure.contrib.string :as st])
  )

; =entity->map
(defn entity->map [e]
  (assoc 
    (fold
      (fn [x res] (assoc res (-> x first keyword) (second x)))
      {} (-> e entity-map seq)
      )
    :id (-> e get-key get-id)
    )
  )

; =get-entity
(defn get-entity [kind id]
  (ds-get (create-key kind (if (string? id) (Long/parseLong id) id)))
  )

; =delete-entity
(defn delete-entity [kind id]
  (ds-delete (create-key kind (if (string? id) (Long/parseLong id) id)))
  )
 
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

; =make-fetch-options
(defn- make-fetch-options [limit offset]
  (if (and (! nil? limit) (! nil? offset))
    (apply fetch-options
           (concat
             (if (nil? offset) () (list :offset offset))
             (if (nil? limit) () (list :limit limit))
             )
           )
    nil)
  )

; =make-query
(defn- make-query [kind & options]
  (let [op (apply array-map options)
        limit (:limit op)
        page (:page op)
        offset (:offset op)
        offset2 (if (and (nil? offset) (! nil? page) (! nil? limit))
                  (* limit (dec page))
                  offset)
        q (query kind)
        fo (make-fetch-options limit offset2)
        ]
    (foreach
      #(when-not (= "" (% op))
         (let [key (-> % name str)]
           (cond
             (.endsWith key "-not") (add-filter q (st/take key (- (count key) 4)) not= (% op))
             :else (add-filter q key = (% op))
             )
           )
         )
      (-> op (dissoc :offset :limit :page :sort :sort-asc) keys)
      )
    (when (! nil? (:sort op)) (add-sort q (:sort op) :desc))
    (when (! nil? (:sort-asc op)) (add-sort q (:sort-asc op) :asc))
    [q fo]
    )
  )

; =find-entity
(defn find-entity [& args]
  (let [[q fo] (apply make-query args)]
    (if (nil? fo)
      (query-seq q)
      (query-seq q fo)
      )
    )
  )

; =count-entity
(defn count-entity [& args]
  (let [[q fo] (apply make-query args)]
    (count-entities q)
    )
  )


