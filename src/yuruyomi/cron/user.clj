(ns yuruyomi.cron.user
  (:use
     [simply core]
     [yuruyomi.model.book :only [get-books]]
     [yuruyomi.model.user :only [update-user-data]]
     )
  )

(defn collect-user []
  (let [res (group-by :user (get-books))]
    (foreach
      (fn [user]
        (let [user-data (user res)
              rwhf (group-by :status user-data)]
          (update-user-data
            :user (-> user name str)
            :reading (count (:reading rwhf))
            :want (count (:want rwhf))
            :have (count (:have rwhf))
            :finish (count (:finish rwhf))
            )
          )
        ) (keys res))
    )
  )

