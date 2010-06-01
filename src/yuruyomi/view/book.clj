(ns yuruyomi.view.book
  (:use simply layout)
  (:require [clojure.contrib.str-utils2 :as su2])
  )

(def *status-text*
  {"reading" "読んでる" "finish" "読み終わった" "want" "欲しい" "have" "持っている" "delete" "削除"}
  )


(defnk book->html [book :show-user? false :show-status? false :show-delete? false]
  (let [class-name (str "book " (:status book))]
    [:div {:id (:id book) :class class-name}
     [:img {:src "" }]
     [:p {:class "icon"} (get *status-text* (:status book))]
     [:p {:class "title"} (:title book)]
     ]
    )
  )

