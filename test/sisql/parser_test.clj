(ns sisql.parser-test
  (:require [clojure.java.io :as io]
            [clojure.data]
            [clojure.set]
            [clojure.string :as st]
            [instaparse.core :as instaparse]
            [sisql.parser :as mm])
  (:use [clojure.test]))

(def ^:dynamic valid nil)

(defn load-valid-file [f]
  (def valid (mm/parse-file (io/resource "sisql/sql/valid.sql")))
  (f)
  (def valid nil))

(use-fixtures :once load-valid-file)

(defn report-metadata-diff [value expected]
  (let [[x y _] (clojure.data/diff value expected)
        missing (for [[k _] expected :when (not (contains? value k))] k)
        extra (for [[k _] value :when (not (contains? expected k))] k)
        diff (for [[k v] expected :when (and (contains? value k) (not= (value k) v))] (format "<%s>: %s != %s" k (value k) v))
        msgs [(if (not (empty? missing))
                (format "missing keys: %s" (st/join ", " missing))
                nil)
              (if (not (empty? extra))
                (format "extra keys: %s" (st/join ", " extra))
                nil)
              (st/join ", " diff)]]
    (str "metadata differ: " (st/join "; " (remove nil? msgs)))))

(defmethod assert-expr 'query [msg form]
  `(let [nm# ~(nth form 1)
         md# ~(nth form 2)
         bd# ~(nth form 3)
         qr# (valid nm#)
         ex# (sisql.parser.Query. nm# md# bd#)
         re# {:type :fail :message ~msg :expected ex#}]
     (cond
       (nil? qr#) (do-report (assoc re# :actual (format "key %s not found" nm#)))
       (not= nm# (.name qr#)) (do-report (assoc re#
                                                :expected (format "name=%s" nm#)
                                                :actual (.name qr#)))
       (not= md# (.metadata qr#)) (do-report (assoc re#
                                                    :expected (report-metadata-diff (.metadata qr#) md#)
                                                    :actual (.metadata qr#)))
       (not= bd# (.body qr#)) (do-report (assoc re#
                                                :expected (format "body=%s" bd#)
                                                :actual (.body qr#)))
       true (do-report {:type :pass :message ~msg
                        :expected ex# :actual ex#}))
     nil))

(defmethod assert-expr 'parse-thrown? [msg form]
  (let [opts (nth form 1)
        body (nthnext form 2)]
     `(try ~@body
       (do-report {:type :fail :message ~msg
                   :expected (format "clojure.lang.ExceptionInfo with %s" ~opts)
                   :actual nil})
       (catch clojure.lang.ExceptionInfo e#
         (let [data# (ex-data e#)
               diff# (for [[k# v#] ~opts :when (and
                                              (contains? data# k#)
                                              (not= (data# k#) v#))]
                      (format "<%s>: %s != %s" k# (data# k#) v#))]
           (if (not (empty? diff#))
             (do-report {:type :fail :message ~msg
                         :expected (format "clojure.lang.ExceptionInfo with %s" ~opts)
                         :actual (st/join ", " diff#)})
             (do-report {:type :pass :message ~msg
                         :expected ~opts :actual ~opts})))))))

(deftest parse-valid
         (is (query :simple1 {} "SELECT * FROM test"))
         (is (query :simple2 {} "SELECT * FROM test;"))
         (is (query :simple3 {} "SELECT * FROM test"))
         (is (query :simple4 {} "SELECT *\nFROM test"))
         (is (query :simple5 {} "SELECT\n*\nFROM\ntest"))
         (is (query :simple6 {} "SELECT *\nFROM test"))
         (is (query :with-comments {} "SELECT *\nFROM test\nWHERE key=?"))
         (is (query :with-metadata {:doc "Query docstring"
                                    :version 12
                                    :is-a-select? true
                                    :tag :some-tag
                                    :long-doc "This is a \"test\" query"}
                    "SELECT true;"))
         (is (query :with-multiline-metadata {:long-doc "This is a \nmulti\nline\ncomment"}
                    "SELECT true"))
         (is (query :with-invalid-metadata-1 {} "SELECT true"))
         (is (query :with-invalid-metadata-2 {} "SELECT true"))
         (is (query :bad {} "SELECT 1\nSELECT 2")))

(deftest parse-invalid
         (is (= {} (mm/parse-text "")))
         (is (parse-thrown? {:type :parser-error} (mm/parse-text "SELECT true")))
         (is (parse-thrown? {:type :parser-error :kind :duplicated :line 4 :original-line 2}
                            (mm/parse-text "\n--@name:test\nSELECT 1\n--@name:test\nSELECT 2"))))

(deftest check-ambiguity
         (is (= 1 (count
                    (take 100 (instaparse/parses mm/parser (slurp (io/resource "sisql/sql/valid.sql"))))))))
