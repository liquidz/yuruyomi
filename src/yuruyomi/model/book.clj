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

(defn- find-books [& args]
  (let [m (apply array-map args)
        user-like (:user-like m) title-like (:title-like m)
        author-like (:author-like m) date-like (:date-like m)
        find-args (fold concat () (dissoc m :user-like :title-like :author-like :date-like))
        res (apply find-entity (cons *book-entity-name* find-args))
        ;res (apply find-entity (cons *book-entity-name* (fold concat () (dissoc m :user-like :title-like :author-like :date-like))))
        ]

    (if (some #(! nil? %) [user-like title-like author-like date-like])
      (filters
        res
        (when (! nil? user-like) #(su2/contains? (get-prop % :user) (to-utf8 user-like)))
        (when (! nil? title-like) #(su2/contains? (get-prop % :title) (to-utf8 title-like)))
        (when (! nil? author-like) #(su2/contains? (get-prop % :author) (to-utf8 author-like)))
        (when (! nil? date-like) #(su2/contains? (get-prop % :date) date-like))
        )
      res
      )
    )
  )

(defn make-book-cache-key [title author size]
  (url-encode (str title author size))
  )

(defnk get-book-image-cache [title author :size "medium" :default ""]
  (get-cached-value (make-book-cache-key title author size) :default default)
  )

(defnk get-book-image [title author :size "medium" :default ""]
  (let [key (make-book-cache-key title author size)
        val (get-cached-value key :default nil)]
    (if (! nil? val)
      val
      (let [req (make-requester "ecs.amazonaws.jp" keys/*aws-access-key* keys/*aws-secret-key*)
            res (z/xml-zip (item-search-map req "Books" title {"Author" author, "ResponseGroup" "Images"}))
            target-size (case size "small" :SmallImage "medium" :MediumImage
                          "large" :LargeImage :else :MediumImage)
            url (zfx/xml1-> res zf/children :Items :Item target-size :URL zfx/text)
            ]
        (cache-val key url :default default :expiration 86400)
        )
      )
    )
  )

; global version of find-books
(defn get-books [& args] (map entity->book (apply find-books args)))

(defn count-books [& args]
  (apply count-entity (cons *book-entity-name* args))
  )

(defn save-book [tweet]
  (set-default-timezone)
  (let [name (:from-user tweet), title (:title tweet)
        author (:author tweet), status (:status tweet)
        icon (:profile-image-url tweet), date (now)
        ]
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
        (cond
          ; 新規登録
          (nil? x) (when (! = status "delete")
                     (ds-put (map-entity *book-entity-name* :user name :title title
                                         :author author :date date :status status :icon icon))
                     (change-user-data name (keyword status) inc)
                     (save-history :user name :title title :author author :date date
                                   :before "new" :after status :text (:original_text tweet))
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
                                :date date :before before-status :after status :text (:original_text tweet))
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

