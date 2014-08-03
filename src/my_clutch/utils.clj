(ns my-clutch.utils
  (:require [clojure.java.io :as io]
            [cemerick.url :as url])
  (:import java.net.URLEncoder
           java.lang.Class
           [java.io File]))

(defn url
  [& [base & parts :as args]]
  (try
    (apply url/url base (map (comp url/url-encode str) parts))
    (catch java.net.MalformedURLException e
      (apply url/url "http://127.0.0.1:5984" (map (comp url/url-encode str) args)))))


(defn server-url
  [db]
  (assoc db :path nil :query nil))

(defn get-mime-type
  [^File file]
  (java.net.URLConnection/guessContentTypeFromName (.getName file)))

(defn read-lines
  [f]
  (let [func (fn this [^java.io.BufferedReader rdr]
               (lazy-seq
                (if-let [line (.readLine rdr)]
                  (cons line (this rdr))
                  (.close rdr))))]
    (func (io/reader f))))

