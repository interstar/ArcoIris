(defproject arcoiris "0.1.0-SNAPSHOT"
  :description "Multi-channel chat server with MCP and web interfaces"
  :url "https://github.com/your-username/arcoiris"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [compojure "1.7.0"]
                 [hiccup "2.0.0-alpha2"]
                 [markdown-clj "1.11.4"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-time "0.15.2"]]
  :main ^:skip-aot arcoiris.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}) 