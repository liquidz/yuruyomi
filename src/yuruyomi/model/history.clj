(ns yuruyomi.model.history
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
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
(defnk save-history [:user nil :title nil :author "" :date nil :before "new" :after nil]
  (when (! or-nil? user title date after)
    (ds-put (map-entity *history-entity-name*
                        :user user :title title :author author
                        :date date :before before :after after))
    )
  )


