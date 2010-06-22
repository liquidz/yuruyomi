(ns yuruyomi.cron.user
  (:use
     simply
     ;[simply :only [group]]
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
            :reading (count (:reading rwhf))
            :want (count (:want rwhf))
            :have (count (:have rwhf))
            :finish (count (:finish rwhf))
            )
          )
        ) (keys res))
    )
  )

