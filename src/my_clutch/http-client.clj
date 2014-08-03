(ns my-clutch.http-client
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [my-clutch.utils :as utils])
  (:use [cemerick.url :only (url)])
  (:import  (java.io IOException InputStream InputStreamReader PushbackReader)
            (java.net URL URLConnection HttpURLConnection MalformedURLException)))

(json/generate-stream {:foo "bar" :baz 5} (clojure.java.io/writer "D:data/tryjson"))

(json/generate-string {:foo "bar" :baz 5} {:pretty true})
