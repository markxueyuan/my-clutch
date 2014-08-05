(ns my-clutch.http-client
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [my-clutch.utils :as utils])
  (:use [cemerick.url :only (url)]
        [slingshot.slingshot :only (throw+ try+)])
  (:import  (java.io IOException InputStream InputStreamReader PushbackReader)
            (java.net URL URLConnection HttpURLConnection MalformedURLException)))

(def ^{:private true} version "0.01")

(def ^{:private true} user-agent (str "my-clutch/" version))

(def ^{:dynamic true} *default-data-type* "application/json")

(def ^{:dynamic true} *configuration-defaults* {:socket-timeout 0
                                                :conn-timeout 5000
                                                :follow-redirects true
                                                :save-request? true
                                                :as :json})

(def ^{:doc "When thread-bound to any value, will be reset!
       to the complete HTTP response of the last couchdb request."
       :dynamic true}
  *response* nil)

(defmacro fail-on-404
  [db expr]
  `(let [f# #(let [resp# ~expr]
               (if (= 404 (:status *response*))
                 (throw (IllegalStateException. (format "Database %s does not exist" ~db)))
                 resp#))]
     (if (thread-bound? #'*response*)
       (f#)
       (binding [*response* nil] (f#)))))

(defn- set!-*response*
  [response]
  (when (thread-bound? #'*response*) (set! *response* response)))

(defn- connect
  [request]
  (let [configuration (merge *configuration-defaults* request)
        data (:data request)]
    (try+
     (let [resp (http/request (merge configuration
                                     {:url (str request)}
                                     (when data {:body data})
                                     (when (instance? InputStream data)
                                       {:length (:data-length request)})))]
       (set!-*response* resp))
     (catch identity ex
       (if (map? ex)
         (do
           (set!-*response* ex)
           (when-not (== 404 (:status ex)) (throw+ ex)))
         (throw+ ex))))))

(defn- configure-request
  [method url {:keys [data data-length content-type headers]}]
  (assoc url
    :method method
    :data (if (map? data) (json/generate-string data) data)
    :data-length data-length
    :headers (merge {"Content-Type" (or content-type *default-data-type*)
                     "User-Agent" user-agent
                     "Accept" "*/*"}
                    headers)))

(defn couchdb-request*
  [method url & {:keys [data data-length content-type headers] :as opts}]
  (connect (configure-request method url opts)))

(defn couchdb-request
  [& args]
  (:body (apply couchdb-request* args)))

(defn lazy-view-seq
  [response-body header?]
  (let [lines (utils/read-lines response-body)
        [lines meta] (if header?
                       [(rest lines)
                        (-> (first lines)
                            (string/replace #",?\"(rows|results)\":\[\s*$" "}")
                            (json/parse-string true))]
                       [lines nil])]
    (with-meta (->> lines
                    (map (fn [^String line]
                           (when (.startsWith line "{")
                             (json/parse-string line true))))
                    (remove nil?))
      (dissoc meta :rows))))

(defn view-request
  [method url & opts]
  (if-let [response (apply couchdb-request method (assoc url :as :stream) opts)]
    (lazy-view-seq response true)
    (throw (java.io.IOException. (str "No such view: " url)))))


































