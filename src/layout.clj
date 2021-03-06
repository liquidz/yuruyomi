(ns layout
  (:use
     [simply core]
     [hiccup.core :only [html]]
     )
  (:require
     [clojure.contrib.logging :as log]
     )
  )

(def *doc-type*
  {:xhtml "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
   }
  )

(defn- if!nil [x] (if (! nil? x) x))
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
(defn- pc-meta [lang content-type charset]
  (map->meta-html {:Content-Language lang
                   :Content-Type (str content-type "; charset=" charset)
                   :Content-Script-Type "text/javascript"
                   :Content-Style-Type "text/css"
                   })
  )

(defn js->html [srcs] (map (make-html-fn :script {:type "text/javascript"} :src) srcs))
(defn css->html [& hrefs]
  (map (make-html-fn :link {:rel "stylesheet" :type "text/css"} :href) hrefs)
  )
(defn rss->html [& hrefs]
  (map (make-html-fn :link {:rel "alternate" :type "application/rss+xml" :title "no-title"} :href) hrefs)
  )

(defnk- base-layout [:head [] :body [] :html-attr nil :head-attr nil :body-attr nil :before "" :after ""]
  (str before
       (html
         [:html (if!nil html-attr)
          [:head (if!nil head-attr) head]
          [:body (if!nil body-attr) body]]
         )
       after
       )
  )

(defnk- pc-layout [title :head [] :js [] :css [] :lang "ja"
                   :content-type "text/html" :charset "UTF-8" & body]
  (base-layout
    :before (:xhtml *doc-type*)
    :html-attr {:xmlns "http://www.w3.org/1999/xhtml" :lang lang}
    :head (list
            (pc-meta lang content-type charset)
            [:link {:rel "shortcut icon" :href "/favicon.ico"}]
            (if-not (empty? js) (js->html js))
            (if-not (empty? css) (css->html css))
            (if-not (empty? head) head)
            [:title title]
            )
    :body body
    )
  )

(defn mobile-layout [title & body]
  (base-layout
    :head [:title title]
    :body-attr {:style "margin:0;padding:0;"}
    :body body
    )
  )


;(defnk layout [title :mobile? false & args]
;  (apply (if mobile? mobile-layout pc-layout) (cons title args))
;  )

(def layout pc-layout)


