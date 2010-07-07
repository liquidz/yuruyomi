(defproject
  yuruyomi "0.0.1"
  :description "lazy management tool for reading books"
  :repositories {"gaejtools" "http://gaejtools.sourceforge.jp/maven/repository",
                 "maven.seasar.org" "http://maven.seasar.org/maven2"}

  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [compojure "0.4.0-SNAPSHOT"]
                 [am.ik/clj-gae-ds "0.2.0-SNAPSHOT"]
                 [am.ik/clj-gae-users "0.1.0-SNAPSHOT"]
                 [am.ik/clj-aws-ecs "0.1.0-SNAPSHOT"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.3.5"]
                 [ring/ring-core "0.2.0"]
                 [ring/ring-servlet "0.2.0"]
                 [ring/ring-jetty-adapter "0.2.0"]
                 [hiccup/hiccup "0.2.3"]
                 [org.clojars.liquidz/simply "0.1.9"]
                 [org.clojars.liquidz/twitter "0.1.1"]
                 ]

  :dev-dependencies [[am.ik/clj-gae-testing "0.1.0"]]
  :compile-path "war/WEB-INF/classes/"
  :library-path "war/WEB-INF/lib/"
  :namespaces [yuruyomi]
  )
