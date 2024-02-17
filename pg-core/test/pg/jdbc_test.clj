(ns pg.jdbc-test
  (:import
   org.pg.error.PGError
   java.time.LocalDate)
  (:require
   [clojure.test :refer [deftest is use-fixtures testing]]
   [pg.core :as pg]
   [pg.integration :refer [*CONFIG* P15]]
   [pg.oid :as oid]
   [pg.jdbc :as jdbc]
   [pg.pool :as pool]))


(def CONFIG
  (assoc *CONFIG* :port P15))


(deftest test-get-connection-map
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn ["select 1 as one"])]

      (is (pg/connection? conn))
      (is (= [{:one 1}] res)))))


(deftest test-get-connection-pool
  (with-open [pool (pool/pool CONFIG)
              conn (jdbc/get-connection pool)]
    (let [res
          (jdbc/execute! conn ["select 1 as one"])]
    (is (pg/connection? conn))
    (is (= [{:one 1}] res)))))


(deftest test-get-connection-error
  (try
    (jdbc/get-connection nil)
    (is false)
    (catch Throwable e
      (is true)
      (is (= "Connection source cannot be null"
             (ex-message e))))))


(deftest test-execute!-conn
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn ["select $1::int4 as num" 42])]
      (is (= [{:num 42}] res)))))


(deftest test-execute!-conn-opt
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute! conn
                         ["select $1::int4 as num, $2::bool" 42 true]
                         {:matrix? true})]
      (is (= [[:num :bool] [42 true]] res))
      (pg/close conn))))


(deftest test-execute!-pool-opt
  (pool/with-pool [pool CONFIG]
    (let [res
          (jdbc/execute! pool
                         ["select $1::int4 as num, $2::bool" 42 true]
                         {:matrix? true})]
      (is (= [[:num :bool] [42 true]] res)))))


(deftest test-prepare-conn
  (with-open [conn (jdbc/get-connection CONFIG)]

    (let [stmt
          (jdbc/prepare conn
                        ["select $1 as num, $2 as bool" 42 true]
                        {:oids [oid/int4]})

          res
          (jdbc/execute! conn
                         [stmt 123 false]
                         {:matrix? true})]

      (is (pg/prepared-statement? stmt))
      (is (= [[:num :bool] [123 false]] res)))))


(deftest test-prepare-pool
  (with-open [pool (pool/pool CONFIG)
              conn (jdbc/get-connection pool)]

    (let [stmt
          (jdbc/prepare conn
                        ["select $1 as num, $2 as bool" 42 true]
                        {:oids [oid/int4]})

          res
          (jdbc/execute! conn
                         [stmt 123 false]
                         {:matrix? true})]

      (is (pg/prepared-statement? stmt))
      (is (= [[:num :bool] [123 false]] res)))))


(deftest test-execute-one!-conn
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [res
          (jdbc/execute-one! conn
                             ["select $1 as foo_bar" 42]
                             {:kebab-keys? true})]
      (is (= {:foo-bar 42} res)))))


(deftest test-execute-one!-pool
  (with-open [pool (pool/pool CONFIG)
              conn (jdbc/get-connection pool)]
    (let [res
          (jdbc/execute-one! conn
                             ["select $1 as foo_bar" true]
                             {:kebab-keys? true})]
      (is (= {:foo-bar true} res)))))


(deftest test-execute-one!-conn-stmt
  (with-open [conn (jdbc/get-connection CONFIG)]
    (let [date
          (LocalDate/parse "2024-01-30")

          stmt
          (jdbc/prepare conn
                        ["select $1 as is_date, $2 as is_num"]
                        {:oids [oid/date oid/int4]
                         :kebab-keys? true})

          res
          (jdbc/execute-one! conn
                             [stmt date 123]
                             {:kebab-keys? true})]

      (is (= {:is-num 123
              :is-date date} res)))))


(deftest test-execute-batch!
  (with-open [conn (jdbc/get-connection CONFIG)]
    (try
      (jdbc/execute-batch! conn "select 1" [])
      (is false)
      (catch PGError e
        (is true)
        (is (= "execute-batch! is not imiplemented"
               (ex-message e)))))))


(deftest test-on-connection-conn
  (jdbc/on-connection [conn CONFIG]
    (is (pg/connection? conn))
    (is (= [{:num 1}]
           (pg/query conn "select 1 as num")))))


(deftest test-on-connection-pool
  (pool/with-pool [pool CONFIG]
    (jdbc/on-connection [conn pool]
      (is (pg/connection? conn))
      (is (= [{:num 1}]
             (pg/query conn "select 1 as num"))))))


(deftest test-transact-config
  (let [func
        (fn [conn]
          (jdbc/execute! conn
                         ["select $1 as one" 42]))

        res
        (jdbc/transact CONFIG
                       func
                       {:isolation :serializable
                        :read-only true
                        :rollback-only true})]

    (is (= [{:one 42}] res))))


(deftest test-transact-pool
  (with-open [pool (pool/pool CONFIG)]

    (let [func
          (fn [conn]
            (jdbc/execute! conn
                           ["select $1 as one" 42]))

          res
          (jdbc/transact pool
                         func
                         {:isolation :serializable
                          :read-only true
                          :rollback-only true})]

      (is (= [{:one 42}] res)))))


(deftest test-with-transaction-config
  (let [opts
        {:isolation :serializable
         :read-only true
         :rollback-only true}]
    (jdbc/with-transaction [conn CONFIG opts]
      (let [res
            (jdbc/execute! conn
                           ["select $1 as one" 42])]
        (is (= [{:one 42}] res))))))


(deftest test-with-transaction-conn
  (let [opts
        {:isolation :serializable
         :read-only true
         :rollback-only true}]
    (pg/with-connection [foo CONFIG]
      (jdbc/with-transaction [TX foo opts]
        (let [res
              (jdbc/execute! TX
                             ["select $1 as one" 42])]
          (is (= [{:one 42}] res)))))))


(deftest test-with-transaction-pool
  (let [opts
        {:isolation :serializable
         :read-only true
         :rollback-only true}]
    (pool/with-pool [foo CONFIG]
      (jdbc/with-transaction [TX foo opts]
        (let [tx?
              (jdbc/active-tx? TX)

              res
              (jdbc/execute! TX
                             ["select $1 as one" 42])]

          (is tx?)
          (is (= [{:one 42}] res)))))))
