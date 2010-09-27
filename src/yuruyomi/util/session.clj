(ns yuruyomi.util.session
  (:use
     simply.core
     twitter
     [yuruyomi.view.book :only [*status-text*]]
     )
  (:require
     keys
     [clojure.contrib.string :as st]
     )
  )

(defn with-session [session body & update-key-value]
  (let [new-session (if (or (nil? update-key-value) (! zero? (mod (count update-key-value) 2)))
                      session (apply assoc (cons session update-key-value)))]
    {:session new-session :body body}
    )
  )

(defn logined? [session]
  (let [tw (:twitter session)]
    (if (nil? tw) false
      (twitter-logined? tw)
      )
    )
  )

(defnk screen-name [session :default "*guest*"]
  (if (logined? session)
    (get-twitter-screen-name (:twitter session))
    default
    )
  )

(defnk profile-image [session :default "/img/npc.png"]
  (if (logined? session)
    (get-twitter-profile-image-url (:twitter session))
    default
    )
  )

(defn oauth-url []
  (get-twitter-oauth-url keys/*twitter-consumer-key* keys/*twitter-consumer-secret*)
  )


(defn session->twitter-data [session]
  (assoc session
         :logined? (logined? session)
         :screen-name (screen-name session)
         :image (profile-image session)
         )
;  {:logined? true
;   :screen-name "testuser"
;   :image "/img/npc.png"
;   }
  )

(defnk tweet-when-update [session :title "" :author "" :status "" :comment ""]
  (twitter-update
    (:twitter session)
    (str
      ;"[テスト]"
      title (when-not (st/blank? author) (str ":" author))
      " [" (get *status-text* status) "] " comment
      )
    )
  )

