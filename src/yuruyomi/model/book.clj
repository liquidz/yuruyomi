(ns yuruyomi.model.book
  (:use
     simply simply.date
     am.ik.clj-gae-ds.core
     am.ik.clj-aws-ecs
     [yuruyomi clj-gae-ds-wrapper]
     [yuruyomi.util seq cache]
     [yuruyomi.model history]
     )
  (:require
     keys
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [clojure.zip :as z]
     [clojure.contrib.zip-filter :as zf]
     [clojure.contrib.zip-filter.xml :as zfx]
     )
  )

(def *book-entity-name* "book")

(defn- entity->book [e]
  (-> e entity->map (assoc :id (-> e get-key get-id)))
  )

(defnk- find-books [:user "" :title "" :author "" :date "" :status ""]
  (let [q (query *book-entity-name*)]
    (when (! = "" user) (add-filter q "user" = user))
    (when (! = "" title) (add-filter q "title" = title))
    (when (! = "" author) (add-filter q "author" = author))
    (when (! = "" date) (add-filter q "date" = date))
    (when (! = "" status) (add-filter q "status" = status))
    ;(when (pos? id) (add-filter q "id" = id))
    (query-seq q)
    )
  )

(defn get-book-image [title author]
  (cache-fn
    (url-encode title)
    (fn []
      (let [req (make-requester "ecs.amazonaws.jp" keys/*aws-access-key* keys/*aws-secret-key*)
            res (z/xml-zip (item-search-map req "Books" title {"Author" author, "ResponseGroup" "Images"}))
            ]
        (println "kiteru")
        (zfx/xml1-> res zf/children :Items :Item :MediumImage :URL zfx/text)
        )
      )
    :expiration 86400
    )
  )

(defn get-user-books [user-name] (map entity->book (find-books :user user-name)))
(defn get-all-books [] (map entity->book (find-books)))

(defn save-book [tweet]
  (let [name (:from-user tweet)
        title (:title tweet)
        author (:author tweet)
        status (:status tweet)
        icon (:profile-image-url tweet)
        date (calendar-format :year "-" :month "-" :day " " :hour ":" :minute ":" :second)
        ;image (get-book-image title author)
        ]
    ; 再読がありえるから fin は同じのがあっても登録/更新
    ; wntの場合でingに既に同じものが入っているのはおかしいからNG
    (if (and (or (= status "status") (zero? (count (find-books :user name :title title :author author :status status))))
               (or (! = status "wnt") (zero? (count (find-books :user name :title title :author author :status "ing")))))
      (let [books (group #(get-prop % :status) (find-books :user name))
            update-target (case status
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
          (nil? x) (do
                     (ds-put (map-entity *book-entity-name* :user name :title title
                                         :author author :date date :status status :icon icon)); :image image))
                     (save-history :user name :title title :author author :date date
                                   :before "new" :after status)
                     )
          :else (let [before-status (get-prop x :status)]
                  (set-prop x :status status)
                  (set-prop x :date date)
                  ; 著者が登録されていなくて、今回入力されている場合は登録する
                  (if (and (! su2/blank? author) (su2/blank? (get-prop x :author)))
                    (set-prop x :author author))
                  (if (su2/blank? (get-prop x :icon))
                    (set-prop x :icon icon))
                  ;(if (su2/blank? (get-prop x :image))
                  ;  (set-prop x :image image)
                  ;  )
                  (ds-put x)

                  (save-history :user name :title title :author (get-prop x :author)
                                :date date :before before-status :after status)
                  )
          )
        true
        )
      false
      )
    )
  )

(defn delete-book [id]
  (try-with-boolean
    (ds-delete (create-key *book-entity-name* (Long/parseLong id)))
    )
  )

