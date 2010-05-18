(ns yuruyomi.view.html
  (:use
     simply
     [hiccup.core :only [html]]
     am.ik.clj-gae-ds.core
     ;[yuruyomi clj-gae-ds-wrapper seq collect-twitter]
     [yuruyomi seq collect-twitter]
     [yuruyomi.model book setting]
     )
  )

; support function {{{
(def *doc-type*
  {:xhtml "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
   }
  )

(defn- make-html-fn [tag base target]
  (fn [x]
    [tag
     (cond
       (vector? x) (apply assoc (concat (list base target (first x)) (rest x)))
       :else (assoc base target (str x))
       )
     ]
    )
  )

(defn meta->html [key value] [:meta {:http-equiv key :content value}])
(defn map->meta-html [m] (map #(apply meta->html %) (seq m)))
(defn js->html [srcs] (map (make-html-fn :script {:type "text/javascript"} :src) srcs))
(defn css->html [& hrefs]
  (map (make-html-fn :link {:rel "stylesheet" :type "text/css"} :href) hrefs)
  )
(defn rss->html [& hrefs]
  (map (make-html-fn :link {:rel "alternate" :type "application/rss+xml" :title "no-title"} :href) hrefs)
  )

(defnk layout [title :head [] :js [] :css []
               :xhtml? true :lang "ja"
               :content-type "text/html"
               :charset "UTF-8"
               :mobile? false
               & body]
  (str
    (if xhtml? (:xhtml *doc-type*) "")
    (html
      [:html (if xhtml? {:xmlns "http://www.w3.org/1999/xhtml" :lang lang})
       (map->meta-html {:Content-Language lang
                        :Content-Type (str content-type "; charset=" charset)
                        :Content-Script-Type "text/javascript"
                        :Content-Style-Type "text/css"
                        })
       [:head
        (js->html js) (css->html css) (if (! empty? head) head)
        [:title title]
        ]
       [:body body]
       ]
      )
    )
  )
; }}}

(defn save-form-html [name]
  [:form {:method "GET" :action "/save"}
   [:input {:type "hidden" :name "name" :value name}]
   [:p "title " [:input {:type "text" :name "title"}]]
   [:p "author" [:input {:type "text" :name "author"}]]
   [:select {:name "flag"}
    [:option {:value "ing"} "ing"]
    [:option {:value "fin"} "fin"]
    [:option {:value "wnt"} "wnt"]
    [:option {:value "has"} "has"]
    ]
   [:input {:type "submit" :value "save"}]
   ]
  )

(def flag->text
  {"ing" "読中" "fin" "読了" "wnt" "欲しい" "has" "所持"})

(defnk book->html [e :show-user? false :show-flag? false]
;  (let [[title author date user flag] (get-props e :title :author :date :user :flag)
;        ]
    [:p "title: " (:title e) " / author: " (:author e)
     (if show-user?
       (list " by " [:a {:href (str "/user/" (:user e))} (:user e)])
       )
     " (" (:date e)
     (if show-flag?
       (list ", " (get flag->text (:flag e)) ")")
       ")"
       )
     ; ↓削除は認証をいれてから
     ;[:a {:href (str "/del?id=" (:id e))} "del"]
     ]
;    )
  )

(defn get-user-data-html [name]
  ;(let [ls (group #(get-prop % :flag) (get-user-books name))
  (let [ls (group :flag (get-user-books name))
        ]
    (concat
      (list [:h2 (get flag->text "ing")])
      (map book->html (:ing ls))
      (list [:h2 (get flag->text "wnt")])
      (map book->html (:wnt ls))
      (list [:h2 (get flag->text "fin")])
      (map book->html (:fin ls))
      (list [:h2 (get flag->text "has")])
      (map book->html (:has ls))
      )
    )
  )

(defn show-user-html [name]
  (layout
    (str "yuruyomi alpha - " name)
    [:h1 (str name "'s books")]
    (get-user-data-html name)
    ;[:hr]
    ;(save-form-html name)
    )
  )

(defn show-html []
  (layout
    "yuruyomi alpha"
    ;[:p "max id = " (get-max-id)]
    ;[:hr]
    (map #(book->html % :show-user? true :show-flag? true) (get-all-books))
    ;[:hr]
    ;[:p [:a {:href "/collect"} "collect twitter data"]]
    ;[:p [:a {:href "/clear"} "clear max id"]]
    )
  )


