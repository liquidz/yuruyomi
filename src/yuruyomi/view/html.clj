(ns yuruyomi.view.html
  (:use
     [simply core string list]
     [yuruyomi.util seq session]
     [yuruyomi.model book history]
     [yuruyomi.view parts book]
     [yuruyomi.cron.twitter :only [*reading-words* *want-words*
                                   *having-words* *finish-words*]]
     layout
     )
  (:require 
     [clojure.contrib.seq :as se]
     [clojure.contrib.string :as st]
     [clojure.contrib.json :as json]
     )
  )

; = CONSTANTS {{{
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
; }}}

(defn ajax-get-book-image [id]
  (let [b (get-a-book id)]
    (when-not (nil? b)
      (get-book-image (:title b) (:author b) :default *no-book-image*)
      )
    )
  )

(defn- to-json [coll callback]
  (let [data (json/json-str coll)]
    (if (nil? callback) data
      (str callback "(" data ");")
      )
    )
  )

(defnk json-get-books [name :limit "10" :offset "0" :sort "date" :callback nil]
  (let [data (->> (get-books :user name :limit (Integer/parseInt limit)
                             :offset (Integer/parseInt offset) :sort sort)
                  (map #(dissoc % :parent :keyname)))]
    (to-json data callback)
    )
  )
(defnk json-get-book-info [id :callback nil]
  (let [book (get-a-book id)
        data (if (nil? book) nil (dissoc book :parent :keyname))]
    (to-json data callback)
    )
  )

(defn redirect-to-twitter [title author status]
  (let [a (if (st/blank? author) author nil)
        ]
    (if (st/blank? title) "/"
      (make-tweet title :author (if (st/blank? author) nil author) :status status)
      )
    )
  )

(defnk status-page [:name "" :session {}]
  (let [status "statuses"]
    (layout
      (str *page-title* " - ステータス一覧")
      :css ["/css/main.css"]
      (pc-header name :session session)
      (when-not (st/blank? name)
        [:div {:id "info"} 
         [:p [:a {:href (str "http://twitter.com/" name)}
              [:img {:src (-> (get-books :user name :limit 1 :offset 0) first :icon)}]]]
         [:h2 name]
         (make-info-ul *main-menu* :name name :class "main" :select status)
         (make-info-ul *etc-menu* :name name :class "etc" :select status)
         ]
        )

      [:div {:id "container"}
       [:h2 "ステータス一覧"]
       (table
         (partition 1 *reading-words*)
         :header ["読んでるステータス"]
         :footer? false :attr {:id "reading_words"})
       (table
         (partition 1 *want-words*)
         :header ["読みたいステータス"]
         :footer? false :attr {:id "want_words"})
       (table
         (partition 1 *having-words*)
         :header ["持ってるステータス"]
         :footer? false :attr {:id "having_words"})
       (table
         (partition 1 *finish-words*)
         :header ["読了ステータス"]
         :footer? false :attr {:id "finish_words"})
       ]

      pc-footer
      )
    )
  )

(defn check-params [params]
  (layout
    *page-title*
    (map #(identity [:p % " => " (get params %)]) (keys params))
    )
  )

; =history-page {{{
(defnk history-page [name :page "1" :session {}]
  (let [now-page (if (pos? (Integer/parseInt page)) (Integer/parseInt page) 1)
        histories (find-history :user name :sort "date" :limit *show-history-num* :page now-page)
        pages (.intValue (Math/ceil (/ (count-histories :user name) *show-history-num*)))
        status "history"]
    (layout
      (str *page-title* " - " name)
      :css ["/css/main.css"]
      (pc-header name :session session)

      [:div {:id "info"} 
       [:p [:a {:href (str "http://twitter.com/" name)}
            [:img {:src (-> (get-books :user name :limit 1 :offset 0) first :icon)}]]]
       [:h2 name]
       (make-info-ul *main-menu* :name name :class "main" :select status)
       (make-info-ul *etc-menu* :name name :class "etc" :select status)
       ]

      [:div {:id "container"} 
       [:h2 name "さんの読書履歴"]
       (if (empty? histories)
         [:h3 "まだ履歴がありません"]
         (table
           (map
             #(list [:a {:href (str "/book/" (:book-id %))} (:title %)]
                    (:author %)
                    (str (get *status-text* (:before %)) " &raquo; " (get *status-text*  (:after %)))
                    (:date %) (:text %)
                    )
             histories)
           :header ["タイトル" "著者" "ステータス" "日付" "つぶやき"]
           :attr {:id "history_table"}
           )
         )
       ]

      (pager name status now-page pages)
      pc-footer
      )
    )
  ) ; }}}

; =book-page {{{
(defnk book-page [id :session {}]
  (let [book (get-a-book id)]
    (if (nil? book)
      (layout "book not found" "book not found")
      (let [title (:title book)
            books (get-books :title title)
            fb (first books)
            author (:author (se/find-first #(! st/blank? (:author %)) books))
            img (get-book-image title author :size "large"
                                :default *no-book-image*)
            histories (find-history :title title :sort "date" :limit 10 :offset 0)
            td (session->twitter-data session)
            your-book (if (:logined? td) (se/find-first #(= (:user %) (:screen-name td)) books) nil)
            ]
        (layout
          (str *page-title* " - " title)
          :css ["/css/main.css"]
          :js ["/js/jquery-1.4.2.min.js" "/js/main.js"]
          (pc-header :session session)

          (if (empty? books)
            [:div {:id "container"} [:h2 "&quote;" title "&quote; という本は見つかりませんでした"]]
            [:div {:id "container"} 
             [:h2 title (when-not (nil? author) (str " - " author "(著)"))]
             [:div {:id "large_book_image"}
              [:img {:src img :alt title}]
              (if-not (:logined? td)
                (tweet-form title author)
                (if (nil? your-book)
                  (add-book-form (:id fb))
                  (update-status-form (:id your-book) (:status your-book))
                  )
                )
              ]
             [:h3 "この本を登録している人"]
             (map (fn [b]
                    [:a {:href (str "/user/" (:user b))}
                     [:img {:src (:icon b)
                            :alt (str (:user b) " - " (get *status-text* (:status b)))}]
                     ]
                    ) books)
             [:h4 "この本に関する履歴"]
             (table
               (map
                 #(list
                    [:a {:href (str "/user/" (:user %))} (:user %)] (:date %)
                    (str (get *status-text* (:before %)) " &raquo; " (get *status-text* (:after %)))
                    (:text %)
                    )
                 histories)
               :header ["ユーザ" "日付" "ステータス" "つぶやき"]
               :attr {:id "book_info_history_table"}
               :footer? false
               )
             ]
            )
          pc-footer
          )
        )
      )
    )
  ) ; }}}

; =first-user-page {{{
(defn first-user-page [name]
  [:div {:id "exp"}
   [:h2 name "さんはまだ本を登録していません"]
   [:p [:span {:class "reading"} "読んでいる本"] "、" [:span {:class "want"} "読みたい本"] "をつぶやいてみよう！"]
   [:div {:id "sample"}
    [:p [:a {:href (make-tweet "本のタイトル" :custom "これ読んでる！")} "つぶやいてみる"]]
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
(defnk search-page [name mode text page user-only :session {}]
  (let [pn (Integer/parseInt (if (st/blank? page) "1" page))
        now-page (if (and (! nil? page) (pos? pn)) pn 1)
        key (case mode
              ("title" "author") (keyword (str mode "-like"))
              :title-like
              )
        books (if-not (st/blank? text)
                (apply get-books (concat (list key text :limit 1000 :offset 0)
                                         (if (and (! st/blank? name) (= user-only "true")) (list :user name) ())))
                ())
        pages (.intValue (Math/ceil (/ (count books) *show-search-num*)))
        status "search"
        ]
    (layout
      (str *page-title* " - " text)
      :css ["/css/main.css"]
      :js ["/js/jquery-1.4.2.min.js" "/js/main.js"]
      (pc-header name :text text :mode mode :user-only (= user-only "true") :session session)

      (when-not (st/blank? name)
        [:div {:id "info"} 
         [:p [:a {:href (str "http://twitter.com/" name)}
              [:img {:src (-> (get-books :user name :limit 1 :offset 0) first :icon)}]]]
         [:h2 name]
         (make-info-ul *main-menu* :name name :class "main" :select status)
         (make-info-ul *etc-menu* :name name :class "etc" :select status)
         ]
        )

      [:div {:id "container"} 
       [:h2 (if (= mode "title") "タイトル" "著者") ": &quot;" text "&quot; の検索結果"]
       (if (empty? books)
         [:h3 "該当するレコードが存在しませんでした"]
         (map book->html books)
         )
       ]

      (pager name status now-page pages
             :link (fn [p] (str "?mode=" mode "&keyword=" (url-encode text) "&page=" p)))
      pc-footer
      )
    )
  ) ; }}}

; =user-page {{{
(defnk user-page [name :status "all" :page "1" :session {}]
  (let [is-all? (= status "all")
        is-finish? (= status "finish")
        now-page (if (pos? (Integer/parseInt page)) (Integer/parseInt page) 1)
        books (if is-all?
                (get-books :user name :limit *show-books-num* :page now-page :sort "date")
                (get-books :user name :status status :limit *show-books-num* :page now-page :sort "date")
                )
        book-num (apply count-books (concat (list :user name) (if is-all? () (list :status status))))
        other-book-num (if is-all?  0 (count-books :user name :status-not status :limit 1 :offset 0))
        pages (.intValue (Math/ceil (/ book-num *show-books-num*)))
        td (session->twitter-data session)
        ]
    (layout
      (str *page-title* " - " name)
      :js ["/js/jquery-1.4.2.min.js" "/js/main.js"]
      :css ["/css/main.css"]

      (pc-header name :session session)

      (if (and (empty? books) (zero? other-book-num))
        (first-user-page name)
        (list
          [:div {:id "info"}
           [:p [:a {:href (str "http://twitter.com/" name)}
                [:img {:src (-> books first :icon)}]]]
           [:h2 name]
           (make-info-ul *main-menu* :name name :class "main" :select status)
           (make-info-ul *etc-menu* :name name :class "etc" :select status)
           ]

          [:div {:id "container"} 
           [:input {:type "hidden" :id "show_qr_code" :value "true"}]
           (cond
             (empty? books) (no-book-box status)
             :else (map book->html books)
             )
           ]

          (pager name status now-page pages)
          )
        )

      ; your page
      (when (= name (:screen-name td))
        [:form {:id "new-book-form" :method "POST" :action "/new"}
         [:fieldset
          [:legend "新しい本を追加"]
          [:p "タイトル: " [:input {:type "text" :name "title"}]]
          [:p "著者: " [:input {:type "text" :name "author"}]]
          [:p "ステータス" status-select]
          [:p "コメント" [:textarea {:name "comment"}]]
          [:p
           update-twitter-check
           [:input {:type "submit" :class "confirm" :value "登録"}]
           ]
          ]
         ]
        )

      pc-footer
      )
    )
  ) ; }}}

; index {{{
(defn index-page [session]
  (let [new-books
        (take 5 (delete-duplicates :title (find-history :before "new" :sort "date" :limit 10 :offset 0)))
        active-user (take 5 (get-active-user :limit 20))
        recent-tweets
        (take 5 (delete-duplicates :text (find-history :sort "date" :limit 10 :offset 0)))
        ]
    (layout
      *page-title*
      :css ["/css/top.css"]
      :js ["/js/jquery-1.4.2.min.js" "/js/jquery.fieldtag.min.js" "/js/top.js"]

      ;[:div {:id "header"} [:h1 {:id "himg"} *page-title*]]
      (pc-header :session session)
      [:div {:id "exp"}
       [:p {:id "copy"} "ゆるよみはTwitterでつぶやくだけで読書管理ができるゆる～いサービスです。"]
       [:form {:method "GET" :action "/"}
        [:fieldset
         [:legend "早速始める"]
         [:input {:type "text" :name "name" :title "TwitterIDを入力" :value (screen-name session :default "")}]
         [:input {:type "submit" :value "あなたのゆるよみを確認" :class "btn"}]
         ]
        ]
       (table
         (map #(list [:a {:href (str "/book/" (:book-id %))} (:title %)]) new-books)
         :header ["最近登録された本"]
         :footer? false
         :attr {:id "new_book_table"}
         )

       (table
         (map #(list [:a {:href (str "/user/" %)} %]) active-user)
         :header ["アクティブなユーザ"]
         :footer? false
         :attr {:id "active_user_table"}
         )

       (table
         (map #(list [:a {:href (str "/user/" (:user %) "/history")} (:text %)]) recent-tweets)
         :header ["最近のつぶやき"]
         :footer? false
         :attr {:id "recent_tweets_table"}
         )
       ]
      pc-footer
      )
    )
  ) ; }}}


; not-found {{{
(defn not-found-page []
  (layout
    (str *page-title* " - page not found")
    [:h1 "page not found"]
    )
  ) ; }}}

