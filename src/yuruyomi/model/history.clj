(ns yuruyomi.model.history
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     )
  )

(def *history-entity-name* "history")

; =find-history
(defn find-history [& args]
  (map
    entity->map
    (apply find-entity (cons *history-entity-name* args))
    )
  )

; =save-history
(defnk save-history [:user nil :title nil :author "" :date nil :before "new" :after nil :text nil]
  (when (! or-nil? user title date after)
    (ds-put (map-entity *history-entity-name*
                        :user user :title title :author author
                        :date date :before before :after after
                        :text text))
    )
  )

(defn count-histories [& args]
  (apply count-entity (cons *history-entity-name* args))
  )

(defnk get-active-user [:limit 100]
  (let [histories (find-history :sort "date" :limit limit :offset 0)]
    (r-fold (fn [h res]
              (if (nil? (se/find-first #(= % (:user h)) res))
                (cons (:user h) res)
                res
                )
              ) () histories)
    )
  )

