(ns com.theinternate.bisectomic.bisect
  (:require [datomic.client.api :as d])
  (:import (java.util Date)
           (java.time Instant)))

(def schema
  [{:db/ident :sale/time
    :db/doc "The time of the sale."
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}])

(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system "bisectomic"}))

(d/create-database client {:db-name "bisect"})
(def conn (d/connect client {:db-name "bisect"}))

(d/transact conn {:tx-data schema})

(defn every-second
  "Returns an infinite sequence of dates, one second apart, from start or
  1970-01-01T00:00:00Z."
  ([] (every-second (Date/from Instant/EPOCH)))
  ([start]
   (->> (iterate #(.plusSeconds % 1) (.toInstant start))
        (map #(Date/from %)))))

(doseq [t (take 100000 (every-second))]
  (let [tx-data [{:sale/time t}]]
    (d/transact conn {:tx-data tx-data})))

(defn- min-tx
  "Returns the id of the first transaction in the database."
  [db]
  (ffirst (d/q '[:find (min ?tx) :where [?tx :db/txInstant _]] db)))

(defn- max-tx
  "Returns the id of the most recent transaction in the database."
  [db]
  (ffirst (d/q '[:find (max ?tx) :where [?tx :db/txInstant _]] db)))

(defn- median-tx
  "Returns the id of the median transaction between two databases."
  [min-db max-db]
  (ffirst (d/q '[:find (median ?tx)
                 :in $ ?min-tx
                 :where
                 [_ :db/txInstant _ ?tx]
                 [(<= ?min-tx ?tx)]]
               max-db
               (max-tx min-db))))

(defn- median-db
  "Returns the database at the median transaction time between two databases."
  [db other-db]
  (let [[min-db max-db] (sort-by max-tx [db other-db])]
    (d/as-of max-db (median-tx min-db max-db))))

(defn bisect
  "Bisects the database to find the first transaction that satisfies a predicate
  taking the database as an argument."
  [good-db bad-db pred]
  (let [mid (median-db good-db bad-db)]
    (if (= good-db mid)
      good-db
      (if (pred mid)
        (bisect mid bad-db pred)
        (bisect good-db mid pred)))))


;; 21b98c9
