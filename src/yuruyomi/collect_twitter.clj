(ns yuruyomi.collect-twitter
  (:use
     simply simply.date
     twitter
     [yuruyomi.util seq]
     [yuruyomi.model book setting]
     )
  (:require
     [clojure.contrib.str-utils2 :as su2]
     )
  )

; =CONSTANT {{{
(def *reading-words* (list "読んでる" "よんでる" "読んでいる" "よんでいる"
                           "読中" "読み始めた" "読みはじめた" "よみはじめた" "読み中"))
(def *want-words* (list "欲しい" "ほしい" "読みたい" "よみたい" "読んでみたい" "よんでみたい"))
(def *finish-words* (list "読み終わった" "よみおわった" "読みおわた" "よみおわた" "読了"
                          "どくりょう" "どくりょ"))
(def *having-words* (list "買った" "かった" "買ってしまった" "かってしまった"
                          "持ってる" "もってる" "積ん読" "積読" "つんどく"))
(def *words-list* (list *reading-words* *want-words* *finish-words* *having-words*))

(def *yuruyomi-tag* "#yuruyomi_test")
; }}}

(defn- string->long [s] (Long/parseLong s))
(defn has-word? [s col] (some #(! = -1 (.indexOf s %)) col))
(defn has-word-all? [s] (some #(has-word? s %) *words-list*))
(defn delete-hash-tag [s] (su2/replace s #"\s*#yuruyomi_test\s*" ""))
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

(defn string->book-title-author [s]
  (let [[title & more] (extended-split s #"\s*[\/\,\.\|\-\_]\s*" "\"")]
    [title (if (empty? more) "" (first more))]
    )
  )

(defn tweets->books [tweets]
  (let [conv (map (fn [t] (assoc t :text ((comp convert-rt-string delete-hash-tag) (:text t)))) tweets)
        [r w f h] (map (fn [wl] (filter #(has-word? (:text %) wl) conv)) *words-list*)
        ]

    (map (fn [t]
           (let [[title author] (string->book-title-author (delete-words (:text t)))]
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

(defn twitter-test [text]
  (let [res {:created-at (today) :from-user "testuser" :from-user-id 0 :id 0
                    :iso-language-code "" :profile-image-url "" :source "test"
                    :text text :to-user "testuser2" :to-user-id 1
                    }]

    ;(foreach #(save-book (:from-user %) (:title %) (:author %) (:flag %)) (tweets->books (list res)))
    (foreach save-book (tweets->books (list res)))
    )
  )

(defn collect-tweets []
  (let [last-id (get-max-id)
        res (apply twitter-search-all (concat (list *yuruyomi-tag*)
                                              (if (su2/blank? last-id) ()
                                                (list :since-id (string->long last-id)))))
        max-id (:max-id res)
        ]
    ; update max id
    (update-max-id (:max-id res))

    ;(let [tmp (tweets->books (:tweets res))]
    ;  (foreach #(save-book (:from-user %) (:title %) (:author %) (:flag %)) tmp)
    ;  )
    (foreach save-book (tweets->books (:tweets res)))
    )
  )



