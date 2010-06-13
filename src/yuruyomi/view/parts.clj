(ns yuruyomi.view.parts
  (:use simply)
  )

(def *page-title* "ゆるよみ ベータ")
(def *no-space* "margin:0;padding:0")

(defnk mobile-border [:color "#dbef02" :height "5px" :text "&nbsp;" :css ""]
  [:div {:style (str "background-color:" color ";height:" height ";font-size:small;" css)} text]
  )

; footer {{{
(def footer
  [:div {:id "footer"}
   [:img {:src "http://code.google.com/appengine/images/appengine-silver-120x30.gif" :alt "Powered by Google App Engine"}]
   [:ul
    [:li [:a {:href ""} "利用規約"]]
    [:li [:a {:href "http://wiki.github.com/liquidz/yuruyomi/770264"} "ヘルプ"]]
    [:li [:a {:href "http://github.com/liquidz/yuruyomi"} "ソースコード"]]
    [:li "Copyright &copy; 2010 " [:a {:href "http://twitter.com/uochan/"} "@uochan"] ". All rights reserved."]
    ]
   ]
  )

(def mobile-footer
  (list
    (mobile-border :color "#aaa")
    [:p {:style "margin:0;padding;0;font-size:x-small;text-align:center"}
     "(C) 2010 " [:a {:href "http://twtr.jp/user/uochan"} "@uochan"]]
    )
  )

; }}}

; search-form {{{
(defn search-form [name]
  [:form {:method "GET" :action (str "/user/" name "/search")}
   [:select {:name "mode"}
    [:option {:value "title" :selected "selected"} "タイトル"]
    [:option {:value "author"} "著者"]
    ]
   [:input {:type "text" :name "keyword"}]
   ;[:input {:type "hidden" :name "user" :value name}]
   [:input {:type "submit" :value "検索"}]
   ]
  ) ; }}}

(def mobile-header
  (list
    ;[:p {:style "background: #dbef02;font-size: x-small"} "ゆる～く読書。ゆる～く管理。"]
    [:p {:style "text-align:center;"} [:img {:src "/img/mobile_logo.png" :alt *page-title*}]]
    )
  )
