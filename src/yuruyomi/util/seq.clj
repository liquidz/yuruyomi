(ns yuruyomi.util.seq
  (:use simply)
  (:require [clojure.contrib.str-utils :as su])
  (:require [clojure.contrib.str-utils2 :as su2])
  )

(comment
(defn group
  ([col] (group (fn [x] x) col))
  ([get-key-f col]
   (fold
     (fn [x res]
       (let [key (-> x get-key-f keyword)]
         (assoc res key (if (nil? (key res)) (list x) (cons x (key res))))
         )
       ) {} col)
   )
  )
  )

(defn extended-split [s reg ignore]
  (let [ir (string->regexp "\\" ignore ".+?\\" ignore)
        grp (su/re-partition ir s)
        ]
    (loop [tmp "", res (), ls grp]
      (let [top (first ls)]
        (cond
          (empty? ls) (reverse (remove empty? (cons tmp res)))
          (starts-with? top ignore) (recur (str tmp (su2/replace top (string->regexp "\\" ignore) ""))
                                          res (rest ls))
          :else (let [[head & tail] (su2/split top reg)]
                  (recur "" (cons (str tmp head) res) (concat tail (rest ls)))
                  )
          )
        )
      )
    )
  )

