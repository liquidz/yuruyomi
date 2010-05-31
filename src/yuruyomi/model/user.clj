(ns yuruyomi.model.user
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  )

(def *user-entity-name* "history")

; =find-user
(def find-user (partial find-entity *user-entity-name*))
(defn get-user [& args]
  (map entity->map (apply find-user args))
  )


;(defn change-user-data [user target f]
(defn change-user-data [user & kvs]
  (let [res (find-user :user user)
        e (if (empty? res) (map-entity *user-entity-name* 0 0 0 0) (first res))
        update-data (apply array-map kvs)
        ]
    (foreach
      #(set-prop e % ((% update-data) (get-prop e %)))
      (keys update-data)
      )
    ;(set-prop e target (f (get-prop e target)))
    (ds-put e)
    )
  )

(defnk update-user-data [:user nil :ing nil :wnt nil :has nil :fin nil]
  (when (! nil? user)
    (println "ing = " ing)
    (println "wnt = " wnt)
    (println "has = " has)
    (println "fin = " fin)
    (let [res (find-user :user user)]
      (if (empty? res)
        (ds-put (map-entity *user-entity-name* user ing wnt has fin))
        (let [e (first res)]
          (println "kiteru?")
          (when (! nil? ing) (set-prop e :ing ing))
          (when (! nil? wnt) (set-prop e :ing wnt))
          (when (! nil? has) (set-prop e :ing has))
          (when (! nil? fin) (set-prop e :ing fin))
          (ds-put e)
          )
        )
      )
    )
  )


