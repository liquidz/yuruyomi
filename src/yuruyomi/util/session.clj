(ns yuruyomi.util.session
  (:use
     simply
     twitter
     )
  (:require keys)
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

(defnk screen-name [session :default "ゲスト"]
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
