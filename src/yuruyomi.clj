(ns yuruyomi
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use 
     simply simply.date
     [hiccup.core :only [html]]
     [compojure.core :only [defroutes GET POST]]
     [ring.util.servlet :only [defservice]]
     [ring.util.response :only [redirect]]
     am.ik.clj-gae-ds.core
     ;[yuruyomi clj-gae-ds-wrapper collect-twitter collect-user-data]
     [yuruyomi clj-gae-ds-wrapper]
     [yuruyomi.model book setting user]
     [yuruyomi.view html admin]
     [yuruyomi.util seq cache]
     [yuruyomi.cron twitter user]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [compojure.route :as route]
     )
  )

(defroutes app
  (GET "/" req (let [name (param req "name")]
                 (if (or (nil? name) (su2/blank? name)) (index-page) (redirect (str "/user/" name)))
                 )
       )
  (GET "/user/:name" req (user-page (param req "name")))
  (GET "/history/:name" req (show-history-html (param req "name")))
  (GET "/search" req (show-search-html (param req "keyword") (param req "mode")))

  (GET "/ajax/getimage" req (ajax-get-book-image (param req "id")))

  ; admin
  (GET "/admin/" [] (admin-index-page))
  (GET "/admin/del" req (do (delete-book (param req "id")) (redirect "/admin/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/admin/")))
  (GET "/admin/test" req (do (twitter-test (param req "text")) (redirect "/admin/")))
  (GET "/admin/history" req (admin-history-page (param req "page")))

  (GET "/admin/cron/twitter" [] (do (collect-tweets) "fin"))
  (GET "/admin/cron/user" [] (do (collect-user) "fin"))

  (route/not-found (not-found-page))
  )

(defservice app)

