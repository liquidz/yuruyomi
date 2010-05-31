(ns yuruyomi.cron.user
  (:use
     simply
     ;[yuruyomi.util seq cache]
     [yuruyomi.model book user]
     )
;  (:require
;     [clojure.contrib.seq-utils :as se]
;     [clojure.contrib.str-utils2 :as su2]
;     )
  )

(defn collect-user []
  (let [res (group :user (get-books))]
    (foreach
      (fn [user]
        (let [user-data (user res)
              rwhf (group :status user-data) ]
          (update-user-data
            :user (-> user name str)
            :ing (count (:ing rwhf))
            :wnt (count (:wnt rwhf))
            :has (count (:has rwhf))
            :fin (count (:fin rwhf))
            )
          )
        ) (keys res))
    )
  )

