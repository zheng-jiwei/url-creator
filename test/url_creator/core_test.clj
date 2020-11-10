(ns url-creator.core-test
  (:require [clojure.test :refer :all]
            [url-creator.core :refer :all])
  (:import (java.text SimpleDateFormat)
           (java.net URLEncoder)))


(def test-input
  [[:if [:news->type "==" 5] "/news/[:news->cf_n_date->__str_date [\"YYYY\"]].html"
    :elif [:news->_key #"\d+" true] "/news/regex/true/:news->_key"
    :elif [:news->category->_key "==" 15] "/tennis/products/[:news->category->__loop [:cf_pc_parent_category, \"==\", \"null\", :cf_pc_code]]"
    :elif [:news->category->_key "==" 18] "/tennis/products/search?name=@:news->category->c_url->__urlencode"
    :elif [:news->category->_key "==" 16] "/[:news->cf_a_regions->__map [\"us\" \"usa\" \"as\" \"asia\"]]/tennis/products/"
    :elif [[[:news->type "==" 3] "&&" [:news->category->rank ">" 90]] "||" [:news->type "==" 20]]
    [:list
     [:if [:news->cf_n_site->cf_s_code "==" "portal"] "/a/b/:news->_key"]
     [:if [:news->category->_key ">" 20] "/en/product/:news->tags->__first"]
     [:if [:news->_key #"^cd\d+" false] "/news/regex/false/:news->_key"]
     ]
    :else "/news/else/:_key"
    ]
   [:if [:news->status "==" 300] "/news/:news->status/:news->_key"]
   ]
  )


;制限事項
;URLの式の中に、[.]と[/]が含まないこと　（"/news/[:news->cf_n_date->__str_date [\"YYYYMMdd\"]].html" を解析のため、/と.で各エレメントを区切りしています）
;urlに値はvectorのfieldは１つのみサポートします


(deftest map-test
  (testing "first"
    (let [new_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")]
      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 9, :rank 80}, :type 5 :status 1, :_key "8888", :cf_n_date new_date}}]
        (is (= (first (get-url-from-condition data (first test-input))) "/news/2020.html"))
        )
      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 9, :rank 80}, :type 4 :status 1, :_key "8888", :cf_n_date new_date}}]
        (is (= (first (get-url-from-condition data (first test-input))) "/news/regex/true/8888"))
        )
      (let [data {:news {:category {:name "[ロッテ] Topバナー", :_key 15, :rank 80 :cf_pc_code "first" :cf_pc_parent_category {:cf_pc_parent_category {:cf_pc_parent_category nil :cf_pc_code "no_code"} :cf_pc_code "second"}}, :type 4 :status 1, :_key "abcd", :cf_n_date new_date}}]
        (is (= (first (get-url-from-condition data (first test-input))) "/tennis/products/first/second/no_code"))
        )
      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 18, :rank 80 :c_url "テストのみ"}, :type 4 :status 1, :_key "t_333", :cf_n_date new_date}}]
        (is (= (first (get-url-from-condition data (first test-input))) (str "/tennis/products/search?name=" (URLEncoder/encode "テストのみ" "UTF-8"))))
        )
      (let [data {:news {:category {:name "[ロッテ] Topバナー", :_key 16}, :cf_a_regions ["us" "as" "en"] :type 4 :status 1, :_key "t_111", :cf_n_date new_date}}
            result (get-url-from-condition data (first test-input))]
        (is (and (some #(= "/en/tennis/products/" %) result)
                 (some #(= "/usa/tennis/products/" %) result)
                 (some #(= "/asia/tennis/products/" %) result)
                 ))
        )
      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 30, :rank 100}, :cf_n_site {:cf_s_code "portal"}  :type 3 :status 1, :_key "t_1023", :tags ["cannon" "toshiba" "sony"]}}
            result (get-url-from-condition data (first test-input))
            ]
        (prn "###" result)
        (is (and (some #(= "/a/b/t_1023" %) result)
                 (some #(= "/news/regex/false/t_1023" %) result)
                 (some #(= "/en/product/cannon" %) result)
                 ))
        )
      )
    )
  )


