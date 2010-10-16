(defproject
  yuruyomi "0.0.1"
  :description "lazy management tool for reading books"
  :repositories {"gaejtools" "http://gaejtools.sourceforge.jp/maven/repository",
                 "maven.seasar.org" "http://maven.seasar.org/maven2"}

  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.5.2"]
                 ;[am.ik/clj-gae-ds "0.2.1"]
                 [am.ik/clj-gae-ds "0.3.0-SNAPSHOT"]
                 [am.ik/clj-gae-users "0.1.1"]
                 [am.ik/clj-aws-ecs "0.1.0-SNAPSHOT"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.3.8"]
                 [ring/ring-core "0.3.1"]
                 [ring/ring-servlet "0.3.1"]
                 [ring/ring-jetty-adapter "0.3.1"]
                 [hiccup/hiccup "0.3.0"]
                 [org.clojars.liquidz/simply "0.2.1"]
                 [org.clojars.liquidz/twitter "0.2.1"]
                 ]

  :dev-dependencies [[am.ik/clj-gae-testing "0.1.0"]]
  :compile-path "war/WEB-INF/classes/"
  :library-path "war/WEB-INF/lib/"
  :namespaces [yuruyomi]
  )
