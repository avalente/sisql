(ns sisql.parser
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [clojure.edn :as edn]
            [instaparse.core :as instaparse]))

(defrecord Query [name metadata body])

(def transforms
  {:WHITESPACE str
   :LINE str
   :TEXT str
   :BODY (fn [& lines] (st/join "\n" lines))
   :QUOTED-STRING str
   :BOOLEAN #(= % "true")
   :INTEGER #(Integer/parseInt %)
   :KEYWORD keyword
   :KEYWORD-DATA str
   :KEYWORD-SYMBOL str
   :ESCAPED-CHAR edn/read-string
   :QUERY #(Query. %1 %2 %3)
   :ATTRIBUTE (fn [ [_ k] [_ v] ] [(keyword k) v])
   :METADATA (fn [& args] (into {} args))
   :NAME identity
   :NAME-VALUE keyword
   :QUERIES vector})

(def parser (instaparse/parser (io/resource "sisql/grammar.bnf")))

(defn check-duplicates [queries]
  (loop [tail queries names {}]
    (if (empty? tail)
      queries
      (let [qry (first tail)
            nm (.name qry)
            line (:instaparse.gll/start-line (meta qry))]
        (if (contains? names nm)
          (throw (ex-info (format "Line %s: query with name \"%s\" already defined on line %s" line nm (names nm))
                          {:type :parser-error :kind :duplicated
                           :line line :original-line (names nm)})))
        (recur (rest tail) (assoc names nm line))))))

(defn- do-parse-text [text]
  (let [fixed (str text "\n")
        parsed (instaparse/parse parser fixed :start :QUERIES)]
    (if (instaparse/failure? parsed)
      (throw (ex-info (with-out-str (instaparse.failure/pprint-failure (instaparse/get-failure parsed))) {:type :parser-error}))
      (let [enriched (instaparse/add-line-and-column-info-to-metadata fixed parsed)]
      (check-duplicates (instaparse/transform transforms enriched))))))

(defn- to-map [queries]
  (into {} (for [q queries] [(.name q) q])))

; TODO: probably an incremental parsing would be better in case of large files,
; it should be easy to implement it with the instaparse :partial feature
(defn parse-text [text]
  (to-map (check-duplicates (do-parse-text text))))

(defn parse-file [filename]
  (parse-text (slurp filename)))
