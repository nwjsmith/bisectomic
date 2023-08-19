(ns com.theinternate.bisectomic
  (:require [datomic.client.api :as d]))

(def client
  (d/client {:server-type :dev-local :storage-dir :mem :system "dev"}))

(def schema
  [{:db/ident :security/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/doc "The unique identifier of the security."}])

(defn set-up
  [client]
  (let [arg-map {:db-name "bisectomic"}]
    (d/create-database client arg-map)
    (d/transact (d/connect client arg-map) {:tx-data schema})))
