(ns yuruyomi
  (:gen-class :extends javax.servlet.http.HttpServlet)
  ; use {{{
  (:use 
     [simply :only [case delete-html-tag i]]
     [hiccup.core :only [html]]
     [compojure.core :only [defroutes GET POST wrap!]]
     [ring.util.servlet :only [defservice]]
     [ring.util.response :only [redirect]]
     [yuruyomi.model.book :only [delete-book]]
     [yuruyomi.model.setting :only [clear-max-id]]
     [yuruyomi.cron.twitter :only [collect-tweets twitter-test save-tweet]]
     [yuruyomi.cron.user :only [collect-user]]
     [yuruyomi.view html admin mobile]
     [yuruyomi.util session]
     [twitter :only [get-twitter-oauth-url get-twitter-oauth-access-token
                     get-twitter-screen-name twitter-logined?]]
     ) ; }}}
  ; require {{{
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [clojure.contrib.logging :as log]
     [compojure.route :as route]
     [ring.middleware.session :as session]
     keys
     ) ; }}}
  )

(defn escape-input [s]
  (if (nil? s) ""
    (-> s delete-html-tag (su2/replace #"[\"'<>]" ""))
    )
  )

(defn get-param [params key] (-> key params escape-input))
(defn get-params [params & keys] (map #(get-param params %) keys))

(defroutes app
  ; pc {{{
  (GET "/" {session :session, params :params}
       (let [name (get-param params "name")]
         (if (or (nil? name) (su2/blank? name))
           (index-page session) (redirect (str "/user/" name)))))
  (GET "/user/:name" {params :params}
       (user-page (get-param params "name")))
  (GET "/user/:name/history" {params :params}
       (history-page (get-param params "name")))
  (GET "/user/:name/history/:page" {params :params}
       (let [[name page] (get-params params "name" "page")]
         (history-page name :page page)))
  (GET "/user/:name/:status" {params :params}
       (let [[name status] (get-params params "name" "status")]
         (user-page name :status status)))
  (GET "/user/:name/:status/:page" {params :params} 
       (let [[name status page] (get-params params "name" "status" "page")]
       (user-page name :status status :page page)))
  
  (GET "/book/:id" {params :params} (book-page (get-param params "id")))
  (GET "/tweet" {params :params} 
       (redirect (apply redirect-to-twitter (get-params params "title" "author" "status"))))
  (GET "/search" {params :params}
       (apply search-page (get-params params "user" "mode" "keyword" "page" "user_only")))
  (GET "/status" _ (status-page))

  (GET "/login" {session :session, params :params}
       (if (logined? session) (assoc (redirect "/") :session session) ;(with-session session (redirect "/"))
         (let [[url rt tw] (oauth-url)]
           ;(with-session session (redirect url) :twitter tw :request-token rt)
           (assoc (redirect url) :session (assoc session :twitter tw :request-token rt))
           )
         )
       )
  (GET "/login/:pin" {session :session, params :params}
       {:session session :body (pr-str session)}
;       (if (logined? session) (assoc (redirect "/") :session session) ;(with-session session (redirect "/"))
;         (let [[tw at] (get-twitter-oauth-access-token (:twitter session) (:request-token session) (get-param params "pin"))]
;           ;(with-session session (redirect "/") :access-token at :twitter tw)
;           (assoc (redirect "/") :session (assoc session :access-token at :twitter tw))
;           )
;         )
       )

  ; }}}

;  (GET "/test" {session :session}
;       (let [tw (:twitter session)
;             logined? (if (! nil? tw) (twitter-logined? tw) false)
;             sn (if logined? (get-twitter-screen-name tw) "guest")
;             ]
;         {:session session
;          :body (str "logined? = " logined? ", screen name = " sn)
;          }
;         )
;       )
;
;  (GET "/test/login" {session :session}
;       (let [[url rt tw] (get-twitter-oauth-url keys/*twitter-consumer-key* keys/*twitter-consumer-secret*)]
;         {:session (assoc session :request-token rt :twitter tw)
;          :body url
;          }
;         )
;       )
;  (GET "/test/login/:pin" {session :session, params :params}
;       (let [tw (:twitter session)
;             rt (:request-token session)
;             [at tw2] (get-twitter-oauth-access-token tw rt (get-param params "pin"))
;             ]
;         {:session (assoc session :access-token at :twitter tw2)
;          :body "ok"
;          }
;         )
;       )

  ; mobile {{{
  (GET "/m/" {params :params}
       (let [name (get-param params "name")]
         (if (or (nil? name) (su2/blank? name)) (mobile-index-page) (redirect (str "/m/" name)))))
  (GET "/m/:name" {params :params} (mobile-user-page (get-param params "name")))
  (GET "/m/:name/history" {params :params} (mobile-history-page (get-param params "name")))
  (GET "/m/:name/history/:page" {params :params}
       (let [[name page] (get-params params "name" "page")]
         (mobile-history-page name :page page)))
  (GET "/m/:name/:status" {params :params}
       (let [[name status] (get-params params "name" "status")]
         (mobile-user-page name :status status)))
  (GET "/m/:name/:status/:page" {params :params} 
       (let [[name status page] (get-params params "name" "status" "page")]
         (mobile-user-page name :status status :page page)))
  (GET "/mb/:id" {params :params} (mobile-book-page (get-param params "id")))
  ; }}}

  (GET "/ajax/getimage" {params :params} (ajax-get-book-image (get-param params "id")))

  ; admin {{{
  (GET "/admin/" {params :params} (admin-index-page (get-param params "page")))
  (GET "/admin/del" {params :params} (do (delete-book (get-param params "id")) (redirect "/admin/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/admin/")))
  (GET "/admin/test" {params :params}
       (do (apply twitter-test (get-params params "user" "image" "text")) (redirect "/admin/")))
  (GET "/admin/history" {params :params} (admin-history-page (get-param params "page")))
  (GET "/admin/search_twitter" {params :params} (admin-search-twitter-page (get-param params "mode")))
  (GET "/admin/save" {params :params} (do (save-tweet (get-param params "id")) (redirect "/admin/")))

  (GET "/admin/cron/twitter" [] (do (collect-tweets) "fin"))
  (GET "/admin/cron/user" [] (do (collect-user) "fin"))
  ; }}}

  (route/not-found (not-found-page))
  )

(wrap! app session/wrap-session)
(defservice app)

