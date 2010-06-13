(ns yuruyomi.view.book
  (:use simply layout)
  (:require [clojure.contrib.str-utils2 :as su2])
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

(defn shorten-title [title]
  (if (> (count title) *show-title-length*)
    (str (su2/take title *show-title-length*) "...")
    title
    )
  )

(defnk book->html [book :show-user? false :show-status? false :show-delete? false]
  (let [class-name (str "book " (:status book))]
    [:div {:class class-name}
     [:div {:class "book_image"}
      [:img {:src "/img/noimg.png" :width "110" :height "160" :id (:id book)}]
      ]
     [:p {:class "icon"} (get *status-text* (:status book))]
     [:p {:class "title"} (shorten-title (:title book))]
     ]
    )
  )

;dbef02ff

(defn mobile-book->html [book]
  [:p {:style "margin:0;padding:0;font-size:small"} [:span {:style (str "color: " (get *status-color* (:status book)))} "■"]
   (:title book)
   ]
  )


