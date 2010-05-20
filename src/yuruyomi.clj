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

(defroutes app
  (GET "/" [] (show-html))
  (GET "/user/:name" req (show-user-html (param req "name")))

  ; admin
  (GET "/admin/" [] (show-admin-html))
  (GET "/admin/del" req (do (delete-book (param req "id")) (redirect "/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/")))
  (GET "/admin/test" req (do (twitter-test (param req "text")) (redirect "/")))

  (GET "/admin/cron/collect" [] (do (collect-tweets) (redirect "/")))

  (route/not-found 
    (layout "page not found" [:h1 "page not found"])
    )
  )

(defservice app)

