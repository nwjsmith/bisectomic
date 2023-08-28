(ns com.theinternate.bisectomic.source
  (:require [datomic.client.api :as d]))

(def schema
  [{:db/ident :data/source
    :db/doc "The source of this transaction's data."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}

   {:db/ident :application/version
    :db/doc "The source of this transaction's data."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}

   {:db/ident :stock/symbol
    :db/doc "The stock's symbol."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :stock/sales
    :db/doc "Sales of the stock."
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident :sale/price
    :db/doc "The price of the sale in CAD."
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one}
   {:db/ident :sale/time
    :db/doc "The time of the sale."
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}])

(def tx-symbols
  [{:stock/symbol "TSLA"}
   {:stock/symbol "APPL"}
   {:stock/symbol "AMD"}])

(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "bisectomic"}))

(d/create-database client {:db-name "source"})

(def conn (d/connect client {:db-name "source"}))

(d/transact conn {:tx-data schema})
(d/transact conn {:tx-data tx-symbols})

(def tx-sales
  [[:db/add "datomic.tx" :data/source "some_awful_file.1692739593.xml"]
   [:db/add "datomic.tx" :application/version "deadbeef"]
   {:db/id [:stock/symbol "TSLA"]
    :stock/sales [{:sale/price 1.0
                   :sale/time #inst "2023-08-22T00:00:00.000-00:00"}
                  {:sale/price 2.0
                   :sale/time #inst "2023-08-22T00:00:01.000-00:00"}
                  {:sale/price 3.0
                   :sale/time #inst "2023-08-22T00:00:02.000-00:00"}]}])

(d/transact conn {:tx-data tx-sales})

(d/q {:find '(pull )
      :where '[[(= ?price 2.0)]
               [?sale :sale/price ?price ?t]
               [?t :data/source ?source]
               [?t :application/version ?version]]}
     (d/db conn))
