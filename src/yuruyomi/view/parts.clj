(ns yuruyomi.view.parts
  )

; footer {{{
(def footer
  [:div {:id "footer"}
   [:img {:src "http://code.google.com/appengine/images/appengine-silver-120x30.gif" :alt "Powered by Google App Engine"}]
   [:ul
    [:li [:a {:href ""} "���ѵ���"]]
    [:li [:a {:href ""} "�إ��"]]
    [:li [:a {:href "http://github.com/liquidz/yuruyomi"} "������������"]]
    [:li "Copyright &copy; 2010 " [:a {:href "http://twitter.com/uochan/"} "@uochan"] ". All rights reserved."]
    ]
   ]
  ) ; }}}

; search-form {{{
(defn search-form [name]
  [:form {:method "GET" :action (str "/user/" name "/search")}
   [:select {:name "mode"}
    [:option {:value "title" :selected "selected"} "�����ȥ�"]
    [:option {:value "author"} "����"]
    ]
   [:input {:type "text" :name "keyword"}]
   ;[:input {:type "hidden" :name "user" :value name}]
   [:input {:type "submit" :value "����"}]
   ]
  ) ; }}}

