(ns yuruyomi.view.mobile
  (:use
     simply
     layout
     [yuruyomi.view parts book]
     [yuruyomi.model book history]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     )
  )

(def *show-mobile-books-num* 10)
(def *show-mobile-history-num* 10)


(defn combine-map [m & more]
  (apply assoc (cons m (apply concat (apply concat (map seq more)))))
  )

(def no-margin {:style "margin:0;padding:0"})
(def no-margin-small (assoc no-margin :style (str (:style no-margin) ";font-size:small")))

(defn- mobile-menu
  ([name]
   (list
     (mobile-border :text "メニュー" :height "1.5em")
     [:ul {:style "list-style:none;margin:0;padding:0;"}
      (when (! nil? name)
        (list
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
          [:li [:span {:style "font-size:small;color:#aaa"} "[5]"]
           [:a {:href (str "/m/" name) :style "font-size:small" :accesskey "5"} "ホーム"]]
          )
        )
      [:li [:span {:style "font-size:small;color:#aaa"} "[0]"]
       [:a {:href "/m/" :style "font-size:small" :accesskey "0"} "トップ"]]
      ]
     )
   )
  ([] (mobile-menu nil))
  )

(defn mobile-no-book-box [status]
  (let [box (fn [label] [:h3 no-margin-small label])]
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
    (let [prev-label "[4.前]"
          next-label "[5.次]"]
      [:div {:class "pager" :style "font-size:small"}
       (mobile-border :color "#ddd")
       (if (> now-page 1)
         [:a {:href (str "/m/" name "/" status (link (dec now-page))) :accesskey "4"} prev-label]
         [:span prev-label]
         )
       (map
         (fn [p]
           (if (= p now-page)
             [:span " " p " "]
             [:span " " [:a {:href (str "/m/" name "/" status (link p))} p] " "]
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
  (let [new-books (find-history :before "new" :sort "date" :limit 5 :offset 0)
        active-user (take 5 (get-active-user :limit 20))
        recent-tweets (find-history :sort "date" :limit 5 :offset 0)
        ]
    (mobile-layout
      *page-title*
      mobile-header
      (mobile-border)
      [:p no-margin-small
       "ゆるよみはTwitterでつぶやくだけで読書管理ができるゆる～いサービスです。" [:br]
       "読んでる本、読みたい本が簡単に共有できます"
       ]
      [:form {:method "GET" :action "/m/"}
       [:p no-margin-small "TwitterIDを入力" [:input {:type "text" :name "name"}]]
       [:input {:type "submit" :value "確認" :style "font-size:small"}]
       ]
      (mobile-border :text "最近登録された本" :height "1.5em")
      (map (fn [b] [:p no-margin-small [:a {:href (str "/mb/" (:title b))} (:title b)]])
           new-books)
      (mobile-border :text "アクティブなユーザ" :height "1.5em")
      (map (fn [u] [:p no-margin-small [:a {:href (str "/m/" u)} u]]) active-user)

      mobile-footer
      )
    )
  )

(defn mobile-book-page [title]
  (let [books (get-books :title title)
        fb (first books)
        author (:author (se/find-first #(! su2/blank? (:author %)) books))
        img (get-book-image (:title fb) (:author fb) :size "medium"
                            :default *default-book-image*)
        histories (find-history :title title :sort "date" :limit 5 :offset 0)
        ]
    (mobile-layout
      *page-title*
      mobile-header
      (mobile-border :text (str title (when (! nil? author) (str " - " author)))
                     :height "1.5em"
                     )
      [:img {:src img}]
      (mobile-border :text "この本を登録している人" :height "1.5em" :css "margin-top:5px")
      (map
        (fn [b] [:p no-margin-small [:a {:href (str "/m/" (:user b))} (:user b)]])
        books)
      (mobile-menu)
      mobile-footer
      )
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
      (mobile-border :text (str name "さんの履歴") :height "1.5em")
      (if (empty? histories)
        [:h3 "まだ履歴はありません"]
        (map
          (fn [h]
            [:p no-margin-small
             [:a {:href (str "/mb/" (:title h))} (:title h)] " " 
             (get *status-text* (:before h)) "&raquo;" (get *status-text* (:after h))
             [:br] [:span {:style "font-size:x-small"} "&nbsp; (" (:date h) ")"]
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
                (get-books :user name :sort "date"
                           :limit *show-mobile-books-num* :page now-page)
                (get-books :user name :status status :sort "date"
                           :limit *show-mobile-books-num* :page now-page))
        book-num (if is-all? (count-books :user name) (count-books :user name :status status))
        other-book-num (if is-all? 0 (count-books :user name :status-not status))
        pages (.intValue (Math/ceil (/ book-num *show-mobile-books-num*)))
        ]
    (if (and (zero? book-num) (zero? other-book-num))
      (mobile-first-user-page)
      (mobile-layout
        (str *page-title* " - " name)
        mobile-header
        ;(mobile-border :text (str name "さんのゆるよみ") :height "1.5em" :color "#eee")
        (mobile-border :text (str name "さんのゆるよみ") :height "1.5em")
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
