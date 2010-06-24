(ns yuruyomi.view.admin
  (:use
     simply
     twitter
     [yuruyomi.util seq]
     [yuruyomi.model book history setting]
     [yuruyomi.view book]
     [yuruyomi.view.parts :only [table]]
     [yuruyomi.cron twitter]
     [yuruyomi.clj-gae-ds-wrapper :only [get-entity]]
     [yuruyomi.model.history :only [*history-entity-name*]]
     [am.ik.clj-gae-ds.core :only [set-prop ds-put]]
     layout
     )
  (:require [clojure.contrib.str-utils2 :as su2])
  )

(def *admin-page-title* "yuruyomi admin")

(def admin-test-form
  [:form {:method "GET" :action "/admin/test"}
   [:span "user: "]
   [:input {:type "text" :name "user"}]
   [:span "image: "]
   [:input {:type "text" :name "image"}]
   [:span "text: "]
   [:input {:type "text" :name "text"}]
   [:input {:type "submit" :value "test"}]
   ]
  )

(defn admin-menu []
  [:ul
   [:li [:a {:href "/admin/"} "top"]]
   [:li "max id = " (get-max-id)]
   [:li [:a {:href "/admin/cron/twitter" :class "warn"} "collect twitter data"]]
   [:li [:a {:href "/admin/cron/user"} "collect user data"]]
   ;[:li [:a {:href "/admin/clear"} "clear max id"]]
   [:li [:a {:href "/admin/search_twitter"} "search twitter test"]]
   [:li [:a {:href "/admin/set_book_id" :class "warn"} "set book id to history"]]
   ]
  )

(defn admin-history-page [page]
  (let [pp (if (su2/blank? page) 1 (i page))]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      (admin-menu)

      (table
        (map #(list (:user %) (:book-id %) (:title %) (:author %) (:date %)
                    (str (:before %) " => " (:after %))
                    )
             (find-history :limit 10 :offset (* 10 (dec pp))))
        :header ["user" "book-id" "title" "author" "date" "status"]
        )

      ;(map (fn [h] [:p (:user h) "-" (:title h) ":" (:author h) " (" (:date h) ") " (:before h) " => " (:after h)])
      ;     (find-history :limit 5 :offset (* 5 (dec pp))))
      )
    )
  )

(defn admin-search-twitter-page [m]
  (let [mode (case m "all" :all :else :since)
        last-id (get-max-id)
        args (concat (list *yuruyomi-tag*)
                     (if (and (pos? last-id) (= mode :since)) (list :since-id last-id) ()))
        res (try (apply twitter-search-all args) (catch Exception _ nil))
        ]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      :js ["/js/jquery-1.4.2.min.js" "/js/jquery.colorize-2.0.0.js" "/js/admin.js"]
      (admin-menu)
      [:p [:a {:href "/admin/search_twitter?mode=all"} "show all"]]
      [:dl
       (map (fn [t]
              (list
                [:dt (:from-user t) " : id = " (:id t)
                 (when (= mode :all) (list " " [:a {:href (str "/admin/save?id=" (:id t))} "save"]))]
                [:dd (:text t)]
                )
              )
            (->> res :tweets (sort #(< (:id %1) (:id %2))))
            )
       ]
      )
    )
  )

;(defn admin-book->html [book]
;  [:tr
;   [:td (:title book)]
;   [:td (:author book)]
;   [:td [:a {:href (str "/user/" (:user book))} (:user book)]]
;   [:td (:date book)]
;   [:td (:status book)]
;   [:td [:a {:href (str "/admin/del?id=" (:id book))} "del"]]
;   ]
;  )

(defn admin-set-book-id []
  (let [histories (find-history)
        books (map history->book histories)
        res (partition 2 (interleave histories books))
        ]
    (foreach (fn [[h b]]
               (when (nil? (:book-id h))
                 (let [his (get-entity *history-entity-name* (:id h))]
                   (set-prop his :book-id (:id b))
                   (ds-put his)
                   )
                 )
               ) res)
    )
  )


(defn admin-index-page [page]
  (let [pp (if (su2/blank? page) 1 (i page))
        bc (count-books)
        pc (.intValue (Math/ceil (/ bc 20)))
        pages (take pc (iterate inc 1))
        ]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      ;:js ["/js/jquery-1.4.2.min.js" "/js/jquery.colorize-2.0.0.js" "/js/admin.js"]
      :js ["/js/jquery-1.4.2.min.js" "/js/admin.js"]
      (admin-menu)
      [:hr]
      [:p "count = " (count-books)]

      (table
        (map #(list [:a {:href (str "/book/" (:id %))} (:id %)]
                    (:title %) (:author %)
                    [:a {:href (str "/user/" (:user %))} (:user %)] (:date %) (:status %)
                    [:a {:href (str "/admin/del?id=" (:id %)) :class "warn"} "削除"])
             (get-books :limit 20 :page pp))
        :header ["ID" "タイトル" "著者" "ユーザ" "日付" "ステータス" "削除"]
        )

;      [:table
;       [:tr [:th "タイトル"] [:th "著者"] [:th "ユーザ"] [:th "日付"] [:th "ステータス"] [:th "削除"]]
;       (map admin-book->html (get-books :limit 10 :page pp))
;       ]

;      (map #(admin-book->html % :show-user? false :show-status? true :show-delete? true)
;           (get-books :limit 10 :page pp))
      [:hr]
      (map (fn [x]
             [:span "&nbsp;" [:a {:href (str "/admin/?page=" x)} x] "&nbsp;"]
             ) pages)

      [:hr]
      admin-test-form
      )
    )
  )


