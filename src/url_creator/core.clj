(ns url-creator.core
  (:gen-class)
  (:require
    [clojure.string :as str]
    )
  (:import (java.util Date)
           (java.net URLEncoder)
           (java.text SimpleDateFormat)))

;clojure.edn/read-string  "#{\"aa\"}" -> #{"aa"}
;clojure.core/prn-str #{"aa"} -> "#{\"aa\"}"

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn date->string
  ([^Date date]
   (date->string date "yyyy")
    )
  ([^Date date ^String fmt]
   (. (SimpleDateFormat. fmt) format date)
    )
  )

(defn collection? [val] (and (coll? val) (not (map? val))))


(defn- __map [input-data vec-data]
  (let [map-data (apply hash-map vec-data)
        input (cond
                (map? input-data) nil
                (collection? input-data) input-data
                :else [input-data]
                )
        ]
    (map #(get map-data % %) input)
    )
  )

(defn- __str_date [^java.util.Date input-data vec-format-data]
  (date->string input-data (first vec-format-data))
  )

;condition will stop loop
(defn- __loop [input-data [field condition compare-value target]]
  (let [value (condp = compare-value
                "null" nil
                compare-value
                )
        compare-function (condp = condition
                    "!=" #(not (= value %))
                    "==" #(= value %)
                    )
        result (loop [current-record input-data
                      result []]
                 (let [current-result (conj result (get current-record target))]
                   (if (compare-function (get current-record field))
                     current-result
                     (recur (get current-record field) current-result)
                     )
                   )
                 )
        ]
    (str/join "/" result)
    )
  )

(defn- __urlencode [input-data]
  (when input-data (URLEncoder/encode input-data "UTF-8"))
  )

(defn- __first [input-data]
  (if (collection? input-data)
    (first input-data)
    input-data
    )
  )

(defn- convert-to-multiple-urls [path-segments]
  (let [coll-seg (flatten (filter #(collection? %) path-segments))]
    (if (empty? coll-seg)
      path-segments
      (map (fn [new-seg]
             (map (fn [ori-seg]
                    (if (collection? ori-seg)
                      new-seg
                      ori-seg
                      )
                    ) path-segments)
             ) coll-seg)
      )
    )
  )

(defn- fill-field-data
  ([record str-keyword params]
   (cond
     (and (collection? str-keyword) (= 2 (count str-keyword)))
     (fill-field-data record (first str-keyword) (last str-keyword))
     (keyword? str-keyword)
     (let [key-list (str/split (name str-keyword) #"->")]
       (reduce (fn [input key-word]
                 (cond
                   (= key-word "__map") (__map input params)
                   (= key-word "__str_date") (__str_date input params)
                   (= key-word "__first") (if (collection? input) (first input) input)
                   (= key-word "__loop") (__loop input params)
                   (= key-word "__urlencode") (URLEncoder/encode input "utf-8")
                   :else
                   (get input (keyword key-word) key-word)
                   )
                 ) record key-list)
       )
     (string? str-keyword)
     (let [segs (str/split str-keyword #"\.")
           _ (prn "#### segs=" segs)
           result (map (fn [seg]
                         (cond
                           (str/starts-with? seg ":") (fill-field-data record (keyword (str/replace-first seg ":" "")))
                           (str/includes? seg "@") (str/replace (first (fill-field-data record (str/replace seg "@" "/"))) "/" "")
                           (collection? (clojure.edn/read-string seg)) (fill-field-data record (clojure.edn/read-string seg))
                           :else seg
                           )
                         ) (str/split (first segs) #"/" -1))
           result (convert-to-multiple-urls result)
           ]
       (let [new-path (if (string? (first result))
                        [(str/join "/" result)]
                        (map #(str/join "/" %) result)
                        )
             ]
         (if (> (count segs) 1)
           (map #(str % "." (str/join "." (next segs))) new-path)
           new-path)
         )
       )
     :else nil
     )
    )
  ([record str-keyword]
   (fill-field-data record str-keyword nil)
    )
  )

(defn- is-condition-true [record array-condition]
  (let [[h m t] array-condition
        cond-fn #(cond
                   (keyword? %) (fill-field-data record %)
                   (and (collection? %) (not (set? %))) (is-condition-true record %)
                   (set? %) (into [] %)
                   :else %
                   )
        cond_1 (cond-fn h)
        cond_2 (cond-fn t)
        ]
    (prn "compare result= ###" cond_1 m cond_2)
    (cond
      (= m "!=") (not= cond_1 cond_2)
      (= m "==") (= cond_1 cond_2)
      (= m "in") (some #(= cond_1 %) cond_2)
      (= m "&&") (and cond_1 cond_2)
      (= m "||") (or cond_1 cond_2)
      (or (= m ">") (= m ">=") (= m "<") (= m "<=")) ((ns-resolve *ns* (symbol m)) cond_1 cond_2)
      :else
      ;(str/starts-with? m "#")
      (let [pattern (try (re-pattern m) (catch Exception e nil))]
        (if pattern
          (= cond_2 (not (nil? (re-matches pattern cond_1))))
          ;Errorで処理終了すべき
          false
          )
        )
      )
    )
  )

(defn get-url-from-condition [record array-data]
  (let [ctrl-cond (first array-data)]
    (cond
      (or (= ctrl-cond :if) (= ctrl-cond :elif))
      (if (is-condition-true record (second array-data))
        (get-url-from-condition record (first (nnext array-data)))
        (get-url-from-condition record (next (nnext array-data)))
        )
      (= ctrl-cond :else)
      (get-url-from-condition record (last array-data))
      (= ctrl-cond :list) (flatten (keep #(get-url-from-condition record %) (next array-data)))
      (nil? ctrl-cond) nil
      :else
      ;url pattern
      (fill-field-data record array-data)
      )
    )
  )

(defn get-all-url [record all-conditions]
  (map #(get-url-from-condition record %) all-conditions)
  )
