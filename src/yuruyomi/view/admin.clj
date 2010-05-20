(ns yuruyomi.view.admin
  (:use
     simply
     [yuruyomi.util seq]
     [yuruyomi.model book setting]
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

(defn admin-index-page []
  (layout
    "yuruyomi admin"
    [:h1 "admin"]
    [:p "max id = " (get-max-id)]
    [:hr]
    (map #(book->html % :show-user? true :show-flag? true :show-delete? true) (get-all-books))
    [:hr]
    (show-test-form)
    [:hr]
    [:p [:a {:href "/admin/cron/collect"} "collect twitter data"]]
    [:p [:a {:href "/admin/clear"} "clear max id"]]
    )
  )


