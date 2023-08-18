(ns com.theinternate.bisectomic
  (:require [datomic.client.api :as d]))

(def client
  (d/client {:server-type :dev-local :storage-dir :mem :system "dev"}))

(defn set-up
  [client]
  (let [arg-map {:db-name "bisectomic"}]
    (d/create-database client arg-map)
    (d/connect client arg-map)))

(comment
  (set-up client))
