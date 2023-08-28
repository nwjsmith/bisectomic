(ns com.theinternate.bisectomic.revert
  (:require [datomic.client.api :as d]))

(def schema
  [{:db/ident :stock/symbol
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

(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "bisectomic"}))

(d/create-database client {:db-name "revert"})

(def conn (d/connect client {:db-name "revert"}))

(d/transact conn {:tx-data schema})

(d/transact conn {:tx-data [{:stock/symbol "TSLA"}
                            {:stock/symbol "APPL"}
                            {:stock/symbol "AMD"}]})

(d/transact conn {:tx-data [{:db/id [:stock/symbol "AMD"]
                             :stock/sales [{:sale/price 1.0
                                            :sale/time #inst "2023-08-22T00:00:00.000-00:00"}
                                           {:sale/price 2.0
                                            :sale/time #inst "2023-08-22T00:00:01.000-00:00"}
                                           {:sale/price 3.0
                                            :sale/time #inst "2023-08-22T00:00:02.000-00:00"}]}]})

(d/q '[:find ?t
       :where
       [?s :stock/symbol "AMD"]
       [?s :stock/sales ?sale]
       [?sale :sale/price ?price ?t]
       [(= ?price 2.0)]]
     (d/db conn))

(defn get-transaction
  "Returns the transaction with the given id."
  [conn tx]
  (first (d/tx-range conn {:start tx :limit 1})))

(get-transaction conn 13194139533320)

(defn get-non-transaction-facts
  "Returns the facts of the transaction with the given id that were about
  entities other than the transaction itself."
  [conn tx]
  (when-let [{:keys [data]} (get-transaction conn tx)]
    (filter #(not= tx (:e %)) data)))

(defn get-transaction-revert
  "Returns a transaction that reverts the transaction with the given id."
  [conn tx]
  (map (fn [[e a v _ op]] [(if op :db/retract :db/add) e a v])
       (reverse (get-non-transaction-facts conn tx))))

(d/transact conn {:tx-data (get-transaction-revert conn 13194139533320)})

(d/q '[:find ?price
       :where
       [?sale :sale/price ?price]
       [?s :stock/sales ?sale]]
     (d/db conn))
