(ns yuruyomi.view.book
  (:use simply layout)
  (:require [clojure.contrib.str-utils2 :as su2])
  )

(def flag->text
  {"ing" "読中" "fin" "読了" "wnt" "欲しい" "has" "所持"})

(defnk book->html [book :show-user? false :show-flag? false :show-delete? false]
  [:p
   (if (! su2/blank? (:icon book)) [:img {:src (:icon book)}])
   "title: " (:title book) " / author: " (:author book)
   (if show-user?
     (list " by " [:a {:href (str "/user/" (:user book))} (:user book)])
     )
   " (" (:date book)
   (if show-flag?
     (list ", " (get flag->text (:flag book)) ")")
     ")"
     )
   ; ↓削除は認証をいれてから
   (if show-delete?
     [:a {:href (str "/admin/del?id=" (:id book))} "del"]
     )
   ]
  )

