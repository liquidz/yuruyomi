(ns yuruyomi.view.admin
  (:use
     simply
     twitter
     [yuruyomi.util seq]
     [yuruyomi.model book history setting]
     [yuruyomi.view book]
     [yuruyomi.cron twitter]
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
   [:li [:a {:href "/admin/cron/twitter"} "collect twitter data"]]
   [:li [:a {:href "/admin/cron/user"} "collect user data"]]
   ;[:li [:a {:href "/admin/clear"} "clear max id"]]
   [:li [:a {:href "/admin/search_twitter"} "search twitter test"]]
   ]
  )

(defn admin-history-page [page]
  (let [pp (if (nil? page) 1 (i page))]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      (admin-menu)
      (map (fn [h] [:p (:user h) "-" (:title h) ":" (:author h) " (" (:date h) ") " (:before h) " => " (:after h)])
           (find-history :limit 5 :offset (* 5 (dec pp))))
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

(defn admin-book->html [book]
  [:tr
   [:td (:title book)]
   [:td (:author book)]
   [:td [:a {:href (str "/user/" (:user book))} (:user book)]]
   [:td (:date book)]
   [:td (:status book)]
   [:td [:a {:href (str "/admin/del?id=" (:id book))} "del"]]
   ]
  )



(defn admin-index-page [page]
  (let [pp (if (su2/blank? page) 1 (i page))
        bc (count-books)
        pc (.intValue (Math/ceil (/ bc 10)))
        pages (take pc (iterate inc 1))
        ]
    (layout
      *admin-page-title*
      :css ["/css/admin.css"]
      :js ["/js/jquery-1.4.2.min.js" "/js/jquery.colorize-2.0.0.js" "/js/admin.js"]
      (admin-menu)
      [:hr]
      [:p "count = " (count-books)]

      [:table
       [:tr [:th "タイトル"] [:th "著者"] [:th "ユーザ"] [:th "日付"] [:th "ステータス"] [:th "削除"]]
       (map admin-book->html (get-books :limit 10 :page pp))
       ]

;      (map #(admin-book->html % :show-user? false :show-status? true :show-delete? true)
;           (get-books :limit 10 :page pp))
      [:hr]
      (map (fn [x]
             [:a {:href (str "/admin/?page=" x)} x]
             ) pages)

      [:hr]
      admin-test-form
      )
    )
  )


