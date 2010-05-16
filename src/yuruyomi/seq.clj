(ns yuruyomi.seq
  (:use simply)
  )

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

