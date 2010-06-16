(ns yuruyomi.cron.twitter
  (:use
     simply simply.date
     twitter
     [yuruyomi.util seq cache]
     [yuruyomi.model book setting]
     )
  (:require
     [clojure.contrib.seq-utils :as se]
     [clojure.contrib.str-utils2 :as su2]
     [clojure.contrib.logging :as log]
     )
  )

; =CONSTANT {{{
(def *reading-words*
  (list "読んでる" "よんでる" "読んでいる" "よんでいる"
        "読中" "読み始めた" "読みはじめた" "よみはじめた" "読み中"
        "読み始めて" "読みはじめて" "よみ始めて" "よみはじめて"))
(def *want-words*
  (list "欲しい" "ほしい" "読みたい" "よみたい" "読みたく" "よみたく" "読んでみたい" "よんでみたい" "買って帰"
        "買ってかえ" "かって帰" "かってかえ" "買って行" "買ってい" "かってい" "買う" "買おう"
        "買いたい" "買いたく"))
(def *finish-words*
  (list "読み終わった" "よみおわった" "読みおわた" "よみおわた" "読了"
        "どくりょう" "どくりょ" "読んだ" "よんだ"
        "読み終えた" "読みおえた" "よみ終えた" "よみおえた"))
(def *having-words*
  (list "買った" "かった" "買ってしまった" "かってしまった"
        "買っちゃった" "かっちゃった"
        "持ってる" "もってる" "積ん読" "積読" "つんどく" "ゲット" "げっと" "GET"))
(def *delete-words*
  (list "取り消し" "とりけし" "取消" "取消し"))
(def *words-list* (list *reading-words* *want-words* *finish-words* *having-words* *delete-words*))

(def *yuruyomi-tag* "#yuruyomi")
(def *yuruyomi-done-tag* "#done")
; }}}

;(def *re-book-sep* #"\s*[,、]\s*")
;(def *re-book-sep* #"[\s　]+と[\s　]+")
(def *re-book-sep* #"[\s　]+[と\s　]*と[\s　]+")

(def *re-title-author-sep* #"[\s　]*[:：][\s　]*")


(defn- string->long [s] (Long/parseLong s))
(defn has-word? [s col] (some #(su2/contains? s %) col))
(defn index-of-word [s col] (apply min (map #(.indexOf s %) col)))
(defn has-word-all? [s] (some #(has-word? s %) *words-list*))
(defn delete-hash-tag [s] (su2/replace s #"[\s　]*#\w+[\s　]*" ""))
(defn delete-words [s]
  (let [ls (su2/split s #"[\s　]+")
        [n s] (se/find-first #(has-word-all? (second %)) (se/indexed ls))]
    ; 見つかったword以降は全て無視
    (if (and (! nil? n) (! nil? s))
      (su2/join " " (take n ls))
      s
      )
    )
  )
(defn delete-html-symbol [s] (su2/replace s #"&#\d+;" ""))
(defn except-dones [f col] (remove #(su2/contains? (f %) *yuruyomi-done-tag*) col))

; "hello RT @hoge ddd / o-sa- xxx #tag"
; => "ddd / o-sa- hello #tag"
(defn convert-rt-string [s]
  (let [[msg & more] (su2/split s #"[\s　]*(RT|Rt|rT|rt)[\s　]*@\w+:?[\s　]*")]
    (cond
      (empty? more) s
      :else (su2/join " " (map #(if (has-word-all? %) msg %) (su2/split (last more) #"[\s　]+")))
      )
    )
  )

(defn string->book-title-author [s]
  (let [[title & more] (extended-split s *re-title-author-sep* "\"")]
    [(su2/replace title #"\"" "")
     (if (empty? more) ""
       ; 著者名の後に余分な文字列が入る場合には先頭だけを抜き出す
       (-> more first su2/trim (extended-split #"[\s　]+" "\"") first (su2/replace #"\"" "") su2/trim)
       )]
    )
  )

(defn- set-statuses [& args]
  (if (zero? (mod (count args) 2))
    (fold (fn [[ls label] res]
                      (concat res (map #(assoc % :status label) ls))
                      ) () (partition 2 args))
    ()
    )
  )

(defn tweets->books [tweets]
  (let [delete-texts (comp delete-hash-tag delete-html-symbol delete-html-tag)
        tweets-without-done (except-dones :text tweets)
        tweets-with-original (map #(assoc % :original_text (delete-texts (:text %))) tweets-without-done)
        converted-tweets (map (fn [t] (assoc t :text (convert-rt-string (:original_text t)))) tweets-with-original)
        ; 各ワードに対して見つかったらそれ以降の文字列を削除する
        ;  →最終的に一番最初にあるワードが残る（それでステータスを判別する）
        tmp-tweets (map (fn [t]
               (assoc t :text
               (fold (fn [x res]
                       (let [i (.indexOf res x)]
                         (if (pos? i) (su2/take res (+ i (count x))) res)
                         )
                       ) (:text t) (apply concat *words-list*))
                      )
               ) converted-tweets)
        [r w f h d] (map (fn [wl] (filter #(has-word? (:text %) wl) tmp-tweets)) *words-list*)
        ; ステータスを付加
        tweets-with-status (set-statuses r "reading" w "want" f "finish" h "have" d "delete")
        tweets-without-words (map #(assoc % :text (delete-words (:text %))) tweets-with-status)
        sorted-tweets (sort #(str< (:created-at %1) (:created-at %2)) tweets-without-words)
        ; もしカンマなどで複数の本がある場合にはここで分けておく
        splitted-tweets (fold (fn [x res]
                                (concat res (map #(assoc x :text %) (extended-split (:text x) *re-book-sep* "\"")))
                                ) () sorted-tweets)
        ]

    (map (fn [t]
           (let [[title author] (string->book-title-author (:text t))]
             (assoc t :title title :author author)
             )
           )
         splitted-tweets
         )
    )
  )

(defnk update-tweets [tweets last-id :max-id -1]
  (let [now-max-id (get-max-id)]
    (loop [save-targets tweets
           local-last-id last-id]
      (cond
        ; 最後まで記録できたらQueryのmax-idを記録
        (empty? save-targets) (when (and (pos? max-id) (> max-id now-max-id))
                                (log/info (str "update max id to " max-id))
                                (update-max-id max-id))
        :else (let [target (first save-targets)]
                (log/info (str "try to save: " (:title target) " (" (:from-user target) "/" (:id target) ")"))
                (if (try (save-book target) (catch Exception e
                                              (log/warn (str "save book fail: " (.getMessage e)))
                                              false))
                  (recur (rest save-targets) (:id target))
                  ; 途中で失敗した場合には次回途中から検索するようにIDを記録
                  (when (and (pos? local-last-id) (> local-last-id now-max-id))
                    (log/info (str "update max id to " local-last-id " (local)"))
                    (update-max-id local-last-id))
                  )
                )
        )
      )
    )
  )

(defn twitter-test [user image text]
  (set-default-timezone)
  (let [name (if (or (nil? user) (su2/blank? user)) "testuser" user)
        icon (if (or (nil? image) (su2/blank? user)) "/img/npc.png" image)
        res {:created-at (now) :from-user name :from-user-id 0 :id 0
                    :iso-language-code "" :profile-image-url icon :source "test"
                    :text text :to-user "testuser2" :to-user-id 1
                    }]
    (update-tweets (tweets->books (list res)) (get-max-id))
    )
  )

(defn save-tweet [id]
  (let [lid (string->long id)]
    (when (< lid (get-max-id))
      (-> (show-twitter-status lid) list tweets->books (update-tweets (get-max-id)))
      )
    )
  )

(defn collect-tweets []
  (let [last-id (get-max-id)
        args (concat (list *yuruyomi-tag*)
                     (if (pos? last-id) (list :since-id last-id) ()))
        res (try (apply twitter-search-all args)
              (catch Exception e
                (log/warn (str "twitter-search-all fail: " (.getMessage e)))
                nil
                ))
        ]
    (when (and (! nil? res) (! empty? (:tweets res)))
      (update-tweets (->> res :tweets tweets->books (sort #(< (:id %1) (:id %2))))
                     last-id :max-id (:max-id res))
      )
    )
  )



