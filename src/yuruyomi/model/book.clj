(ns yuruyomi.model.book
  (:use
     simply
     simply.date
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper seq]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     )
  )

(def *book-entity-name* "book")

(defn- entity->book [e]
  (-> e entity->map (assoc :id (-> e get-key get-id)))
  )

(defnk- find-books [:user "" :title "" :author "" :date "" :flag ""]
  (let [q (query *book-entity-name*)]
    (when (! = "" user) (add-filter q "user" = user))
    (when (! = "" title) (add-filter q "title" = title))
    (when (! = "" author) (add-filter q "author" = author))
    (when (! = "" date) (add-filter q "date" = date))
    (when (! = "" flag) (add-filter q "flag" = flag))
    (query-seq q)
    )
  )

(defn get-user-books [user-name] (map entity->book (find-books :user user-name)))
(defn get-all-books [] (map entity->book (find-books)))

(defn save-book [name title author flag]
  (let [date (calendar-format :year "-" :month "-" :day " " :hour ":" :minute ":" :second)]
    ; 再読がありえるから fin は同じのがあっても登録/更新
    ; wntの場合でingに既に同じものが入っているのはおかしいからNG
    (if (and (or (= flag "fin") (zero? (count (find-books :title title :author author :flag flag))))
               (or (! = flag "wnt") (zero? (count (find-books :title title :author author :flag "ing")))))
      (let [books (group #(get-prop % :flag) (find-books :user name))
            update-target (case flag
                            ; ing <= wnt or has
                            "ing" (concat (:wnt books) (:has books))
                            "wnt" ()
                            ; fin <= ing, wnt or has
                            "fin" (concat (:ing books) (:wnt books) (:has books))
                            ; has <= wnt
                            "has" (concat (:ing books) (:wnt books))
                            )
            x (se/find-first #(and (= title (get-prop % :title))
                                   (if (and (! su2/blank? author) (! su2/blank? (get-prop % :author)))
                                     (= author (get-prop % :author))
                                     true
                                     )
                                   )
                             update-target)
            ]
        (cond
          (nil? x) (ds-put (map-entity *book-entity-name* :user name :title title
                                       :author author :date date :flag flag))
          :else (do
                  (set-prop x :flag flag)
                  (set-prop x :date date)
                  ; 著者が登録されていなくて、今回入力されている場合は登録する
                  (if (and (! su2/blank? author) (su2/blank? (get-prop x :author)))
                    (set-prop x :author author))
                  (ds-put x)
                  )
          )
        true
        )
      false
      )
    )
  )

(defn delete-book [id]
  (let [target (se/find-first #(= id (-> % get-key get-id str)) (find-books))]
    (cond
      (! nil? target) (do (-> target get-key ds-delete) true)
      :else false
      )
    )
  )

