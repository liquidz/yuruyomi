(ns yuruyomi.model.user
  (:use
     simply.core
     am.ik.clj-gae-ds.core
     yuruyomi.clj-gae-ds-wrapper
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

(defnk update-user-data [:user nil :reading nil :want nil :have nil :finish nil]
  (when-not (nil? user)
    (let [res (find-user :user user)]
      (if (empty? res)
        (ds-put (map-entity *user-entity-name* :user user :reading reading :want want :have have :finish finish))
        (let [e (first res)]
          (when-not (nil? reading) (set-prop e :reading reading))
          (when-not (nil? want) (set-prop e :want want))
          (when-not (nil? have) (set-prop e :have have))
          (when-not (nil? finish) (set-prop e :finish finish))
          (ds-put e)
          )
        )
      )
    )
  )


