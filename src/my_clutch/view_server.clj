(ns my-clutch.view-server
  (:require [cheshire.core :as json]))

(defn view-server-exec-string
  []
  (format "java -cp \"%s\" clojure.main -i @my_clutch.view_server.clj -e \"(my-clutch.view-server/-main)\""
          (System/getProperty "java.class.path")))
