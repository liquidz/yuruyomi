(ns yuruyomi.model.history
  (:use
     [simply core]
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  (:require
     [clojure.contrib.seq :as se]
     )
  )

(def *history-entity-name* "history")

; =find-history
(defn find-history [& args]
  (map
    entity->map
    ;#(assoc (entity->map %) :id (-> % get-key get-id))
    (apply find-entity (cons *history-entity-name* args))
    )
  )

; =save-history
(defnk save-history [:user nil :title nil :author "" :date nil :before "new" :after nil :text nil :book-id nil]
  ;(when-not (or-nil? user title date after)
  (when-not (some #(nil? %) [user title date after])
    (ds-put (map-entity *history-entity-name*
                        :user user :title title :author author
                        :date date :before before :after after
                        :text text :book-id book-id))
    )
  )

(defn count-histories [& args]
  (apply count-entity (cons *history-entity-name* args))
  )

(defnk get-active-user [:limit 100]
  (let [histories (find-history :sort "date" :limit limit :offset 0)]
    (reverse
      (fold (fn [h res]
              (if (nil? (se/find-first #(= % (:user h)) res))
                (cons (:user h) res)
                res
                )
              ) () histories)
      )
    )
  )

