(ns yuruyomi.view.mobile
  (:use
     simply
     layout
     [yuruyomi.view parts book]
     [yuruyomi.model book history]
     )
  )

(def *show-mobile-books-num* 10)
(def *show-mobile-history-num* 10)

(defn- mobile-menu [name]
  (list
    (mobile-border :text "メニュー" :height "1.5em" :css "text-align:center")
    ;[:p {:style "text-align:center;font-size:small;background:#dbef02"} "メニュー"]
    [:ul {:style "list-style:none;margin:0;padding:0;"}
     [:li [:span {:style "font-size:small;color:#93d31b"} "[1]"]
      [:a {:href (str "/m/" name "/reading") :style "font-size:small" :accesskey "1"} "読んでる本"]]
     [:li [:span {:style "font-size:small;color:#e13b75"} "[2]"]
      [:a {:href (str "/m/" name "/want") :style "font-size:small" :accesskey "2"} "欲しい本"]]
     [:li [:span {:style "font-size:small;color:#479ece"} "[3]"]
      [:a {:href (str "/m/" name "/have") :style "font-size:small" :accesskey "3"} "持ってる本"]]
     [:li [:span {:style "font-size:small;color:#a1723a"} "[7]"]
      [:a {:href (str "/m/" name "/finish") :style "font-size:small" :accesskey "7"} "読み終わった本"]]
     [:li [:span {:style "font-size:small;color:#aaa"} "[9]"]
      [:a {:href (str "/m/" name "/history") :style "font-size:small" :accesskey "9"} "履歴"]]
     [:li [:span {:style "font-size:small;color:#aaa"} "[0]"]
      [:a {:href (str "/m/" name) :style "font-size:small" :accesskey "0"} "トップ"]]
     ]
    )
  )

(defn mobile-no-book-box [status]
  (let [box (fn [label] [:h3 {:style "font-size:small"} label])]
    (case status
      "reading" (box "読んでる本はありません")
      "want" (box "欲しい本はありません")
      "have" (box "持ってる本はありません")
      "finish" (box "読み終わった本はありません")
      )
    )
  )

(defnk mobile-pager [name status now-page max-page :link (fn [x] (str "/" x))]
  (when (> max-page 1)
    (let [prev-label "&laquo;"
          next-label "&raquo;"]
      [:div {:class "pager"}
       (if (> now-page 1)
         [:a {:href (str "/m/" name "/" status (link (dec now-page))) :accesskey "4"} prev-label]
         [:span prev-label]
         )
       (map
         (fn [p]
           (if (= p now-page)
             [:span p]
             [:a {:href (str "/m/" name "/" status (link p))} p]
             )
           )
         (take max-page (iterate inc 1))
         )
        (if (< now-page max-page)
          [:a {:href (str "/m/" name "/" status (link (inc now-page))) :accesskey "6"} next-label]
          [:span next-label]
          )
       ]
      )
    )
  )

(defn mobile-index-page []
  (mobile-layout
    *page-title*
    [:h1 *page-title*]
    [:hr]
    [:p "ゆるよみはTwitterでつぶやくだけで読書管理ができるゆる～いサービスです。"]
    [:p "読んでる本、読みたい本が簡単に共有できます"]
    [:form {:method "GET" :action "/m/"}
     [:p "TwitterIDを入力" [:input {:type "text" :name "name"}]]
     [:input {:type "submit" :value "確認"}]
     ]
    mobile-footer
    )
  )

(defnk mobile-history-page [name :page 1]
  (let [now-page (if (pos? (i page)) (i page) 1)
        histories (find-history :user name :sort "date" :limit *show-mobile-history-num* :page now-page)
        pages (.intValue (Math/ceil (/ (count-histories :user name) *show-mobile-history-num*)))
        status "history"
        ]
    (mobile-layout
      *page-title*
      mobile-header
      (mobile-border :text (str name "さんの履歴") :height "1.5em" :css "font-size:small;text-align:center" :color "#eee")
      (if (empty? histories)
        [:h3 "まだ履歴はありません"]
        (map
          (fn [h]
            [:p (:title h) " " (get *status-text* (:before h)) "&raquo;" (get *status-text* (:after h)) [:br]
             (:date h)
             ]
            )
          histories)
        )
      (mobile-pager name status now-page pages)
      (mobile-menu name)
      mobile-footer
      )
    )
  )

(defn mobile-first-user-page []
  (mobile-layout
    *page-title*
    [:h2 "first"]
    )
  )

(defnk mobile-user-page [name :status "all" :page 1]
  (let [is-all? (= status "all")
        now-page (if (pos? (i page)) (i page) 1)
        books (if is-all?
                (get-books :user name :sort "date" :limit *show-mobile-books-num* :page now-page)
                (get-books :user name :status status :sort "date" :limit *show-mobile-books-num* :page now-page))
        book-num (if is-all? (count-books :user name) (count-books :user name :status status))
        other-book-num (if is-all? 0 (count-books :user name :status-not status))
        pages (.intValue (Math/ceil (/ book-num *show-mobile-books-num*)))
        ]
    (if (and (zero? book-num) (zero? other-book-num))
      (mobile-first-user-page)
      (mobile-layout
        (str *page-title* " - " name)
        mobile-header
        (mobile-border :text (str name "さんのゆるよみ") :height "1.5em" :css "font-size:small;text-align:center" :color "#eee")
        (if (zero? book-num)
          (mobile-no-book-box status)
          (map mobile-book->html books)
          )
        (mobile-pager name status now-page pages)
        (mobile-menu name)
        mobile-footer
        )
      )
    )
  )
