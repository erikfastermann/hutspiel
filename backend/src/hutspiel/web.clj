(ns hutspiel.web
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.request :as ring-request]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]))

(defn pr-edn-str
  "Adapted from https://github.com/jafingerhut/jafingerhut.github.com/blob/master/clojure-info/using-edn-safely.md"
  [& xs]
  (binding [*print-length* false
            *print-dup* false
            *print-level* false
            *print-readably* true]
    (apply pr-str xs)))

(defn call-route [route-fn parsed-body]
  (try (let [result (route-fn parsed-body)]
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body (pr-edn-str result)})
       (catch Exception e (do (log/error e)
                              {:status 500
                               :headers {"Content-Type" "text/plain"}
                               :body "Internal error"}))))

(defn parse-and-call-route [route-fn request]
  (try (->> (ring-request/body-string request)
            (edn/read-string)
            (call-route route-fn))
       (catch Exception _ {:status 400
                           :headers {"Content-Type" "text/plain"}
                           :body "error parsing request body"})))

(defn handler [routes request] 
  (let [request-uri (:uri request)
        route-fn (get routes request-uri)]
    (if (nil? route-fn)
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body (str "unknown route " request-uri)}
      (parse-and-call-route (var-get route-fn) request))))

; TODO: timeout
(def run-jetty-options
  {:port 8080
; :ssl? true
; :ssl-port 8443
; TODO :exclude-ciphers :exclude-protocols :min-threads :max-threads
; :keystore file
   :send-server-version? false
; TODO: probably needed in prod version
   :join? false})

(defn foo [_data] (throw (Exception. "foo")))
(defn bar [_data] 42)

(def routes
  {"/foo" #'foo
   "/bar" #'bar})

(defn jetty-handler [request] (handler routes request))

(defonce server (jetty/run-jetty jetty-handler run-jetty-options))
; (.stop server)
