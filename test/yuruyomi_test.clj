(ns yuruyomi-test
  (:use
     simply
     clojure.test
     [yuruyomi.cron twitter]
     [yuruyomi.view html]
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
  (list "aaa : bbb"
        "aaa ： bbb"
        "aaa　: bbb"
        "\"a:a\":bbb"
        "\"a：a\"　:bbb"
        "\"a:a\"：bbb"
        "\":::\" 　:　 \":::\""
        "aaa:bbb:ccc:　ddd"
        "\"a a\":\"b b\""
        )
  )

(def *test-data2*
  (list
    {:created-at "1", :text "aaa 読んでる"}
;    {:created-at "2", :text "aaa　と ccc 読んでる"}
;    {:created-at "3", :text "aaa:bbb と　ccc 読んでる"}
;    {:created-at "4", :text "aaa と ccc ： ddd 読んでる"}
;    {:created-at "5", :text "aaa: bbb と  ccc :ddd 読んでる"}
;    {:created-at "6", :text "aaa と ccc と eee 読んでる"}
;    {:created-at "7", :text "aaa と と eee 読んでる"}
;    {:created-at "7", :text "aaa と　と eee 読んでる"}
;    {:created-at "8", :text "aaa　とと ccc 読んでる"}
;    {:created-at "9", :text "aaa と ccc と と　　eee"}
    )
  )
; }}}

(comment

 18 (def wl (list
 19           (list "a" "b")
 20           (list "1" "2")
 21           )
 22   )
 23
 24 (def sample (list "hello a 2" "world bbb"))
 25
 26 (defn index-of-word [s col]
 27   (let [res (remove #(= % -1) (map #(.indexOf s %) col))]
 28     (if (empty? res) -1 (apply min res))
 29     )
 30   )
 31
 32
 33 (map (fn [s]
 34        (sort (map #(index-of-word s %) wl))
 35        ) sample)


  )


(deftest twitter-convert-ng-test
  (foreach #(println (str "title = [" (:title %) "], author = [" (:author %) "], status = " (:status %)))
           (tweets->books *test-data*))

  (is (every? #(and (! su2/contains? (:title %) "ng")
                    (! su2/contains? (:author %) "ng")
                    ) (tweets->books *test-data*)))
  )

;(deftest twitter-convert-count-test
;;  (foreach #(let [[title author] (string->book-title-author %)]
;;              (println "title = [" title "], author = [" author "]")
;;              ) *title-author-test-data*)
;
;
;  (is (every?  #(let [[title author] (string->book-title-author %)]
;                  (and (= 3 (count title)) (= 3 (count author)))
;                  ) *title-author-test-data*))
;
;  (let [res (tweets->books *test-data2*)]
;    ;(foreach println res)
;    (is (every? #(= 3 (count (:title %))) res))
;    (is (every? #(if (su2/blank? (:author %)) true (= 3 (count (:author %)))) res))
;    (is (= 18 (count res)))
;    )
;  )


;(deftest parts-test
;  (*parts-info* :name "uochan")
;  )



