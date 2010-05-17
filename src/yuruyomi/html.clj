(ns yuruyomi.html
  (:use
     simply
     [hiccup.core :only [html]]
     am.ik.clj-gae-ds.core
     ;[yuruyomi clj-gae-ds-wrapper seq collect-twitter]
     [yuruyomi seq collect-twitter]
     [yuruyomi.model book setting]
     )
  )

(defn meta->html [key value] [:meta {:http-equiv key :content value}])
(defn map->meta-html [m] (map #(apply meta->html %) (seq m)))
(defn js->html [srcs] (map (fn [src] [:script {:type "text/javascript" :src src}]) srcs))
; media
(defn css->html [hrefs] (map (fn [href] [:link {:rel "stylesheet" :type "text/css" :href href}]) hrefs))
; rss -> rsses
(defnk rss->html [href :title ""]
  [:link {:rel "alternate" :type "application/rss+xml" :title title :href href}]
  )

(defnk layout [title :head [] :js [] :css []
               :xhtml? true :lang "ja"
               :content-type "text/html"
               :charset "UTF-8"
               & body]
  [:html {:xmlns "http://www.w3.org/1999/xhtml" :lang lang}
   (map->meta-html {:Content-Language lang
                    :Content-Type (str content-type "; charset=" charset)
                    :Content-Script-Type "text/javascript"
                    :Content-Style-Type "text/css"
                    })
   [:head
    [:title title]
    (js->html js) (css->html css)
    head
    ]
   [:body body]
   ]
  )

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

(defn book->html [e]
;  (let [[title author date user flag] (get-props e :title :author :date :user :flag)
;        ]
    [:p (:title e) "/" (:author e) " (" (:date e) ", " (:user e) ", " (:flag e) ")"
     ; ↓削除は認証をいれてから
     ;[:a {:href (str "/del?id=" (-> e get-key get-id))} "del"]
     [:a {:href (str "/del?id=" (:id e))} "del"]
     ]
;    )
  )

(defn get-user-data-html [name]
  (println "x = " (get-user-books name))
  ;(let [ls (group #(get-prop % :flag) (get-user-books name))
  (let [ls (group :flag (get-user-books name))
        ]
    (concat
      (list [:h4 "ing"])
      (map book->html (:ing ls))
      (list [:h4 "wnt"])
      (map book->html (:wnt ls))
      (list [:h4 "fin"])
      (map book->html (:fin ls))
      (list [:h4 "has"])
      (map book->html (:has ls))
      )
    )
  )

(defn show-user-html [name]
  (layout
    "show-user"
    [:h3 name]
    (get-user-data-html name)
    [:hr]
    (save-form-html name)
    )
  )

(defn show-html []
  (layout
    "top"
    [:p "max id = " (get-max-id)]
    [:hr]
    (map book->html (get-all-books))
    [:hr]
    [:p [:a {:href "/collect"} "collect twitter data"]]
    [:p [:a {:href "/clear"} "clear max id"]]
    )
  )


