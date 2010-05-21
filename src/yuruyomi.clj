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
     ;layout
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [compojure.route :as route]
     )
  (:import [com.google.appengine.api.memcache MemcacheServiceFactory Expiration])
  )

(defroutes app
  (GET "/" [] (index-page))
  (GET "/user/:name" req (show-user-html (param req "name")))

  (GET "/hoge" [] (str (cache-fn "neko" (fn [] (println "kiteru") (+ 1 3)) :expiration 30)))

  ; admin
  (GET "/admin/" [] (admin-index-page))
  (GET "/admin/del" req (do (delete-book (param req "id")) (redirect "/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/")))
  (GET "/admin/test" req (do (twitter-test (param req "text")) (redirect "/")))

  (GET "/admin/cron/collect" [] (do (collect-tweets) (redirect "/")))

  (route/not-found (not-found-page))
  )

(defservice app)

