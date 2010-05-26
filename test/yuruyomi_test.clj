(ns yuruyomi-test
  (:use
     simply
     clojure.test
     [yuruyomi collect-twitter]
     )
  (:require
     [clojure.contrib.str-utils2 :as su2]
     )
  )

; =data {{{
(def *test-data*
  (list {:created-at "01", :text "ok ほしい #yuruyomi_test"}
        {:created-at "02", :text "ng かった #yuruyomi_test #done"}
        {:created-at "03", :text "ng かった #done #yuruyomi_test"}
        {:created-at "04", :text "ng neko"}
        {:created-at "05", :text "ng"}
        {:created-at "06", :text ""}
        {:created-at "07", :text "ok:aaa かった"}
        {:created-at "08", :text "ng:bbb あああ"}
        {:created-at "09", :text "ok：ok ほしい ng"}
        {:created-at "10", :text "ok 222 よんでる #ng"}
        {:created-at "11", :text "読了 RT @ng: ok 読んでる ng #ng"}
        {:created-at "12", :text "読了 RT @ng: ok 買った #ng #done"}
        {:created-at "13", :text "買った RT @ng123: ok：ook 読んでる ng"}
        {:created-at "14", :text "買った RT @ng_123: ok,ok 読んでる ng"}
        {:created-at "15", :text "\"<ng>ok</ng>\" もってる"}
        {:created-at "16", :text "\"ok:ok もってる ng"}
        {:created-at "17", :text "ok/\"ok もってる ng"}
        {:created-at "18", :text "ok/hello\"ok もってる ng"}
        {:created-at "19", :text "&#60;ok&#62; もってる ng"}
        )
  )

(def *title-author-test-data*
  (list "aaa : bbb" "aaa ： bbb"
        "\"a:a\":bbb" "\"a：a\":bbb" "\"a:a\"：bbb"
        "\":::\":\":::\"" "aaa:bbb:ccc:ddd"
        "\"a a\":\"b b\""
        )
  )
; }}}

(deftest twitter-convert-test
;  (foreach #(println (str "title = [" (:title %) "], author = [" (:author %) "], status = " (:status %)))
;           (tweets->books *test-data*))

  (is (every? #(and (! su2/contains? (:title %) "ng")
                    (! su2/contains? (:author %) "ng")
                    ) (tweets->books *test-data*)))

;  (foreach #(let [[title author] (string->book-title-author %)]
;              (println "title = [" title "], author = [" author "]")
;              ) *title-author-test-data*)


  (is (every?  #(let [[title author] (string->book-title-author %)]
                  (and (= 3 (count title)) (= 3 (count author)))
                  ) *title-author-test-data*))
  )


