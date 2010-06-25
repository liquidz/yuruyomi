(ns yuruyomi.bot
  (:use twitter)
  (:require keys)
  )

(defn bot-tweet [s]
  (let [tw (get-twitter-authorized-instance
             keys/*twitter-consumer-key*
             keys/*twitter-consumer-secret*
             keys/*twitter-yuruyomi-access-token*
             keys/*twitter-yuruyomi-access-token-secret*
             )]
    (twitter-update tw s)
    )
  )

