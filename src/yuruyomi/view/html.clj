(ns yuruyomi.view.html
  (:use
     simply
     [yuruyomi.util seq]
     [yuruyomi.model book history user]
     [yuruyomi.view book]
     layout
     )
  (:require [clojure.contrib.seq-utils :as se])
  )

(def *page-title* "yuruyomi alpha")

(defn ajax-get-book-image [id]
  (let [b (se/find-first #(= id (str (:id %))) (get-books))]
    (p get-book-image (:title b) (:author b))
    )
  )

(defn get-user-data-html [name]
  (let [ls (group :status (get-books :user name))
        ]
    (concat
      (list [:h2 (get status->text "ing")])
      (map book->html (:ing ls))
      (list [:h2 (get status->text "wnt")])
      (map book->html (:wnt ls))
      (list [:h2 (get status->text "fin")])
      (map book->html (:fin ls))
      (list [:h2 (get status->text "has")])
      (map book->html (:has ls))
      )
    )
  )

(defn show-history-html [name]
  (layout
    (str *page-title* " - " name)
    [:h1 (str name "'s history")]
    (map (fn [h]
           [:p (:title h) " - " (:author h) ", " (:before h) "=>" (:after h) " (" (:date h) ")"]
           )
         (find-history :name name)
         )
    )
  )

(defn show-search-html [text mode]
  (layout
    *page-title*

    (map (fn [b]
           [:p (:title b) " : " (:author b) " (" (:user b) ")"]
           )
         (let [res (apply get-books
                (list
                  (keyword (case mode
                             ["title" "author" "user"] (str mode "-like")
                             :else "title-like"
                             )
                           )
                  text
                  )
                )]
           res
           )
         )
    )
  )

(defn show-user-html [name]
  (layout
    (str *page-title* " - " name)
    [:h1 (str name "'s books")]
    (let [x (get-user :user name)]
      (when (! empty? x)
        (list
          [:p "ing = " (:ing (first x))]
          [:p "wnt = " (:wnt (first x))]
          [:p "has = " (:has (first x))]
          [:p "fin = " (:fin (first x))]
          )
        )
      )
    [:hr]
    (get-user-data-html name)
    )
  )

(defn not-found-page []
  (layout
    (str *page-title* " - page not found")
    [:h1 "page not found"]
    )
  )

(defn search-form []
  [:form {:method "GET" :action "/search"}
   [:select {:name "mode"}
    [:option {:value "title"} "title"]
    [:option {:value "author"} "author"]
    [:option {:value "user"} "user"]
    ]
   [:input {:type "text" :name "keyword"}]
   [:input {:type "submit" :value "search"}]
   ]
  )

(defn index-page []
  (layout
    *page-title*
    :js (list "/js/jquery.js" "/js/main.js")
    (search-form)
    (map #(book->html % :show-user? true :show-status? true) (get-books))
    )
  )


