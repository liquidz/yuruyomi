(ns yuruyomi.view.html
  (:use
     simply
     [yuruyomi.util seq]
     [yuruyomi.model book history user]
     [yuruyomi.view parts book]
     layout
     )
  (:require 
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     )
  )

(def *page-title* "ゆるよみ ベータ")

(defn ajax-get-book-image [id]
  (let [b (se/find-first #(= id (str (:id %))) (get-books))]
    (get-book-image (:title b) (:author b))
    )
  )

(def *show-finish-books-num* 15)
(def *show-history-num* 20)
(def *show-search-num* 20)
(def *show-books-num* 20)
(def *main-menu*
  {"all" "すべての本"
   "reading" "読んでる本"
   "want" "欲しい本"
   "have" "持ってる本"
   }
  )
(def *etc-menu*
  {"finish" "読み終わった本"
   "history" "読書履歴"
   }
  )

; =pager {{{
(defnk pager [name status now-page max-page :link (fn [x] (str "/" x))]
  (when (> max-page 1)
    (let [prev-label "前のページ"
          next-label "次のページ"]
      [:ul {:class "pager"}
       [:li {:class "prev"}
        (if (> now-page 1)
          [:a {:href (str "/user/" name "/" status (link (dec now-page)))} prev-label]
          [:span prev-label]
          )
        ]
       (map
         (fn [p]
           (if (= p now-page)
             [:li {:class "page now"} [:span p]]
             [:li {:class "page"} [:a {:href (str "/user/" name "/" status (link p))} p]]
             )
           )
         (take max-page (iterate inc 1))
         )
       [:li {:class "next"}
        (if (< now-page max-page)
          ;[:a {:href (str "/user/" name "/" status "/" (inc now-page))} next-label]
          [:a {:href (str "/user/" name "/" status (link (inc now-page)))} next-label]
          [:span next-label]
          )
        ]
       ]
      )
    )
  ) ; }}}

(defn escape-input [s]
  (if (nil? s)
    ""
    (-> s delete-html-tag (su2/replace #"[\"'<>]" ""))
    )
  )

(defn selected? [x y]
  (if (= x y) "selected" "")
  )

(defnk make-info-ul [menu-pair :name "" :class "" :select "" :user-data nil]
  [:ul {:class class}
    (map
      (fn [k]
        [:li [:a {:id (str k "_books")
                  :href (str "/user/" name (if (= k "all") "" (str "/" k)))
                  :class (selected? select k)}
              (get menu-pair k)
              ;(when (and (! = k "all") (! nil? user-data) (! nil? (get user-data (keyword k))))
              (when (or (= k "all") (and (! nil? user-data) (! nil? (get user-data (keyword k)))))
                (str " ("
                     (if (= k "all")
                       (fold (fn [x res] (+ (get user-data x) res)) 0 (list :reading :want :finish :have))
                       (get user-data (keyword k))
                       )
                     ")")
                )
              ]
         ]
        )
      (keys menu-pair))
    ]
  )

; =history-page {{{
(defnk history-page [name :page 1]
  (let [now-page (if (pos? (i page)) (i page) 1)
        histories (find-history :user name :sort "date" :limit *show-history-num* :page now-page)
        pages (.intValue (Math/ceil (/ (count-histories :user name) *show-history-num*)))
        status "history"
        user-data (first (get-user :user name))]
    (layout
      (str *page-title* " - " name)
      :css ["/css/main.css"]
      [:div {:id "header"}
       [:h1 [:a {:href "/" :id "himg"} *page-title*]]
       [:p "ゆる～く読書。ゆる～く管理。"]
       (search-form name)
       ]

      [:div {:id "info"} 
       [:p [:a {:href (str "http://twitter.com/" name)}
            [:img {:src (-> (get-books :user name :limit 1 :offset 0) first :icon)}]]]
       [:h2 name]
       (make-info-ul *main-menu* :name name :class "main" :select status :user-data user-data)
       (make-info-ul *etc-menu* :name name :class "etc" :select status :user-data user-data)
       ]

      [:div {:id "container"} 
       [:h2 name "さんの読書履歴"]
       (if (empty? histories)
         [:h3 "まだ履歴がありません"]
         [:table
          [:tr [:th "タイトル"] [:th "著者"] [:th "ステータス"] [:th "日付"] [:th "つぶやき"]]
          (map
            (fn [h]
              [:tr
               [:td (:title h)]
               [:td (:author h)]
               [:td (get *status-text* (:before h)) " → "(get *status-text*  (:after h))]
               [:td (:date h)]
               [:td (:text h)]
               ]
              )
            histories
            )
          ]
         )
       ]

      (pager name status now-page pages)
      footer
      )
    )
  ) ; }}}

; =first-user-page {{{
(def *test-tweet* (str "http://twitter.com/home?status=" (url-encode "本のタイトル これ読んでる！ #yuruyomi")))
(defn first-user-page [name]
  [:div {:id "exp"}
   [:h2 name "さんはまだ本を登録していません"]
   [:p [:span {:class "reading"} "読んでいる本"] "、" [:span {:class "want"} "読みたい本"] "をつぶやいてみよう！"]
   [:div {:id "sample"}
    [:p [:a {:href *test-tweet*} "つぶやいてみる"]]
    [:img {:src "/img/sample.png"}]
    ]
   [:div {:id "caution"}
    [:h3 "注意点"]
    [:ul
     [:li "ツイートを" [:strong "非公開"] "にしていると、つぶやいても反映されません"]
     [:li "つぶやいてから反映までに時間がかかります。気長に待ちましょう"]
     [:li "詳しくは" [:a {:href ""} "ヘルプ"] "を参照してください"]
     ]
    ]
   ]
  ) ; }}}

; =search-page {{{
(defn search-page [name mode text page]
  (let [now-page (if (and (! nil? page) (pos? (i page))) (i page) 1)
        key (case mode
              ["title" "author"] (keyword (str mode "-like"))
              :else :title-like
              )
        escaped-text (escape-input text)
        books (if (> (count escaped-text) 1) (get-books key escaped-text :limit 1000 :offset 0) ())
        pages (.intValue (Math/ceil (/ (count books) *show-search-num*)))
        status "search"
        user-data (first (get-user :user name))
        ]
    (layout
      (str *page-title* " - " name)
      :css ["/css/main.css"]
      [:div {:id "header"}
       [:h1 [:a {:href "/" :id "himg"} *page-title*]]
       [:p "ゆる～く読書。ゆる～く管理。"]
       (search-form name)
       ]

      [:div {:id "info"} 
       [:p [:a {:href (str "http://twitter.com/" name)}
            [:img {:src (-> (get-books :user name :limit 1 :offset 0) first :icon)}]]]
       [:h2 name]
       (make-info-ul *main-menu* :name name :class "main" :select status :user-data user-data)
       (make-info-ul *etc-menu* :name name :class "etc" :select status :user-data user-data)
       ]

      [:div {:id "container"} 
       [:h2 (if (= mode "title") "タイトル" "著者") ": &quot;" escaped-text "&quot; の検索結果"]
       (if (empty? books)
         [:h3 "該当するレコードが存在しませんでした"]
         [:table
          [:tr [:th "ユーザ"] [:th "タイトル"] [:th "著者"] [:th "ステータス"] [:th "日付"]]
          (map
            (fn [b]
              [:tr
               [:td [:a {:href (str "/user/" (:user b))} (:user b)]]
               [:td (:title b)]
               [:td (:author b)]
               [:td (get *status-text* (:status b))]
               [:td (:date b)]
               ]
              )
            (take *show-search-num* (drop (* (dec now-page) *show-search-num*) books))
            )
          ]
         )
       ]

      (pager name status now-page pages
             :link (fn [p] (str "?mode=" mode "&keyword=" (url-encode escaped-text) "&page=" p)))
      footer
      )
    )
  ) ; }}}

(defn no-book-box [class]
  (let [box (fn [label] [:div {:class (str "book no_book_msg " class)} [:h3 label]])]
    (list
      (case class
        "reading" (box "読んでる本はありません")
        "want" (box "欲しい本はありません")
        "have" (box "持ってる本はありません")
        "finish" (box "読み終わった本はありません")
        )
      )
    )
  )

; =user-page {{{
(defnk user-page [name :status "all" :page 1]
  (let [is-all? (= status "all")
        is-finish? (= status "finish")
        now-page (if (pos? (i page)) (i page) 1)
;        books (if is-finish?
;                (get-books :user name :status status :limit *show-finish-books-num* :page now-page :sort "date")
;                (get-books :user name :status-not "finish"); :sort "date")
;                )
        books (if is-all?
                (get-books :user name :limit *show-books-num* :page now-page :sort "date")
                (get-books :user name :status status :limit *show-books-num* :page now-page :sort "date")
                )
        book-num (apply count-books (concat (list :user name) (if is-all? () (list :status status))))
;        other-book-count (if is-finish?
;                           (count-books :user name :status-not "finish" :limit 1 :offset 0)
;                           (count-books :user name :status "finish" :limit 1 :offset 0))
        other-book-num (if is-all?  0 (count-books :user name :status-not status :limit 1 :offset 0))
        pages (.intValue (Math/ceil (/ book-num *show-books-num*)))
        user-data (first (get-user :user name))
        ]
    (layout
      (str *page-title* " - " name)
      :js ["/js/jquery-1.4.2.min.js" "/js/main.js"]
      :css ["/css/main.css"]

      [:div {:id "header"}
       [:h1 [:a {:href "/" :id "himg"} *page-title*]]
       [:p "ゆる～く読書。ゆる～く管理。"]
       (search-form name)
       ]

      (if (and (empty? books) (zero? other-book-num))
        (first-user-page name)
        (list
          [:div {:id "info"}
           [:p [:a {:href (str "http://twitter.com/" name)}
                [:img {:src (-> books first :icon)}]]]
           [:h2 name]
           (make-info-ul *main-menu* :name name :class "main" :select status :user-data user-data)
           (make-info-ul *etc-menu* :name name :class "etc" :select status :user-data user-data)
           ]

          [:div {:id "container"} 
           (cond
             (empty? books) (no-book-box status)
;             is-finish? (if (empty? books)
;                          [:h3 "読み終わった本はありません"]
;                          (map book->html books)
;                          )
;             (and (= status "all") (empty? books)) [:h3 "読んでる本、読みたい本などはありません"]
             :else (map book->html books)
;             (let [x (group :status books)]
;                     (concat
;                       (if (empty? (:reading x)) (no-book-box "reading") (map book->html (:reading x)))
;                       (if (empty? (:want x)) (no-book-box "want") (map book->html (:want x)))
;                       (if (empty? (:have x)) (no-book-box "have") (map book->html (:have x)))
;                       (if (empty? (:finish x)) (no-book-box "finish") (map book->html (:finish x)))
;                       )
;                     )
             )
           ]

          (pager name status now-page pages)
          )
        )

      footer
      )
    )
  ) ; }}}

  ; index {{{
  (defn index-page []
    (layout
      *page-title*
      :css ["/css/top.css"]
      :js ["/js/jquery-1.4.2.min.js" "/js/jquery.fieldtag.min.js" "/js/top.js"]

      [:div {:id "header"} [:h1 {:id "himg"} *page-title*]]
      [:div {:id "exp"}
        [:p {:id "copy"} "ゆるよみはTwitterでつぶやくだけで読書管理ができるゆる～いサービスです。"]
	    [:form {:method "GET" :action "/"}
	      [:fieldset
	        [:legend "早速始める"]
	        [:input {:type "text" :name "name" :title "TwitterIDを入力"}]
	        [:input {:type "submit" :value "あなたのゆるよみを確認" :class "btn"}]
	        ]
	      ]
	   ]
	   footer
	)
  ) ; }}}

                                                                                                                                                                                                         ; not-found {{{
                                                                                                                                                                                                         (defn not-found-page []
                                                                                                                                                                                                         (layout
                                                                                                                                                                                                         (str *page-title* " - page not found")
                                                                                                                                                                                                                                             [:h1 "page not found"]
                                                                                                                                                                                                                                                                 )
                                                                                                                                                                                                                                                                 ) ; }}}

