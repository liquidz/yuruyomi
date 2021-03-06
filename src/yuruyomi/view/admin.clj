(ns yuruyomi.view.admin
  (:use
     simply.core
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
  (:require [clojure.contrib.string :as st])
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
   [:li [:a {:href "/admin/history"} "history"]]
   ;[:li "max id = " (get-max-id)]
   [:li [:a {:href "/admin/cron/twitter" :class "warn"} "collect twitter data"]]
   [:li [:a {:href "/admin/cron/user"} "collect user data"]]
   [:li [:a {:href "/admin/clear" :class "warn"} "clear max id"]]
   [:li [:a {:href "/admin/search_twitter" :class ""} "search twitter test"]]
   [:li [:a {:href "/admin/set_book_id" :class "warn"} "set book id to history"]]
   ]
  )

(defn admin-history-page [page]
  (let [pp (if (st/blank? page) 1 (Integer/parseInt page))]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      (admin-menu)

      (table
        (map #(list (:user %) (:book-id %) (:title %) (:author %) (:date %)
                    (str (:before %) " => " (:after %))
                    )
             (find-history :sort "date"))
             ;(find-history :limit 10 :offset (* 10 (dec pp))))
        :header ["user" "book-id" "title" "author" "date" "status"]
        )

      ;(map (fn [h] [:p (:user h) "-" (:title h) ":" (:author h) " (" (:date h) ") " (:before h) " => " (:after h)])
      ;     (find-history :limit 5 :offset (* 5 (dec pp))))
      )
    )
  )

(defn admin-search-twitter-page [m]
  (let [mode (case m "all" :all :since)
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
  (foreach #(when (nil? (:book-id %))
              (let [his (get-entity *history-entity-name* (:id %))]
                (set-prop his :book-id (:book-id %))
                (ds-put his)
                )
              ) (find-history))
  )

(defn admin-index-page [page]
  (let [pp (if (st/blank? page) 1 (Integer/parseInt page))
        bc (count-books)
        pc (.intValue (Math/ceil (/ bc 20)))
        pages (take pc (iterate inc 1))
        ]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      :js ["/js/jquery-1.4.2.min.js" "/js/admin.js"]
      (admin-menu)
      [:hr]
      [:p "max id = " (get-max-id)]
      [:p "count = " (count-books)]

      (table
        (map #(list [:a {:href (str "/book/" (:id %))} (:id %)]
                    (:title %) (:author %)
                    [:a {:href (str "/user/" (:user %))} (:user %)] (:date %) (:status %)
                    [:a {:href (str "/admin/del?id=" (:id %)) :class "warn"} "del"])
             (get-books :sort "date" :limit 20 :page pp))
        :header ["ID" "title" "author" "user" "date" "status" "delete"]
        )

      [:hr]
      (map (fn [x]
             [:span "&nbsp;" [:a {:href (str "/admin/?page=" x)} x] "&nbsp;"]
             ) pages)

      [:hr]
      admin-test-form
      )
    )
  )


