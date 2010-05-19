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

(defroutes app
  (GET "/" _ (show-html))
  (GET "/user/:name" _ (show-user-html (first (params _ "name"))))

  (GET "/admin/" _ (show-admin-html))
  (GET "/admin/save" _ (save-user-data _))
  (GET "/admin/del" req (delete-data req))
  (GET "/admin/clear" _ (do (clear-max-id) (redirect "/")))
  (GET "/admin/test" _ (do (twitter-test (-> _ (params "text") first)) (redirect "/")))

  (GET "/admin/cron/collect" _ (do (collect-tweets) (redirect "/")))

  (route/not-found "<h1>page not found</h1>")
  )

(defservice app)

