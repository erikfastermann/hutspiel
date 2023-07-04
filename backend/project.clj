(defproject hutspiel "0.1.0-SNAPSHOT"
  :description "A text-based multiplayer guessing game for the web"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.9.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.slf4j/slf4j-api "2.0.7"]
                 ; TODO: use another logging provider
                 [org.slf4j/slf4j-simple "2.0.7"]]
  :main ^:skip-aot hutspiel.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]}})
