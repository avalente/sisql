(ns sisql.grammar-test
  (:require [clojure.java.io :as io]
            [clojure.string :as st]
            [instaparse.core :as ip]
            [sisql.parser])
  (:use [clojure.test]))

(def parser (ip/parser (io/resource "sisql/grammar.bnf")))

(defn- parse [text sym]
  (ip/transform sisql.parser/transforms (ip/parse parser text :start sym)))

(deftest test-grammar-fragments
         (is (= false (parse "false" :BOOLEAN)))
         (is (= true (parse "true" :BOOLEAN)))
         (is (ip/failure? (parse "maybe" :BOOLEAN)))
         (is (ip/failure? (parse "123" :BOOLEAN)))

         (is (= 0 (parse "0" :INTEGER)))
         (is (= 123 (parse "123" :INTEGER)))
         (is (ip/failure? (parse "" :INTEGER)))
         (is (ip/failure? (parse "xxx" :INTEGER)))

         (is (= :abc (parse ":abc" :KEYWORD)))
         (is (= :a1X*+!-_? (parse ":a1X*+!-_?" :KEYWORD)))
         (is (ip/failure? (parse ":123" :KEYWORD)))
         (is (ip/failure? (parse ":a.b" :KEYWORD)))
         (is (ip/failure? (parse ":a/b" :KEYWORD)))

         (is (= "xxx" (parse "\"xxx\"" :QUOTED-STRING)))
         (is (= "x\"xx" (parse "\"x\\\"xx\"" :QUOTED-STRING)))
         (is (= "x\nxx" (parse "\"x\nxx\"" :QUOTED-STRING)))
         (is (= "x\n\nxx" (parse "\"x\n\nxx\"" :QUOTED-STRING)))
         (is (= "x\nxx" (parse "\"x\\\nxx\"" :QUOTED-STRING)))
         (is (ip/failure? (parse "\"xxx" :QUOTED-STRING)))
         (is (ip/failure? (parse "xxx\"" :QUOTED-STRING)))
         (is (ip/failure? (parse "\"x\"xx\"" :QUOTED-STRING)))

         (is (= [:key "value"] (parse "--@key:\"value\"\n" :ATTRIBUTE)))
         (is (= [:key :value] (parse "--@key::value\n" :ATTRIBUTE)))
         (is (= [:key 123] (parse "--@key:123\n" :ATTRIBUTE)))
         (is (= [:key true] (parse "--@key:true\n" :ATTRIBUTE)))
         (is (= [:key false] (parse "--@key:false\n" :ATTRIBUTE)))
         (is (= [:key "value"] (parse "-- @key:\"value\"\n" :ATTRIBUTE)))
         (is (= [:key "value"] (parse "--@key: \"value\"\n" :ATTRIBUTE)))
         (is (= [:key "value"] (parse "--   \t@key:\t \"value\"\n" :ATTRIBUTE)))
         (is (= [:key " \tvalue "] (parse "--   \t@key:\t \" \tvalue \"\n" :ATTRIBUTE)))
         (is (= [:key "val\n\"ue"] (parse "--@key:\"val\\\n\\\"ue\"\n" :ATTRIBUTE)))
         (is (ip/failure? (parse "--@key \"value\"\n" :ATTRIBUTE)))
         (is (ip/failure? (parse "--@key:\"value\"" :ATTRIBUTE)))
         (is (ip/failure? (parse "-@key:\"value\"" :ATTRIBUTE)))
         (is (ip/failure? (parse "--key:\"value\"" :ATTRIBUTE)))
         (is (ip/failure? (parse "--@key:value\"" :ATTRIBUTE)))
         (is (ip/failure? (parse "--@key:value" :ATTRIBUTE)))
         (is (ip/failure? (parse "--@k/ey:\"value\"" :ATTRIBUTE)))

         (is (= :value (parse "--@name:value\n" :NAME)))
         (is (= :value (parse "--@name::value\n" :NAME)))
         (is (= :value (parse "-- \t@name::value\n" :NAME)))
         (is (= :value (parse "-- \t@name: value  \n" :NAME)))
         (is (ip/failure? (parse "--@name value\n" :NAME)))
         (is (ip/failure? (parse "--@name:value" :NAME)))
         (is (ip/failure? (parse "-@name:value" :NAME)))

         (is (= "SELECT\n*\nFROM\ntable" (parse "SELECT\n*\n\tFROM\n\n  table\n" :BODY)))
         (is (= "SELECT\n*\nFROM\ntable" (parse "SELECT\n  --fields\n\n*\n\tFROM\n\n  table\n" :BODY)))
         (is (= "SELECT\n*\nFROM\ntable --table name" (parse "SELECT\n  --fields\n\n*\n\tFROM\n\n  table --table name\n" :BODY)))

         (is (= [:COMMENT "this is a comment"] (parse "-- this is a comment\n" :COMMENT)))
         (is (= [:COMMENT "this is a comment"] (parse "--this is a comment\n" :COMMENT)))
         (is (= [:COMMENT "this is a comment"] (parse " --this is a comment\n" :COMMENT)))
         (is (ip/failure? (parse "-- this is a \ncomment\n" :COMMENT)))
         (is (ip/failure? (parse "--@name:puppa\n" :COMMENT)))
         (is (ip/failure? (parse "-- @name:puppa\n" :COMMENT)))
         )
