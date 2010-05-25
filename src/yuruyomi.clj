(ns yuruyomi
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use 
     simply simply.date
     [hiccup.core :only [html]]
     [compojure.core :only [defroutes GET POST]]
     [ring.util.servlet :only [defservice]]
     [ring.util.response :only [redirect]]
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper collect-twitter]
     [yuruyomi.model book setting]
     [yuruyomi.view html admin]
     [yuruyomi.util seq cache]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [compojure.route :as route]
     )
  )

(defroutes app
  (GET "/" [] (index-page))
  (GET "/user/:name" req (show-user-html (param req "name")))
  (GET "/history/:name" req (show-history-html (param req "name")))
  (GET "/search" req (show-search-html (param req "keyword") (param req "mode")))

  (GET "/ajax/getimage" req (ajax-get-book-image (param req "id")))

  ; admin
  (GET "/admin/" [] (admin-index-page))
  (GET "/admin/del" req (do (delete-book (param req "id")) (redirect "/admin/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/admin/")))
  (GET "/admin/test" req (do (twitter-test (param req "text")) (redirect "/admin/")))

  (GET "/admin/cron/collect" [] (do (collect-tweets) "fin"))

  (route/not-found (not-found-page))
  )

(defservice app)

