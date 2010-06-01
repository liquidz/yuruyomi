(ns yuruyomi.model.book
  (:use
     simply simply.date
     am.ik.clj-gae-ds.core
     am.ik.clj-aws-ecs
     [yuruyomi clj-gae-ds-wrapper]
     [yuruyomi.util seq cache]
     [yuruyomi.model history user]
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

(defnk filters [col :ignore-nil? true & preds]
  (filter #(every? (fn [pre] (if (nil? pre) (if ignore-nil? true false) (pre %))) preds) col)
  )

(defnk- find-books [:user "" :title "" :author "" :date "" :status ""
                    :user-like "" :title-like "" :author-like "" :date-like ""
                    ]
  (let [res (find-entity *book-entity-name* :user user :title title :author author :date date :status status)]
    (if (some #(! = "" %) [user-like title-like author-like date-like])
      (filters
        res
        (if (! = "" user-like) #(su2/contains? (get-prop % :user) user-like))
        (if (! = "" title-like) #(su2/contains? (get-prop % :title) title-like))
        (if (! = "" author-like) #(su2/contains? (get-prop % :author) author-like))
        (if (! = "" date-like) #(su2/contains? (get-prop % :date) date-like))
        )
      res
      )
    )
  )

(defnk get-book-image [title author :size "medium"]
  (cache-fn
    (url-encode (str title size))
    (fn []
      (let [req (make-requester "ecs.amazonaws.jp" keys/*aws-access-key* keys/*aws-secret-key*)
            res (z/xml-zip (item-search-map req "Books" title {"Author" author, "ResponseGroup" "Images"}))
            target-size (case size
                          "small" :SmallImage
                          "medium" :MediumImage
                          "large" :LargeImage
                          :else :MediumImage
                          )
            ]
        (zfx/xml1-> res zf/children :Items :Item target-size :URL zfx/text)
        )
      )
    :expiration 86400
    )
  )

; global version of find-books
(defn get-books [& args] (map entity->book (apply find-books args)))

(defn save-book [tweet]
  (let [name (:from-user tweet), title (:title tweet)
        author (:author tweet), status (:status tweet)
        icon (:profile-image-url tweet), date (now)
        ]
    (println "status = " status)
    ; 再読がありえるから fin は同じのがあっても登録/更新
    ; wntの場合でingに既に同じものが入っているのはおかしいからNG
    (when (and (or (= status "finish") (zero? (count (find-books :user name :title title :author author :status status))))
               (or (! = status "want") (zero? (count (find-books :user name :title title :author author :status "reading")))))
      (let [books (group #(get-prop % :status) (find-books :user name))
            update-target (case status
                            ; reading <= want or have
                            "reading" (concat (:want books) (:have books) (:delete books))
                            "want" (:delete books)
                            ; fin <= ing, wnt or has
                            "finish" (concat (:reading books) (:wwnt books) (:have books) (:delete books))
                            ; has <= wnt
                            "have" (concat (:reading books) (:want books) (:delete books))
                            "delete" (concat (:reading books) (:want books) (:finish books) (:have books))
                            )
            x (se/find-first #(and (= title (get-prop % :title))
                                   (if (and (! su2/blank? author) (! su2/blank? (get-prop % :author)))
                                     (= author (get-prop % :author))
                                     true
                                     )
                                   )
                             update-target)
            ]
        (println "x = " x)
        (cond
          ; 新規登録
          (nil? x) (when (! = status "delete")
                     (println "aaaaa")
                     (ds-put (map-entity *book-entity-name* :user name :title title
                                         :author author :date date :status status :icon icon))
                     (println "bbb")
                     (change-user-data name (keyword status) inc)
                     (println "ccc")
                     (save-history :user name :title title :author author :date date
                                   :before "new" :after status)
                     (println "ddd")
                     )
          ; 登録済みのものを更新
          :else (let [before-status (get-prop x :status)]
                  (change-user-data
                    name
                    (keyword before-status) dec
                    (keyword status) inc
                    )
                  (set-prop x :status status)
                  (set-prop x :date date)
                  ; 著者が登録されていなくて、今回入力されている場合は登録する
                  (when (and (! su2/blank? author) (su2/blank? (get-prop x :author)))
                    (set-prop x :author author))
                  (when (su2/blank? (get-prop x :icon))
                    (set-prop x :icon icon))
                  (ds-put x)

                  (save-history :user name :title title :author (get-prop x :author)
                                :date date :before before-status :after status)
                  )
          )
        )
      ;false
      true
      )
    )
  )

(defn delete-book
  ([id]
   (try-with-boolean
     (ds-delete (create-key *book-entity-name* (Long/parseLong id)))
     )
   )
  ([user title author]
   (let [res (apply find-books (p list :user user :title title :author author))]
     (if (empty? res) false
       (try-with-boolean (ds-delete (-> res first get-key)))
       )
     )
   )
  )

