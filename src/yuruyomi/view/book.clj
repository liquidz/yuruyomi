(ns yuruyomi.view.book
  (:use
     simply.core
     layout
     [yuruyomi.model book])
  (:require [clojure.contrib.string :as st])
  )

(def *status-text*
  {"reading" "読んでる" "finish" "読み終わった"
   "want" "欲しい" "have" "持っている" "delete" "削除" "new" "新規登録"}
  )
(def *status-color*
  {"reading" "#93d31b"
   "finish" "#a1723a"
   "want" "#e13b75"
   "have" "#479ece"
   }
  )
(def *show-title-length* 8)
(def *loading-image* "/img/loading.png")
(def *no-book-image* "/img/noimg.png")

(defn shorten-title [title]
  (if (> (count title) *show-title-length*)
    (str (st/take title *show-title-length*) "...")
    title
    )
  )

(defnk book->html [book :show-user? false :show-status? false :show-delete? false]
  (let [class-name (str "book " (:status book))
        image-url (get-book-image-cache (:title book) (:author book) :default *loading-image*)
        no-image? (= image-url *loading-image*)
        book-link (fn [t] [:a {:href (str "/book/" (:id book))} t])
        ]
    [:div {:class class-name}
     [:div {:class "book_image"}
      (book-link
        [:img {:src image-url :width "110" :height "160" :id (:id book) :class (if no-image? "load" "")}]
        )
      ]
     [:p {:class "icon"} (get *status-text* (:status book))]
     [:div {:class "book_info"}
      [:p {:class "title"} (book-link (:title book))]
      [:p {:class "author"} (book-link (:author book))]
      [:p {:class "date"} (book-link (first (st/split #"\s+" (:date book))))]
      ]
     ]
    )
  )

(defn mobile-book->html [book]
  [:p {:style "margin:0;padding:0;font-size:small"} [:span {:style (str "color: " (get *status-color* (:status book)))} "■"]
   [:a {:href (str "/mb/" (:id book))} (:title book)]
   ]
  )


