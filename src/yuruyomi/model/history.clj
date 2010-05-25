(ns yuruyomi.model.history
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  )

(def *history-entity-name* "history")

; =find-history
(def find-history (partial find-entity *history-entity-name*))
(comment
(defnk find-history [:user ""]
  (let [q (query *history-entity-name*)]
    (when (! = user "") (add-filter q "user" = user))
    (map entity->map (query-seq q))
    )
  ))

; =save-history
(defnk save-history [:user nil :title nil :author "" :date nil :before "new" :after nil]
  (when (! or-nil? user title date after)
    (ds-put (map-entity *history-entity-name*
                        :user user :title title :author author
                        :date date :before before :after after))
    )
  )


