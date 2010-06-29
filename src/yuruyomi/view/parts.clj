(ns yuruyomi.view.parts
  (:use 
     simply
     [yuruyomi.model user]
     [yuruyomi.view book]
     [yuruyomi.util.session :only [session->twitter-data]]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     )
  )

(def *page-title* "ゆるよみ ベータ")
(def *no-space* "margin:0;padding:0")

;(defnk mobile-border [:color "#dbef02" :height "5px" :text "&nbsp;" :css "" :align "center"]
(defnk mobile-border [:color "#B5D900" :height "5px" :text "&nbsp;"
                      :text-color "#fff" :css "" :align "center"]
  [:div {:style (str "background-color:" color ";height:" height 
                     ";color:" text-color ";text-align:" align
                     ";font-size:small;" css)} text]
  )

; =search-form {{{
(defnk search-form [name :mode "title" :text "" :user-only false :session {}]
  (let [td (session->twitter-data session)]
    [:form {:method "GET" :action "/search"}
     [:span {:id "login_user"}
      (if (:logined? td)
        [:a {:href (str "/user/" (:screen-name td))} (:screen-name td) "さん"]
        "ゲストさん"
        )
      ]
     [:select {:name "mode"}
      (if (= mode "title") 
        [:option {:value "title" :selected "selected"} "タイトル"]
        [:option {:value "title"} "タイトル"]
        )
      (if (= mode "author")
        [:option {:value "author" :selected "selected"} "著者"]
        [:option {:value "author"} "著者"]
        )
      ]
     [:input {:type "text" :name "keyword" :value text}]
     ;(when (! su2/blank? name)
     (when (:logined? td)
       (if user-only
         [:span [:input {:type "checkbox" :name "user_only" :value "true" :id "user_only" :checked "checked"}]
          [:label {:for "user_only"} "あなたの本だけ"]]
         [:span [:input {:type "checkbox" :name "user_only" :value "true" :id "user_only"}]
          [:label {:for "user_only"} "あなたの本だけ"]]
         )
       )
     [:input {:type "hidden" :name "page" :value "1"}]
     [:input {:type "hidden" :name "user" :value name}]
     [:input {:type "submit" :value "検索"}]
     ]
    )
  ) ; }}}

(defnk login-block [td]
  (if (:logined? td)
    [:p [:a {:href "/logout"} "logout"]]
    [:p [:a {:href "/login"} "login"]]
    )
  )

; =header {{{
(defn pc-header
  ([& args]
   (let [[name & more] (if (keyword? (first args)) (cons "" args) args)
         td (->> more (apply array-map) :session session->twitter-data)
         ]

     [:div {:id "header"}
      [:h1 [:a {:href "/" :id "himg"} *page-title*]]
      [:p "ゆる～く読書。ゆる～く管理。"]
      [:div {:id "search-login"}
       (apply search-form (cons name more))
       (login-block td)
       ]
      ]
     )
   )
  ([] (pc-header ""))
  )

(def mobile-header
  (list
    ;[:p {:style "background: #dbef02;font-size: x-small"} "ゆる～く読書。ゆる～く管理。"]
    [:p {:style "margin:0 0 5px 0;padding:0;text-align:center;"} [:img {:src "/img/mobile_logo.png" :alt *page-title*}]]
    )
  )
; }}}

; =footer {{{
(def pc-footer
  [:div {:id "footer"}
   [:img {:src "http://code.google.com/appengine/images/appengine-silver-120x30.gif" :alt "Powered by Google App Engine"}]
   [:ul
    [:li [:a {:href "http://wiki.github.com/liquidz/yuruyomi/782841"} "利用規約"]]
    [:li [:a {:href "http://wiki.github.com/liquidz/yuruyomi/770195"} "ヘルプ"]]
    [:li [:a {:href "http://github.com/liquidz/yuruyomi"} "ソースコード"]]
    [:li [:a {:href "/status"} "ステータス一覧"]]
    [:li "Copyright &copy; 2010 " [:a {:href "http://twitter.com/uochan/" :target "_blank"} "@uochan"] ". All rights reserved."]
    ]
   ]
  )

(def mobile-footer
  (list
    (mobile-border :color "#aaa" :css "margin-top:5px")
    [:p {:style "margin:0;padding;0;font-size:x-small;text-align:center"}
     "(C) 2010 " [:a {:href "http://twtr.jp/user/uochan"} "@uochan"]]
    )
  )
; }}}

; =selected? {{{
(defn selected? [x y]
  (if (= x y) "selected" "")
  ) ; }}}

; =make-info-ul {{{
(defnk make-info-ul [menu-pair :name "" :class "" :select ""]
  (let [user-data (first (get-user :user name))]
    [:ul {:class class}
     (map
       (fn [k]
         [:li [:a {:id (str k "_books")
                   :href (str "/user/" name (if (= k "all") "" (str "/" k)))
                   :class (selected? select k)}
               (get menu-pair k)
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
  ) ; }}}

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
          [:a {:href (str "/user/" name "/" status (link (inc now-page)))} next-label]
          [:span next-label]
          )
        ]
       ]
      )
    )
  ) ; }}}

; =no-book-box {{{
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
  ) ; }}}

; =table {{{
(defnk table [body :header () :attr {} :footer? true]
  [:table attr
   (when (! empty? header)
     [:thead [:tr (map (fn [h] [:th h]) header)]]
     )
   (when (and (! empty? header) footer?)
     [:tfoot [:tr (map (fn [h] [:td h]) header)]]
     )
   [:tbody
    (map 
      (fn [[i r]]
        [:tr {:class (if (odd? i) "odd" "even")}
        (map
          (fn [[j d]] [:td {:class (if (odd? j) "odd" "even")} d])
          (se/indexed r))
         ]
        )
      (se/indexed body))
    ]
   ]
  ) ; }}}

; tweet-form {{{
(defn tweet-form [title author]
  [:form {:id "tweet" :method "GET" :action "/tweet" :target "_blank"}
   [:input {:type "hidden" :name "title" :value title}]
   [:input {:type "hidden" :name "author" :value author}]
   [:p "この本を"
    [:select {:name "status"}
     [:option {:value "want" :selected "selected"} "読みたい"]
     [:option {:value "reading"} "読んでる"]
     [:option {:value "have"} "持ってる"]
     [:option {:value "finish"} "読んだ"]
     ]
    [:input {:type "submit" :value "つぶやく"}]
    ]
   ]
  ) ; }}}

; =make-tweet {{{
(defnk make-tweet [title :author nil :status "reading" :custom nil]
  (str "http://twitter.com/home?status="
       (url-encode
         (str title (if (nil? author) "" (str " : " author))
              " " (if (nil? custom) (get *status-text* status) custom)
              " #yuruyomi" 
              )
         )
       )
  ) ; }}}

; =update-status-form {{{
(defn update-status-form [id status]
  [:form {:id "tweet" :method "POST" :action "/change"}
   [:input {:type "hidden" :name "id" :value id}]
   (case status
     "reading" [:div
                [:textarea {:name "comment"}]
                [:input {:type "hidden" :name "status" :value "finish"}]
                [:input {:type "checkbox" :name "twitter-update" :id "twitter-update" :checked "checked"}]
                [:label {:for "twitter-update"} "Twitterでつぶやく"]
                [:input {:type "submit" :value "読み終わった"}]
                ]
     :else
     [:div
      [:p "ステータス: "
       [:select {:name "status"}
        [:option {:value "reading"} (if (= status "finish") "再読" "読み始めた")]
        (when (= status "finish") [:option {:value "want"} "読みたい"])
        (when (= status "want") [:option {:value "have"} "買った"])
        (when (! = status "finish")
          [:option {:value "finish"} (if (= status "reading") "読み終わった" "読んだ")]
          )
        ]
       ]
      [:p "コメント:"]
      [:textarea {:name "comment"}]
      [:p
       [:input {:type "checkbox" :name "twitter-update" :id "twitter-update" :checked "checked"}]
       [:label {:for "twitter-update"} "Twitterでつぶやく"]
       [:span "&nbsp;"]
       [:input {:type "submit" :value "変更"}]
       ]
      ]
     )
   ]
  ) ; }}}

; =add-book-form {{{
(defn add-book-form [id]
  [:form {:id "tweet" :method "POST" :action "/add"}
   [:input {:type "hidden" :name "id" :value id}]

   [:p "ステータス: "
    [:select {:name "status"}
     [:option {:value "reading"} "読んでる"]
     [:option {:value "want"} "欲しい"]
     [:option {:value "have"} "持ってる"]
     [:option {:value "finish"} "読んだ"]
     ]
    ]
   [:p "コメント:"]
   [:textarea {:name "comment"}]
   [:p
    [:input {:type "checkbox" :name "twitter-update" :id "twitter-update" :checked "checked"}]
    [:label {:for "twitter-update"} "Twitterでつぶやく"]
    [:span "&nbsp;"]
    [:input {:type "submit" :value "登録する"}]
    ]
   ]
  ) ; }}}

