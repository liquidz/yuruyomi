(ns yuruyomi.view.admin
  (:use
     simply
     [yuruyomi.util seq]
     [yuruyomi.model book history setting]
     [yuruyomi.view book]
     layout
     )
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

(defn admin-index-page []
  (layout
    "yuruyomi admin"
    [:h1 "admin"]
    [:p "max id = " (get-max-id)]
    [:hr]
    (map #(book->html % :show-user? true :show-status? true :show-delete? true) (get-books))
    [:hr]
    (show-test-form)
    [:hr]
    [:p [:a {:href "/admin/cron/collect"} "collect twitter data"]]
    [:p [:a {:href "/admin/clear"} "clear max id"]]
    )
  )


