(ns yuruyomi.util.seq
  (:use [simply core string])
  (:require [clojure.contrib.string :as st])
  )

(defn extended-split [reg ignore s]
  (let [ir (string->regexp "\\" ignore ".+?\\" ignore)
        grp (st/partition ir s)
        ]
    (loop [tmp "", res (), ls grp]
      (let [top (first ls)]
        (cond
          (empty? ls) (reverse (remove empty? (cons tmp res)))
          ;(starts-with? top ignore) (recur (str tmp (st/replace top (string->regexp "\\" ignore) ""))
          (starts-with? top ignore) (recur (str tmp top) res (rest ls))
          :else (let [[head & tail] (st/split reg top)]
                  (recur "" (cons (str tmp head) res) (concat tail (rest ls)))
                  )
          )
        )
      )
    )
  )

(defnk filters [col :ignore-nil? true & preds]
  (filter #(every? (fn [pre] (if (nil? pre) ignore-nil? (pre %))) preds) col)
  )

(defn map-remove [f m]
  (apply
    hash-map
    (reduce (fn [res [k v]]
              (if (f k v)
                res
                (concat res (list k v))
                )
              ) () m)
    )
  )

