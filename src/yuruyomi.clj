(ns yuruyomi
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use 
     simply simply.date
     [hiccup.core :only [html]]
     [compojure.core :only [defroutes GET POST wrap!]]
     [ring.util.servlet :only [defservice]]
     [ring.util.response :only [redirect]]
     am.ik.clj-gae-ds.core
     [yuruyomi clj-gae-ds-wrapper]
     [yuruyomi.model book setting user]
     [yuruyomi.view html admin mobile]
     [yuruyomi.util seq cache]
     [yuruyomi.cron twitter user]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [clojure.contrib.logging :as log]
     [compojure.route :as route]
     [ring.middleware.session :as session]
     )
  )

(defn escaped-param [& args] (escape-input (apply param args)))
(defn escaped-params [& args] (map escape-input (apply params args)))

(defroutes app
  ; pc {{{
  (GET "/" req (let [name (escaped-param req "name")]
                 (if (or (nil? name) (su2/blank? name))
                   (index-page) (redirect (str "/user/" name)))))
  (GET "/user/:name" req (user-page (escaped-param req "name")))
  (GET "/user/:name/history" req (history-page (escaped-param req "name")))
  (GET "/user/:name/history/:page" req (history-page (escaped-param req "name")) :page (escaped-param req "page"))
  (GET "/user/:name/:status" req (user-page (escaped-param req "name") :status (escaped-param req "status")))
  (GET "/user/:name/:status/:page" req (user-page (escaped-param req "name") :status (escaped-param req "status") :page (escaped-param req "page")))
  
  (GET "/book/:id" req (book-page (escaped-param req "id")))
  (GET "/tweet" req (redirect (apply redirect-to-twitter (escaped-params req "title" "author" "status"))))
  (GET "/search" req (apply search-page (escaped-params req "user" "mode" "keyword" "page" "user_only")))
  (GET "/status" _ (status-page))
  ; }}}

  (GET "/test/session" {session :session} {:body (pr-str session)})
  (GET "/test/session/:key/:val" {sess :session, prms :params}
       (let [key (keyword (prms "key"))]
         {
          :session (assoc sess key
                          (case key
                            :fn (fn [x y] (+ x y))
                            :else (prms "val")
                            )
                          )
          :body (str "session set (" (prms "key") " = " (prms "val") ")")
          }
         )
       )
  (GET "/test/session/fn_test/:x/:y" {sess :session, prms :params}
       (let [x (i (prms "x"))
             y (i (prms "y"))]
         {:body (str "result = " ((:fn sess) x y))
          :session sess
          }
         )
       )

  ; mobile {{{
  (GET "/m/" req (let [name (escape-input (escaped-param req "name"))]
                   (if (or (nil? name) (su2/blank? name)) (mobile-index-page) (redirect (str "/m/" name)))))
  (GET "/m/:name" req (mobile-user-page (escaped-param req "name")))
  (GET "/m/:name/history" req (mobile-history-page (escaped-param req "name")))
  (GET "/m/:name/history/:page" req (mobile-history-page (escaped-param req "name") :page (escaped-param req "page")))
  (GET "/m/:name/:status" req (mobile-user-page (escaped-param req "name") :status (escaped-param req "status")))
  (GET "/m/:name/:status/:page" req (mobile-user-page (escaped-param req "name") :status (escaped-param req "status") :page (escaped-param req "page")))
  (GET "/mb/:id" req (mobile-book-page (escaped-param req "id")))
  ; }}}

  (GET "/ajax/getimage" req (ajax-get-book-image (param req "id")))

  ; app {{{
  (GET "/admin/" req (admin-index-page (param req "page")))
  (GET "/admin/del" req (do (delete-book (param req "id")) (redirect "/admin/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/admin/")))
  (GET "/admin/test" req (do (apply twitter-test (params req "user" "image" "text")) (redirect "/admin/")))
  (GET "/admin/history" req (admin-history-page (param req "page")))
  (GET "/admin/search_twitter" req (admin-search-twitter-page (param req "mode")))
  (GET "/admin/save" req (do (save-tweet (param req "id")) (redirect "/admin/")))

  (GET "/admin/cron/twitter" [] (do (collect-tweets) "fin"))
  (GET "/admin/cron/user" [] (do (collect-user) "fin"))
  ; }}}

  (route/not-found (not-found-page))
  )

(wrap! app session/wrap-session)
(defservice app)

