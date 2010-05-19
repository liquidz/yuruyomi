(ns yuruyomi
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use 
     simply
     simply.date
     [hiccup.core :only [html]]
     [compojure.core :only [defroutes GET POST]]
     [ring.util.servlet :only [defservice]]
     [ring.util.response :only [redirect]]
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper seq collect-twitter]
     [yuruyomi.model book setting]
     [yuruyomi.view html]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [compojure.route :as route]
     )
  )

(defn save-user-data [req]
  (let [[name title author flag] (params req "name" "title" "author" "flag")]
    (save-book name title author flag)
    (redirect (str "/user/" name))
    )
  )

(defn delete-data [req]
  (let [[id] (params req "id")]
    (delete-book id)
    (redirect "/")
    )
  )

; {{{
(defn get-last-time []
  (let [res (query-seq (q "core"))]
    (if (zero? (count res)) "" (get-prop (first res) :last-time))
    )
  )

(defn td []
  (calendar-format :year "-" :month "-" :day " " :hour ":" :minute ":" :second)
  )

(defn collect-twitter-data []
  (let [res (query-seq (q "core"))]
    (if (zero? (count res))
      (ds-put (map-entity "core" :last-time (td)))
      (let [x (first res)]
        (set-prop x :last-time (td))
        (ds-put x)
        )
      )
    )
  )
; }}}

(defroutes app
  (GET "/" _ (show-html))
  (GET "/user/:name" _ (show-user-html (first (params _ "name"))))
  ;(GET "/save" _ (save-user-data _))
  ;(GET "/del" req (delete-data req))
  ;(GET "/cron/collect" _ (collect-twitter-data))
  (GET "/cron/collect" _ (do (collect-tweets) (redirect "/")))
  ;(GET "/clear" _ (do (clear-max-id) (redirect "/")))
  (GET "/test" _ (do (twitter-test (-> _ (params "text") first)) (redirect "/")))

  (route/not-found "<h1>page not found</h1>")
  )

(defservice app)

