(ns yuruyomi.view.parts
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

