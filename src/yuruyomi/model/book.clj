; ns {{{
(ns yuruyomi.model.book
  (:use
     [simply core date string]
     [am.ik.clj-gae-ds.core :only [get-prop set-prop ds-put get-id get-key
                                   ds-get create-key map-entity ds-delete]]
     [am.ik.clj-aws-ecs :only [make-requester item-search-map]]
     [yuruyomi.clj-gae-ds-wrapper :only [find-entity count-entity entity->map get-entity delete-entity get-props]]
     [yuruyomi.util.seq :only [filters]]
     [yuruyomi.util.cache :only [get-cached-value cache-val]]
     [yuruyomi.model.history :only [save-history]]
     [yuruyomi.model.user :only [change-user-data]]
     )
  (:require
     keys
     [clojure.contrib.seq :as se]
     [clojure.contrib.string :as st]
     [clojure.zip :as z]
     [clojure.contrib.zip-filter :as zf]
     [clojure.contrib.zip-filter.xml :as zfx]
     [clojure.contrib.logging :as log]
     )
  ); }}}

(def *book-entity-name* "book")

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
        search-arg (if (st/blank? author) base-arg (assoc base-arg "Author" author))
        ]
    (if-not (nil? val)
      (if (st/blank? val) default val)
      (try
        (let [req (make-requester "ecs.amazonaws.jp" keys/*aws-access-key* keys/*aws-secret-key*)
              res (z/xml-zip (item-search-map req "Books" title search-arg))
              target-size (case size "small" :SmallImage "medium" :MediumImage
                            "large" :LargeImage :MediumImage)
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
        (when-not (nil? user-like) #(st/substring? user-like (get-prop % :user)))
        (when-not (nil? title-like) #(st/substring? title-like (get-prop % :title)))
        (when-not (nil? author-like) #(st/substring? author-like (get-prop % :author)))
        (when-not (nil? date-like) #(st/substring? date-like (get-prop % :date)))
        )
      res
      )
    )
  )


; global version of find-books
(defn get-books [& args] (map entity->map (apply find-books args)))

(defn get-a-book [id]
  (let [e (get-entity *book-entity-name* id)]
    (if (nil? e) nil (entity->map e))
    )
  ;(->> id (get-entity *book-entity-name*) entity->map)
  )

(defn count-books [& args]
  (apply count-entity (cons *book-entity-name* args))
  )

; =save-new-book
(defnk save-new-book [:user "" :id -1 :title "" :author "" :date (now) :status "" :icon "" :text ""]
  (let [book (if (pos? id) (get-a-book id))
        book-title (if (nil? book) title (:title book))
        book-author (if (nil? book) author (if (st/blank? (:author book)) author (:author book)))
        ]
    (when (and (! st/blank? user) (! st/blank? book-title) (! st/blank? status) (! = status "delete"))
      (let [e (map-entity *book-entity-name* :user user :title book-title
                          :author book-author :date date :status status :icon icon)]
        (ds-put e)
        (change-user-data user (keyword status) inc)
        (save-history :user user :title book-title :author book-author :date date
                      :before "new" :after status :text text
                      :book-id (-> e get-key get-id))
        ;(bot-tweet (str "new book " title " added."))
        )
      )
    )
  )

; =change-book-status
(defnk change-book-status [id-or-entity new-status :author "" :date "" :icon "" :text ""]
  (set-default-timezone)
  (let [book-entity (if (string? id-or-entity) (get-entity *book-entity-name* id-or-entity) id-or-entity)
        before-status (get-prop book-entity :status)
        [name title] (get-props book-entity :user :title)
        date2 (if (st/blank? date) (now) date)]

    (change-user-data name (keyword before-status) dec (keyword new-status) inc)

    (set-prop book-entity :status new-status)
    (set-prop book-entity :date date2)

    ; 著者が登録されていなくて、今回入力されている場合は登録する
    (when (and (! st/blank? author) (st/blank? (get-prop book-entity :author)))
      (set-prop book-entity :author author))
    ; アイコンが登録されていなくて、今回入力されている場合は登録する
    (when (st/blank? (get-prop book-entity :icon))
      (set-prop book-entity :icon icon))
    (ds-put book-entity)

    (save-history :user name :title title :author (get-prop book-entity :author)
                  :date date2 :before before-status :after new-status
                  :text text :book-id (-> book-entity get-key get-id))
    )
  )

; =save-book-from-tweet
(defn save-book-from-tweet [tweet]
  (set-default-timezone)
  (let [name (:from-user tweet), title (:title tweet)
        author (:author tweet), status (:status tweet)
        icon (:profile-image-url tweet), date (now)
        ]
    ; 再読がありえるから fin は同じのがあっても登録/更新
    ; wntの場合でingに既に同じものが入っているのはおかしいからNG
    (when (and (or (= status "finish") (zero? (count (find-books :user name :title title :author author :status status))))
               (or (! = status "want") (zero? (count (find-books :user name :title title :author author :status "reading")))))
      (let [books (group-by #(get-prop % :status) (find-books :user name))
            update-target (case status
                            ; reading <= want or have
                            "reading" (concat (get books "want") (get books "have") (get books "delete"))
                            "want" (get books "delete")
                            ; fin <= ing, wnt or has
                            "finish" (concat (get books "reading") (get books "wwnt") (get books "have") (get books "delete"))
                            ; has <= wnt
                            "have" (concat (get books "reading") (get books "want") (get books "delete"))
                            "delete" (concat (get books "reading") (get books "want") (get books "finish") (get books "have"))
                            )
            x (se/find-first #(and (= title (get-prop % :title))
                                   (if (and (! st/blank? author) (! st/blank? (get-prop % :author)))
                                     (= author (get-prop % :author))
                                     true
                                     )
                                   )
                             update-target)
            ]
        (if (nil? x)
          ; 新規登録
          (save-new-book :user name :title title :author author :date date
                         :status status :icon icon :text (:original_text tweet))
          ; 登録済みのものを更新
          (change-book-status x status :author author :date date :icon icon
                              :text (:original_text tweet))
          )
        )
      ;false
      true
      )
    )
  )

(defn delete-book [id]
  (try
    (delete-entity *book-entity-name* id)
    true
    (catch Exception e false)
    )
  )

