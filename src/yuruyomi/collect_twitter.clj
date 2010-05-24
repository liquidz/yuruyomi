(ns yuruyomi.collect-twitter
  (:use
     simply simply.date
     twitter
     [yuruyomi.util seq cache]
     [yuruyomi.model book setting]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     )
  )

; =CONSTANT {{{
(def *reading-words*
  (list "読んでる" "よんでる" "読んでいる" "よんでいる"
        "読中" "読み始めた" "読みはじめた" "よみはじめた" "読み中"))
(def *want-words*
  (list "欲しい" "ほしい" "読みたい" "よみたい" "読んでみたい" "よんでみたい" "買って帰る"
        "買ってかえる" "かって帰る" "かってかえる" "買って行く" "買っていく" "かっていく"))
(def *finish-words*
  (list "読み終わった" "よみおわった" "読みおわた" "よみおわた" "読了"
        "どくりょう" "どくりょ"))
(def *having-words*
  (list "買った" "かった" "買ってしまった" "かってしまった"
        "持ってる" "もってる" "積ん読" "積読" "つんどく" "ゲット" "げっと" "GET"))
(def *words-list* (list *reading-words* *want-words* *finish-words* *having-words*))

(def *yuruyomi-tag* "#yuruyomi_test")
(def *yuruyomi-done-tag* "#done")
; }}}


(defn- string->long [s] (Long/parseLong s))
(defn has-word? [s col] (some #(su2/contains? s %) col))
(defn has-word-all? [s] (some #(has-word? s %) *words-list*))
(defn delete-hash-tag [s] (su2/replace s #"\s*#\w+\s*" ""))
(defn delete-words [s]
  (let [ls (su2/split s #"\s+")
        [n s] (se/find-first #(has-word-all? (second %)) (se/indexed ls))]
    ; 見つかったword以降は全て無視
    (su2/join " " (take n ls))
    )
  )
(defn delete-html-symbol [s] (su2/replace s #"&#\d+;" ""))
(defn except-dones [f col] (remove #(su2/contains? (f %) *yuruyomi-done-tag*) col))

; "hello RT @hoge ddd / o-sa- xxx #tag"
; => "ddd / o-sa- hello #tag"
(defn convert-rt-string [s]
  (let [[msg & more] (su2/split s #"\s*(RT|Rt|rT|rt)\s*@\w+:?\s*")]
    (cond
      (empty? more) s
      :else (su2/join " " (map #(if (has-word-all? %) msg %) (su2/split (last more) #"\s+")))
      )
    )
  )

(defn string->book-title-author [s]
  (let [[title & more] (extended-split s #"\s*[\/\,\.\|\-]\s*" "\"")]
  ;(let [[title & more] (extended-split s #"\s*[\/\,\|]\s*" "\"")]
    [title (if (empty? more) ""
             ; 著者名の後に余分な文字列が入る場合には先頭だけを抜き出す
             (-> more first su2/trim (su2/split #"\s+") first su2/trim)
             )]
    )
  )

(defn tweets->books [tweets]
  (let [conv (except-dones :text tweets)
        conv2 (map (fn [t] (assoc t :text ((comp convert-rt-string delete-hash-tag
                                                 delete-html-symbol delete-html-tag) (:text t)))) conv)
        [r w f h] (map (fn [wl] (filter #(has-word? (:text %) wl) conv2)) *words-list*)
        ]

    (map (fn [t]
           (let [[title author] (string->book-title-author (delete-words (:text t)))]
             (assoc t :title title :author author)
             )
           )
         (sort
           ;(fn [x y] (neg? (.compareTo (:created-at x) (:created-at y))))
           (fn [x y] (str< (:created-at x) (:created-at y)))
           (concat
             (map #(assoc % :status "ing") r)
             (map #(assoc % :status "wnt") w)
             (map #(assoc % :status "fin") f)
             (map #(assoc % :status "has") h)
             )
           )
         )
    )
  )

(defn twitter-test [text]
  (let [res {:created-at (now) :from-user "testuser" :from-user-id 0 :id 0
                    :iso-language-code "" :profile-image-url "" :source "test"
                    :text text :to-user "testuser2" :to-user-id 1
                    }]
    (foreach save-book (tweets->books (list res)))
    )
  )

(defn collect-tweets []
  (let [last-id (get-max-id)
        res (apply twitter-search-all (concat (list *yuruyomi-tag*)
                                              (if (su2/blank? last-id) ()
                                                (list :since-id (string->long last-id)))))
        ]

    (loop [save-targets (-> res :tweets tweets->books)
           local-last-id last-id]
      (cond
        (empty? save-targets) (update-max-id (:max-id res)) ; 最後まで記録できたらQueryのmax-idを記録
        :else (if (try (save-book (first save-targets)) (catch Exception _ false))
                (recur (rest save-targets) (:id (first save-targets)))
                (update-max-id local-last-id) ; 途中で失敗した場合には次回途中から検索するようにIDを記録
                )
        )
      )
    )
  )



