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
      (fn [user-name]
        (let [user-data (get res user-name)
              rwhf (group-by :status user-data)]
          (update-user-data
            :user user-name
            :reading (count (get rwhf "reading"))
            :want (count (get rwhf "want"))
            :have (count (get rwhf "have"))
            :finish (count (get rwhf "finish"))
            )
          )
        ) (keys res))
    )
  )

