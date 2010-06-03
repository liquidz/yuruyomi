(ns yuruyomi.view.html
  (:use
     simply
     [yuruyomi.util seq]
     [yuruyomi.model book history user]
     [yuruyomi.view book]
     layout
     )
  (:require [clojure.contrib.seq-utils :as se])
  )

(def *page-title* "ゆるよみ ベータ")

(defn ajax-get-book-image [id]
  (let [b (se/find-first #(= id (str (:id %))) (get-books))]
    (p get-book-image (:title b) (:author b))
    )
  )

; footer {{{
(def footer
  [:div {:id "footer"}
   [:img {:src "http://code.google.com/appengine/images/appengine-silver-120x30.gif" :alt "Powered by Google App Engine"}]
   [:ul
    [:li [:a {:href ""} "利用規約"]]
    [:li [:a {:href ""} "ヘルプ"]]
    [:li [:a {:href "http://github.com/liquidz/yuruyomi"} "ソースコード"]]
    [:li "Copyright &copy; 2010 " [:a {:href "http://twitter.com/uochan/"} "@uochan"] ". All rights reserved."]
    ]
   ]
  ) ; }}}

; search-form {{{
(def search-form
  [:form {:method "GET" :action "/search"}
   [:select {:name "mode"}
    [:option {:value "title" :selected "selected"} "タイトル"]
    [:option {:value "author"} "著者"]
    ]
   [:input {:type "text" :name "keyword"}]
   [:input {:type "submit" :value "検索"}]
   ]
  ) ; }}}

(def *main-menu*
  {"all" "すべての本"
   "reading" "読んでる本"
   "want" "欲しい本"
   "have" "持ってる本"
   }
  )
(def *etc-menu*
  {:finish "読み終わった本"}
  )

(defn selected? [x y]
  (if (= x y) "selected" "")
  )

(defnk make-info-ul [menu-pair :name "" :class "" :select ""]
  [:ul {:class class}
    (map
      (fn [k]
        [:li [:a {:id (str k "_books")
                  :href (str "/user/" name (if (= k "all") "" (str "/" k)))
                  :class (selected? select k)} (get menu-pair k)]]
        )
      (keys menu-pair))
    ]
  )

(defn show-history-html [name]
  (layout
    (str *page-title* " - " name)
    [:h1 (str name "'s history")]
    (map (fn [h]
           [:p (:title h) " - " (:author h) ", " (:before h) "=>" (:after h) " (" (:date h) ")"]
           )
         (find-history :name name)
         )
    )
  )

; =search {{{
(defn show-search-html [text mode]
  (layout
    *page-title*

    (map (fn [b]
           [:p (:title b) " : " (:author b) " (" (:user b) ")"]
           )
         (let [res (apply get-books
                (list
                  (keyword (case mode
                             ["title" "author" "user"] (str mode "-like")
                             :else "title-like"
                             )
                           )
                  text
                  )
                )]
           res
           )
         )
    )
  ) ; }}}

(defnk user-page [name :status "all"]
  (let [is-finish? (if (= status "finish")
                     true false
                     )
        books (get-books :user name)]
    (layout
      (str *page-title* " - " name)
      :js ["/js/jquery-1.4.2.min.js" "/js/jquery.fieldtag.min.js" "/js/main.js"]
      :css ["/css/main.css"]

      [:div {:id "header"}
       ;[:h1 [:a {:href "/" :id "himg"} *page-title*]]
       ;[:p "ゆる～く読書。ゆる～く管理。"]
       search-form
       ]

      [:div {:id "info"}
       [:p [:a {:href (str "http://twitter.com/" name)}
            [:img {:src (-> books first :icon)}]]]
       [:h2 name]
       (make-info-ul *main-menu* :name name :class "main" :select status)
       (make-info-ul *etc-menu* :name name :class "etc" :select status)
       ]

      [:div {:id "container"} (map book->html books) ]

      footer
      )
    )
  )


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

