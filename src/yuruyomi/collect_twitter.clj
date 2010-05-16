(ns yuruyomi.collect-twitter
  (:use
     simply
     twitter
     am.ik.clj-gae-ds.core
     yuruyomi.book
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils :as su]
     [clojure.contrib.str-utils2 :as su2]
     )
  )

(def *reading-words* (list "読んでる" "読んでいる" "読中" "読み始めた" "読み中"))
(def *want-words* (list "欲しい" "読みたい" "読んでみたい"))
(def *finish-words* (list "読み終わった" "読みおわた" "読了"))
(def *having-words* (list "買った" "持ってる"))
(def *words-list* (list *reading-words* *want-words* *finish-words* *having-words*))

(defstruct book :user :title :author :flag)

(defn has-word? [s col] (some #(! = -1 (.indexOf s %)) col))
(defn has-word-all? [s] (some #(has-word? s %) *words-list*))
(defn delete-hash-tag [s] (su2/replace s #"\s*#yuruyomi\s*" ""))
(defn delete-words [s] (su2/join " " (remove has-word-all? (su2/split s #"\s+"))))

; "hello RT @hoge ddd / o-sa- xxx #tag"
; => "ddd / o-sa- hello #tag"
(defn convert-rt-string [s]
  (let [[msg & more] (su2/split s #"\s*(RT|Rt|rt)\s*@\w+:?\s*")]
    (cond
      (empty? more) s
      :else (su2/join " " (map #(if (has-word-all? %) msg %) (su2/split (last more) #"\s+")))
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

(defn string->book [s]
  (let [[title & more] (extended-split s #"\s*/\s*" "\"")]
    (struct book title (if (empty? more) "" (first more)) "d" "u")
    )
  )

(defn string->book-title-author [s]
  (let [[title & more] (extended-split s #"\s*/\s*" "\"")]
    [title (if (empty? more) "" (first more))]
    )
  )

(defn collect-books [col] ; {{{
  (let [txts col
        conv (map (comp convert-rt-string delete-hash-tag) txts)
        res (map (fn [wl] (filter #(has-word? % wl) conv)) *words-list*)
        [r w f h] (map #(map (comp string->book delete-words) %) res)
        ]

    [r w f h]
    )
  ) ; }}}

(defn tweets->books [tweets]
  (let [conv (map (fn [t] (assoc t :text ((comp convert-rt-string delete-hash-tag) (:text t)))) tweets)
        [r w f h] (map (fn [wl] (filter #(has-word? (:text %) wl) conv)) *words-list*)
        ]

    (map (fn [t]
           (let [[title author] (string->book-title-author (delete-words (:text t)))]
             (println "delete-words = " (delete-words (:text t)))
             (assoc t :title title :author author)
             )
           )
         (concat
           (map #(assoc % :flag "ing") r)
           (map #(assoc % :flag "wnt") w)
           (map #(assoc % :flag "fin") f)
           (map #(assoc % :flag "has") h)
           )
         )
    )
  )

(def *yuruyomi-tag* "#yuruyomi")
(def *yuruyomi-entity* "yuruyomi-core")

;(defn- string->long [s] (.longValue (java.lang.Integer. s)))
(defn- string->long [s] (Long/parseLong s))

; {{{
(defn get-yuruyomi-max-id []
  (let [res (-> (q *yuruyomi-entity*) (add-filter "key" = "max-id") query-seq)]
    (if (zero? (count res)) "" (get-prop (first res) :value))
    )
  )
(defn clear-yuruyomi-max-id []
  (let [res (-> (q *yuruyomi-entity*) (add-filter "key" = "max-id") query-seq)]
    (when (! zero? (count res))
      (-> res first get-key ds-delete)
      )
    )
  )

(defn update-max-id [max-id]
  (let [res (-> (q *yuruyomi-entity*) (add-filter "key" = "max-id") query-seq)]
    (if (empty? res)
      (ds-put (map-entity *yuruyomi-entity* :key "max-id" :value (str max-id)))
      (let [x (first res)]
        (set-prop x :value (str max-id))
        (ds-put x)
        )
      )
    )
  )
; }}}

(defn collect-tweets []
  (let [last-id (get-yuruyomi-max-id)
        res (apply twitter-search-all (concat (list *yuruyomi-tag*); :lang "ja")
                                              (if (su2/blank? last-id) ()
                                                (list :since-id (string->long last-id)))))
        max-id (:max-id res)
        ]
    ; update max id
    (update-max-id (:max-id res))

    (let [tmp (tweets->books (:tweets res))]
      (foreach #(println %) tmp)
      (foreach #(save-book (:from-user %) (:title %) (:author %) (:flag %)) tmp)
      )

    ;(foreach #(println (:user %) (:title %) (:author %) (:flag %)) (tweets->books (:tweets res)))
    ;(foreach #(println %) (tweets->books (:tweets res)))
    )
  )



