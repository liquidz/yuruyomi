(ns yuruyomi.model.user
  (:use
     simply
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     )
  )

(def *user-entity-name* "user")

; =find-user
(def find-user (partial find-entity *user-entity-name*))
(defn get-user [& args]
  (map entity->map (apply find-user args))
  )


(defn change-user-data [user & kvs]
  (let [res (find-user :user user)
        e (if (empty? res)
            (map-entity *user-entity-name* :user user :reading 0 :want 0 :have 0 :finish 0)
            (first res))
        update-data (apply array-map kvs)
        ]
    (foreach
      #(let [tmp ((% update-data) (get-prop e %))]
         (set-prop e % (if (neg? tmp) 0 tmp))
         )
      (keys update-data)
      )
    (ds-put e)
    )
  )

(defnk update-user-data [:user nil :ing nil :wnt nil :has nil :fin nil]
  (when (! nil? user)
    (let [res (find-user :user user)]
      (if (empty? res)
        (ds-put (map-entity *user-entity-name* :user user :reading ing :want wnt :have has :finish fin))
        (let [e (first res)]
          (when (! nil? ing) (set-prop e :reading ing))
          (when (! nil? wnt) (set-prop e :want wnt))
          (when (! nil? has) (set-prop e :have has))
          (when (! nil? fin) (set-prop e :finish fin))
          (ds-put e)
          )
        )
      )
    )
  )


