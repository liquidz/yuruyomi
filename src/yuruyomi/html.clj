(ns yuruyomi.html
  (:use
     simply
     [hiccup.core :only [html]]
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper book seq collect-twitter]
     )
  )

(defnk layout [:title "non titled"
               :head ()
               :body "body"]
  (html
    [:html
     [:head [:title title] head ]
     [:body body]
     ]
    )
  )

(defn js [& srcs]
  (map (fn [s] [:script {:type "text/javascript" :src s}]) srcs)
  )
(defn rss [href] [:link {:rel "style-sheet" :type "text/css" :href href}])

(defn save-form-html [name]
  [:form {:method "GET" :action "/save"}
   [:input {:type "hidden" :name "name" :value name}]
   [:p "title " [:input {:type "text" :name "title"}]]
   [:p "author" [:input {:type "text" :name "author"}]]
   [:select {:name "flag"}
    [:option {:value "ing"} "ing"]
    [:option {:value "fin"} "fin"]
    [:option {:value "wnt"} "wnt"]
    [:option {:value "has"} "has"]
    ]
   [:input {:type "submit" :value "save"}]
   ]
  )

(defn book->html [e]
  (let [[title author date user flag] (get-props e :title :author :date :user :flag)
        ]
    [:p title "/" author " (" date ", " user ", " flag ")"
     ; ↓削除は認証をいれてから
     [:a {:href (str "/del?id=" (-> e get-key get-id))} "del"]
     ]
    )
  )

(defn get-user-data-html [name]
  (let [ls (group #(get-prop % :flag) (get-user-books name))
        ]
    (concat
      (list [:h4 "ing"])
      (map book->html (:ing ls))
      (list [:h4 "wnt"])
      (map book->html (:wnt ls))
      (list [:h4 "fin"])
      (map book->html (:fin ls))
      (list [:h4 "has"])
      (map book->html (:has ls))
      )
    )
  )

(defn show-user-html [req]
  (let [[name] (params req "name")]
    (layout
      :title "show-user"
      :body (list [:h3 name]
                  (get-user-data-html name)
                  [:hr]
                  (save-form-html name)
                  )
      )
    )
  )

(defn show-html []
  (layout
    :title "top"
    :body (list
            [:p "max id = " (get-yuruyomi-max-id)]
            [:hr]
            (map book->html (find-books))
            [:hr]
            [:p [:a {:href "/collect"} "collect twitter data"]]
            [:p [:a {:href "/clear"} "clear max id"]]
            )
    )
  )


