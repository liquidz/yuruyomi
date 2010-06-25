; ns {{{
(ns yuruyomi.model.book
  (:use
     [simply :only [defnk case fold ! url-encode group try-with-boolean]]
     [simply.date :only [set-default-timezone now]]
     [am.ik.clj-gae-ds.core :only [get-prop set-prop ds-put get-id get-key
                                   ds-get create-key map-entity ds-delete]]
     [am.ik.clj-aws-ecs :only [make-requester item-search-map]]
     [yuruyomi.clj-gae-ds-wrapper :only [find-entity count-entity entity->map get-entity delete-entity]]
     [yuruyomi.util.seq :only [filters]]
     [yuruyomi.util.cache :only [get-cached-value cache-val]]
     [yuruyomi.model.history :only [save-history]]
     [yuruyomi.model.user :only [change-user-data]]
     )
  (:require
     keys
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [clojure.zip :as z]
     [clojure.contrib.zip-filter :as zf]
     [clojure.contrib.zip-filter.xml :as zfx]
     [clojure.contrib.logging :as log]
     )
  ); }}}

(def *book-entity-name* "book")

;(defn- entity->book [e]
;  (-> e entity->map (assoc :id (-> e get-key get-id)))
;  )

; =make-book-cache-key
(defn- make-book-cache-key [title author size]
  (url-encode (str title author size))
  )

; =get-book-image-cache
(defnk get-book-image-cache [title author :size "medium" :default ""]
  (get-cached-value (make-book-cache-key title author size) :default default)
  )

; =get-book-image
(defnk get-book-image [title author :size "medium" :default ""]
  (let [key (make-book-cache-key title author size)
        val (get-cached-value key :default nil)
        base-arg {"ResponseGroup" "Images"}
        search-arg (if (su2/blank? author) base-arg (assoc base-arg "Author" author))
        ]
    (if (! nil? val)
      val
      (try
        (let [req (make-requester "ecs.amazonaws.jp" keys/*aws-access-key* keys/*aws-secret-key*)
              res (z/xml-zip (item-search-map req "Books" title search-arg))
              target-size (case size "small" :SmallImage "medium" :MediumImage
                            "large" :LargeImage :else :MediumImage)
              url (zfx/xml1-> res zf/children :Items :Item target-size :URL zfx/text)
              ]
          (cache-val key url :default default :expiration 86400)
          )
        (catch Exception e
          (log/warn (str "get-book-image error: " (.getMessage e)))
          default)
        )
      )
    )
  )

; =find-books
(defn- find-books [& args]
  (let [m (apply array-map args)
        user-like (:user-like m) title-like (:title-like m)
        author-like (:author-like m) date-like (:date-like m)
        find-args (fold concat () (dissoc m :user-like :title-like :author-like :date-like))
        res (apply find-entity (cons *book-entity-name* find-args))
        ]

    (if (some #(! nil? %) [user-like title-like author-like date-like])
      (filters
        res
        (when (! nil? user-like) #(su2/contains? (get-prop % :user) user-like))
        (when (! nil? title-like) #(su2/contains? (get-prop % :title) title-like))
        (when (! nil? author-like) #(su2/contains? (get-prop % :author) author-like))
        (when (! nil? date-like) #(su2/contains? (get-prop % :date) date-like))
        )
      res
      )
    )
  )


; global version of find-books
;(defn get-books [& args] (map entity->book (apply find-books args)))
(defn get-books [& args] (map entity->map (apply find-books args)))

(defn get-a-book [id]
  ;(->> id (get-entity *book-entity-name*) entity->book)
  (->> id (get-entity *book-entity-name*) entity->map)
  )

(defn count-books [& args]
  (apply count-entity (cons *book-entity-name* args))
  )

(defn history->book [h]
  (first (get-books :user (:user h) :title (:title h)
                    :author (:author h) :limit 1 :offset 0))
  )

; =save-new-book
(defnk save-new-book [:user "" :title "" :author "" :date (now) :status "" :icon "" :text ""]
  (when (and (! su2/blank? user) (! su2/blank? title)(! su2/blank? status) (! = status "delete"))
    (let [e (map-entity *book-entity-name* :user name :title title
                        :author author :date date :status status :icon icon)]
      (ds-put e)
      (change-user-data name (keyword status) inc)
      (save-history :user name :title title :author author :date date
                    :before "new" :after status :text text
                    :book-id (-> e get-key get-id))
      ;(bot-tweet (str "new book " title " added."))
      )
    )
  )

; =change-book-status
(defnk change-book-status [book-entity new-status :author "" :icon "" :text ""]
   (let [before-status (get-prop book-entity :status)
         [name title date] (get-props book-entity :user :title :date)]

     (change-user-data name (keyword before-status) dec (keyword new-status) inc)

     (set-prop book-entity :status status)
     (set-prop book-entity :date date)
     ; 著者が登録されていなくて、今回入力されている場合は登録する
     (when (and (! su2/blank? author) (su2/blank? (get-prop book-entity :author)))
       (set-prop book-entity :author author))
     ; アイコンが登録されていなくて、今回入力されている場合は登録する
     (when (su2/blank? (get-prop book-entity :icon))
       (set-prop book-entity :icon icon))
     (ds-put book-entity)

     (save-history :user name :title title :author (get-prop book-entity :author)
                   :date date :before before-status :after new-status
                   :text text :book-id (-> book-entity get-key get-id))
     )
  )

; =save-book
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
          (nil? x) (save-new-book :user user :title title :author author :date date
                                  :status status :icon icon :text (:original_text tweet))
;          (when (! = status "delete")
;                     (let [e (map-entity *book-entity-name* :user name :title title
;                                         :author author :date date :status status :icon icon)]
;                       (ds-put e)
;                       (change-user-data name (keyword status) inc)
;                       (save-history :user name :title title :author author :date date
;                                     :before "new" :after status :text (:original_text tweet)
;                                     :book-id (-> e get-key get-id))
;                       )
;                     )
          ; 登録済みのものを更新
          :else (change-book-status x status :author author :icon icon :text (:original_text tweet))
;          (let [before-status (get-prop x :status)]
;                  (change-user-data
;                    name
;                    (keyword before-status) dec
;                    (keyword status) inc
;                    )
;                  (set-prop x :status status)
;                  (set-prop x :date date)
;                  ; 著者が登録されていなくて、今回入力されている場合は登録する
;                  (when (and (! su2/blank? author) (su2/blank? (get-prop x :author)))
;                    (set-prop x :author author))
;                  (when (su2/blank? (get-prop x :icon))
;                    (set-prop x :icon icon))
;                  (ds-put x)
;
;                  (save-history :user name :title title :author (get-prop x :author)
;                                :date date :before before-status :after status
;                                :text (:original_text tweet) :book-id (-> x get-key get-id))
;                  )
          )
        )
      ;false
      true
      )
    )
  )

(defn delete-book [id]
  (try-with-boolean
    (delete-entity *book-entity-name* id)
    )
  )

