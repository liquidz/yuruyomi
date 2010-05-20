(ns yuruyomi.view.html
  (:use
     simply
     ;am.ik.clj-gae-ds.core
     ;[yuruyomi collect-twitter]
     [yuruyomi.util seq]
     ;[yuruyomi.model book setting]
     [yuruyomi.model book]
     [yuruyomi.view book]
     layout
     )
  )

(defn get-user-data-html [name]
  (let [ls (group :flag (get-user-books name))
        ]
    (concat
      (list [:h2 (get flag->text "ing")])
      (map book->html (:ing ls))
      (list [:h2 (get flag->text "wnt")])
      (map book->html (:wnt ls))
      (list [:h2 (get flag->text "fin")])
      (map book->html (:fin ls))
      (list [:h2 (get flag->text "has")])
      (map book->html (:has ls))
      )
    )
  )

(defn show-user-html [name]
  (layout
    (str "yuruyomi alpha - " name)
    [:h1 (str name "'s books")]
    (get-user-data-html name)
    )
  )

(defn not-found-page []
  (layout
    "page not found"
    [:h1 "page not found"]
    )
  )

(defn index-page []
  (layout
    "yuruyomi alpha"
    (map #(book->html % :show-user? true :show-flag? true) (get-all-books))
    )
  )


