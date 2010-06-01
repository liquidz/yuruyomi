(ns yuruyomi.view.admin
  (:use
     simply
     [yuruyomi.util seq]
     [yuruyomi.model book history setting]
     [yuruyomi.view book]
     layout
     )
  (:require [clojure.contrib.str-utils2 :as su2])
  )

(defn show-test-form []
  [:form {:method "GET" :action "/admin/test"}
   [:p "text: " [:input {:type "text" :name "text"}]
    [:input {:type "submit" :value "test"}]
    ]
   ]
  )

(defn admin-history-page [page]
  (let [pp (if (nil? page) 1 (i page))]
    (layout
      "yuruyomi admin"
      (map (fn [h] [:p (:user h) "-" (:title h) ":" (:author h) " (" (:date h) ") " (:before h) " => " (:after h)])
           (find-history :limit 5 :offset (* 5 (dec pp))))
      )
    )
  )

(defnk admin-book->html [book :show-user? false :show-status? false :show-delete? false]
  [:p
   (if (! su2/blank? (:icon book)) [:img {:src (:icon book)}])
   "title: " (:title book) " / author: " (:author book)
   (if show-user?
     (list " by " [:a {:href (str "/user/" (:user book))} (:user book)])
     )
   " (" (:date book)
   (if show-status?
     (list ", " (:status book) ")")
     ")"
     )
   ; 削除は認証をいれてから
   (if show-delete?
     [:a {:href (str "/admin/del?id=" (:id book))} "del"]
     )
   [:div {:id (str "box" (:id book))}]
   [:a {:href (str "javascript:getImage(" (:id book) ");")} "get-image"]
   ]
  )

(defn admin-index-page []
  (layout
    "yuruyomi admin"
    [:h1 "admin"]
    [:p "max id = " (get-max-id)]
    [:hr]
    (map #(admin-book->html % :show-user? true :show-status? true :show-delete? true) (get-books))
    [:hr]
    (show-test-form)
    [:hr]
    [:p [:a {:href "/admin/cron/twitter"} "collect twitter data"]]
    [:p [:a {:href "/admin/cron/user"} "collect user data"]]
    [:p [:a {:href "/admin/clear"} "clear max id"]]
    )
  )


