(ns yuruyomi
  (:gen-class :extends javax.servlet.http.HttpServlet)
  ; use {{{
  (:use 
     [simply :only [case delete-html-tag i escape !]]
     [hiccup.core :only [html]]
     [compojure.core :only [defroutes GET POST wrap!]]
     [ring.util.servlet :only [defservice]]
     [ring.util.response :only [redirect]]
     [yuruyomi.model.book :only [delete-book get-a-book change-book-status save-new-book]]
     [yuruyomi.model.setting :only [clear-max-id]]
     [yuruyomi.cron.twitter :only [collect-tweets twitter-test save-tweet]]
     [yuruyomi.cron.user :only [collect-user]]
     [yuruyomi.view html admin mobile]
     [yuruyomi.view.book :only [*status-text*]]
     [yuruyomi.util session]
     [twitter :only [get-twitter-oauth-url get-twitter-oauth-access-token
                     get-twitter-screen-name twitter-logined? twitter-update]]
     ) ; }}}
  ; require {{{
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [clojure.contrib.logging :as log]
     [compojure.route :as route]
     [ring.middleware.session :as session]
     ) ; }}}
  )

;(defn escape-input [s] (if (nil? s) "" (-> s delete-html-tag (su2/replace #"[\"'<>]" ""))))
(defn get-param [params key] (-> key params escape))
(defn get-params [params & keys] (map #(get-param params %) keys))

(defroutes app
  ; pc {{{
  (GET "/" {session :session, params :params}
       (let [name (get-param params "name")]
         (if (or (nil? name) (su2/blank? name))
           (index-page session) (redirect (str "/user/" name)))))
  (GET "/user/:name" {session :session, params :params}
       (user-page (get-param params "name") :session session))
  (GET "/user/:name/history" {session :session, params :params}
       (history-page (get-param params "name")))
  (GET "/user/:name/history/:page" {session :session, params :params}
       (let [[name page] (get-params params "name" "page")]
         (history-page name :page page :session session)))
  (GET "/user/:name/:status" {session :session, params :params}
       (let [[name status] (get-params params "name" "status")]
         (user-page name :status status :session session)))
  (GET "/user/:name/:status/:page" {session :session, params :params} 
       (let [[name status page] (get-params params "name" "status" "page")]
       (user-page name :status status :page page :session session)))
  
  (GET "/book/:id" {session :session, params :params} 
       (book-page (get-param params "id") :session session))
  (GET "/tweet" {session :session, params :params} 
       (redirect (apply redirect-to-twitter (get-params params "title" "author" "status"))))
  (GET "/search" {session :session, params :params}
       (apply search-page
              (concat 
                (get-params params "user" "mode" "keyword" "page" "user_only")
                (list :session session))))
  (GET "/status" {session :session} (status-page :session session))

  (POST "/change" {session :session, params :params}
       (let [[id status comment update?] (get-params params "id" "status" "comment" "twitter-update")
             book (get-a-book id)
             td (session->twitter-data session)]
         (when (and (:logined? td) (= (:screen-name td) (:user book)))
           (change-book-status id status :text comment)
           (when (! su2/blank? update?)
             (twitter-update (:twitter session)
                             (str
                               "[テスト]"
                               (:title book) " "
                               (when (! su2/blank? (str (:author book) " ")))
                               (get *status-text* status) "。" comment
                               )
                             )
             )
           )
         (redirect
           (if (su2/blank? id)
             "/"
             (str "/book/" id)
             )
           )
         )
       )

  (POST "/new" {session :session, params :params}
        (let [[title author status comment update?] (get-params params "title" "author" "status" "twitter-update")
              td (session->twitter-data session)
              ]
          (when (:logined? td)
            (save-new-book
              )
            )
          (redirect "/")
          )
        )

  (POST "/add" {session :session, params :params}
        (let [[id status comment update?] (get-params params "id" "status" "comment" "twitter-update")
              book (get-a-book id)
              td (session->twitter-data session)
              ]
          (when (:logined? td)
            (save-new-book :user (:screen-name td) :id id :status status :icon (:image td) :text comment)
            (when (! su2/blank? update?)
              (twitter-update (:twitter session)
                              (str
                                "[TEST]"
                                (:title book) " "
                                (when (! su2/blank? (str (:author book) " ")))
                                (get *status-text* status) "。" comment
                                )
                              )
              )
            )
          (redirect
            (if (su2/blank? id)
              "/"
              (str "/book/" id)
              )
            )
          )
        )

  ;(GET "/home" {session :session, params :params})

  (GET "/login" {session :session, params :params}
       (if (logined? session)
         (redirect "/")
         (let [verifier (get-param params "oauth_token")]
           (if (su2/blank? verifier)
             (let [[url rt tw] (oauth-url)]
               (assoc (redirect url) :session (assoc session :twitter tw :request-token rt))
               )
             ;(let [[at tw] (get-twitter-oauth-access-token (:twitter session) (:request-token session) verifier)]
             (let [[at tw] (get-twitter-oauth-access-token (:twitter session) (:request-token session))]
               (assoc (redirect "/") :session (assoc session :access-token at :twitter tw))
               )
             )
           )
         )
       )
  (GET "/login/:pin" {session :session, params :params}
       (if (logined? session) (assoc (redirect "/") :session session) ;(with-session session (redirect "/"))
         (let [[at tw] (get-twitter-oauth-access-token (:twitter session) (:request-token session) (get-param params "pin"))]
           ;(with-session session (redirect "/") :access-token at :twitter tw)
           (assoc (redirect "/") :session (assoc session :access-token at :twitter tw))
           )
         )
       )
  (GET "/logout" _ (assoc (redirect "/") :session {}))
  (GET "/update" {session :session, params :params}
       (when (logined? session)
         (twitter-update (:twitter session) (get-param params "status"))
         )
       (redirect "/")
       )

  ; }}}

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

  ; ajax {{{
  (GET "/ajax/getimage" {params :params} (ajax-get-book-image (get-param params "id")))
  ; }}}

  ; admin {{{
  (GET "/admin/" {params :params} (admin-index-page (get-param params "page")))
  (GET "/admin/del" {params :params} (do (delete-book (get-param params "id")) (redirect "/admin/")))
  (GET "/admin/clear" [] (do (clear-max-id) (redirect "/admin/")))
  (GET "/admin/test" {params :params}
       (do (apply twitter-test (get-params params "user" "image" "text")) (redirect "/admin/")))
  (GET "/admin/history" {params :params} (admin-history-page (get-param params "page")))
  (GET "/admin/search_twitter" {params :params} (admin-search-twitter-page (get-param params "mode")))
  (GET "/admin/save" {params :params} (do (save-tweet (get-param params "id")) (redirect "/admin/")))
  (GET "/admin/set_book_id" _ (do (admin-set-book-id) "fin"))

  (GET "/admin/cron/twitter" [] (do (collect-tweets) "fin"))
  (GET "/admin/cron/user" [] (do (collect-user) "fin"))
  ; }}}

  (GET "/check" {params :params}
       (check-params params)
       )

  (route/not-found (not-found-page))
  )

(wrap! app session/wrap-session)
(defservice app)

