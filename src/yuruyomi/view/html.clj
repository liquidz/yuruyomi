(ns yuruyomi.view.html
  (:use
     simply
     ;am.ik.clj-gae-ds.core
     ;[yuruyomi collect-twitter]
     [yuruyomi.util seq]
     ;[yuruyomi.model book setting]
     [yuruyomi.model book history]
     [yuruyomi.view book]
     layout
     )
  (:require [clojure.contrib.seq-utils :as se])
  )

(def *page-title* "yuruyomi alpha")

(defn ajax-get-book-image [id]
  (let [b (se/find-first #(= id (str (:id %))) (get-all-books))]
    (p get-book-image (:title b) (:author b))
    )
  )

(defn get-user-data-html [name]
  (let [ls (group :status (get-user-books name))
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

(defn show-user-html [name]
  (layout
    (str *page-title* " - " name)
    [:h1 (str name "'s books")]
    (get-user-data-html name)
    )
  )

(defn not-found-page []
  (layout
    (str *page-title* " - page not found")
    [:h1 "page not found"]
    )
  )

(defn index-page []
  (layout
    *page-title*
    :js (list "/js/jquery.js" "/js/main.js")
    (map #(book->html % :show-user? true :show-status? true) (get-all-books))
    )
  )


