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

(defn admin-menu []
  [:ul
   [:li "max id = " (get-max-id)]
   [:li [:a {:href "/admin/cron/twitter"} "collect twitter data"]]
   [:li [:a {:href "/admin/cron/user"} "collect user data"]]
   ;[:li [:a {:href "/admin/clear"} "clear max id"]]
   ]
  )

(defn admin-index-page [page]
  (let [pp (if (nil? page) 1 (i page))
        bc (count-books)
        pc (.intValue (Math/ceil (/ bc 2)))
        pages (take pc (iterate inc 1))
        ]
    (layout
      "yuruyomi admin"
      :css ["/css/admin.css"]
      (admin-menu)
      [:hr]
      [:p "count = " (count-books)]
      (map #(admin-book->html % :show-user? true :show-status? true :show-delete? true)
           (get-books :limit 2 :page pp))
           ;(get-books :limit 2 :offset (* 2 (dec pp))))
      [:hr]
      (map (fn [x]
             [:a {:href (str "/admin/?page=" x)} x]
             ) pages)

      [:hr]
      admin-test-form
      )
    )
  )


