(ns yuruyomi.cron.user
  (:use
     [simply :only [group foreach]]
     [yuruyomi.model.book :only [get-books]]
     [yuruyomi.model.user :only [update-user-data]]
     )
  )

(defn collect-user []
  (let [res (group :user (get-books))]
    (foreach
      (fn [user]
        (let [user-data (user res)
              rwhf (group :status user-data) ]
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

