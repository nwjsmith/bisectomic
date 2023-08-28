(ns com.theinternate.bisectomic.bug
  (:require [datomic.client.api :as d]))

(def schema
  [{:db/ident :bug/id
    :db/doc "The bug's JIRA ID."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :bug/tx
    :db/doc "The transaction that introduced the bug."
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :bug/fixed?
    :db/doc "Is it fixed?"
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

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

(def tx-data
  [{:stock/symbol "ABC"
    :stock/sales [{:sale/price 1.0
                   :sale/time #inst "2023-08-22T00:00:00.000-00:00"}
                  {:sale/price 2.0
                   :sale/time #inst "2023-08-22T00:00:01.000-00:00"}
                  {:sale/price 3.0
                   :sale/time #inst "2023-08-22T00:00:02.000-00:00"}]}])

(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "bisectomic"}))

(d/create-database client {:db-name "bug-report"})
(def conn (d/connect client {:db-name "bug-report"}))

(d/transact conn {:tx-data schema})
(d/transact conn {:tx-data tx-data})
(d/transact conn {:tx-data [{:sale/price 12.0}]})

(d/q '[:find ?t
       :where
       [_ :sale/price ?price ?t]
       [(= ?price 12.0)]]
     (d/db conn))

(d/transact conn {:tx-data [{:bug/id "JIRA-134" :bug/tx 13194139533320}]})

(d/q '[:find ?id ?time ?fixed
       :where
       [?t :db/txInstant ?time]
       [_ :bug/tx ?t]
       [_ :bug/fixed? ?fixed]
       [_ :bug/id ?id]]
     (d/db conn))

(d/transact conn {:tx-data [[:db/add [:bug/id "JIRA-134"] :bug/fixed? true]]})
