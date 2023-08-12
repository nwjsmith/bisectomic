(ns com.theinternate.bisectomic
  (:require [datomic.client.api :as d]))

(def client (d/client {:server-type :dev-local :storage-dir :mem :system "dev"}))

(def conn (d/connect client {:db-name "bisectomic"}))
