(ns url-creator.core-test
  (:require [clojure.test :refer :all]
            [url-creator.core :refer :all]
            [eggplant.core :refer :all]
            )
  (:import (java.text SimpleDateFormat)
           (java.net URLEncoder)))



;各サイトから纏めたURLパターンは、評価式を解析して予定なURL出力できるかをテストします
;各パターンのテスト情報を、specificationの前に記述しています。
(defspec ^:user001 test-get-url-by-db-and-input
         (let [new_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")]
           ; Date のyear部分のみURLに利用する
           (specification
             {:given "テスト（１）\n
                      :record (:cf_n_dateはDateタイプですが、YYYYの部分のみURLに埋め込むたい) \n
                      :input (ユーザー入力したIF文) がある"
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (yearの部分だけURLに追加する) を期待される"
              :data {:record {:news {:category {:_key 9, :rank 80}, :type 5 :status 1, :_key "8888", :cf_n_date new_date}}
                     :input [:if [:news->type "==" 5] "/news/[:news->cf_n_date->__str_date [\"YYYY\"]].html"]
                     :result ["/news/2020.html"]
                     }
              }
             )
           ; Dateの yyyy-MM-dd をURLに利用する
           (specification
             {:given "テスト（2）\n
                      :record (:cf_n_dateはDateタイプですが、YYYY-MM-ddのようにURLに埋め込みしたい) \n
                      :input (ユーザー入力したIF文) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (year-month-dayの部分を含むURL) を期待される"
              :data {:record {:news {:include_day true :category {:_key 9, :rank 80}, :type 6 :status 1, :_key "8888", :cf_n_date new_date}}
                     :input [:if [:news->type "==" 6 "&&" :news->include_day "==" true] "/news/[:news->cf_n_date->__str_date [\"YYYY-MM-dd\"]].html"]
                     :result ["/news/2020-08-01.html"]
                     }
              }
             )
           ; URLにあるfiledのkeywordを値で差し替える
           (specification
             {:given "テスト（3）\n
                      :record (DBから抽出したデータ) \n
                      :input (ユーザー入力したIF文評価式は正規表現を利用する) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordは、newsの_keyで差し替える) を期待される"
              :data {:record {:news {:include_day true :category {:_key 9, :rank 80}, :type 7 :status 1, :_key "8888", :cf_n_date new_date}}
                     :input [:if [:news->_key #"\d+" true] "/news/regex/true/:news->_key"]
                     :result ["/news/regex/true/8888"]
                     }
              }
             )
           ; collection_Aにあるfield_parentは再び collection_Aを参照する、field_parentはnilではなければ、collection_Aの field_bの値を順次取得してURLに埋め込む
           (specification
             {:given "テスト（4）\n
                      :record (:cf_pc_parent_category は自分自身のcollectionを参照する、または nil。nilではなければ、繰り返し:cf_pc_codeを取ってURLに埋め込む) \n
                      :input (ユーザー入力したIF文とURLパターン) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordは、record中の:cf_pc_codeを親から子までの順でつなげる) を期待される"
              :data {:record {:news {:category {:_key 15, :cf_pc_code "first" :cf_pc_parent_category {:cf_pc_parent_category {:cf_pc_parent_category nil :cf_pc_code "no_code"} :cf_pc_code "second"}}}}
                     :input [:if [:news->category->_key "==" 15] "/tennis/products/[:news->category->__loop [:cf_pc_parent_category, \"==\", \"null\", :cf_pc_code]]/index.html"]
                     :result ["/tennis/products/first/second/no_code/index.html"]
                     }
              }
             )
           ; fieldの値はURLに埋め込む前にencodeする必要があるのテスト
           (specification
             {:given "テスト（5）\n
                      :record (あるfieldの文字列をURLに埋め込みしたいので、その文字列をencodeする) \n
                      :input (ユーザー入力したIF文とURLパターン)"
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordの部分はencodeされる文字列になる) を期待される"
              :data {:record {:news {:category {:_key 18, :rank 80 :c_url "テストのみ"}, :type 4 :status 1, :_key "t_333"}}
                     :input [:if [:news->category->_key "==" 18] "/tennis/products/search?name=$:news->category->c_url->__urlencode"]
                     :result [(str "/tennis/products/search?name=" (URLEncoder/encode "テストのみ" "UTF-8"))]
                     }
              }
             )
           ; fieldの値はマッピングして、結果の文字列を利用するケース。 ["a" "b" "c" "d"] は場合、filed値は"a"なら、"b"を利用する
           (specification
             {:given "テスト（6）\n
                      :record (ある field の値を別の値に変更して利用したい) \n
                      :input (ユーザー入力したIF文とURLパターン) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordの部分は__mapで結果文字列へ変換する、対応する文字列がなければ、field値をそのまま利用する) を期待される"
              :data {:record {:news {:category {:_key 16}, :cf_a_regions ["us" "as" "en"] :type 4 :status 1, :_key "t_111", :cf_n_date new_date}}
                     :input [:if [:news->category->_key "==" 16] "/[:news->cf_a_regions->__map [\"us\" \"usa\" \"as\" \"asia\"]]/tennis/products/"]
                     :result ["/usa/tennis/products/"
                              "/asia/tennis/products/"
                              "/en/tennis/products/"
                              ]
                     }
              }
             )
           ; fieldの値は、ある文字列グループ中の一つである場合、IFの条件式が成立させて後ろのURLを出力する
           (specification
             {:given "テスト（7）\n
                      :record (あるfieldの文字列は複数文字列中の任意の文字列と同じの場合のみ、結果URLを出力する) \n
                      :input (ユーザー入力したIF文とURLパターン) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordの部分を値で入れ替えして出力する) を期待される"
              :data {:record {:news {:type 4 :status 1, :_key "t_333" :cf_s_add_business_type "ベルエポック"}}
                     :input [:if [:news->cf_s_add_business_type "in" #{"ミキサロン" ,"エンターテインメント", "ベルエポック" , "ロッジ", "クーリエ"}] "/shop/business/:news->_key/"]
                     :result ["/shop/business/t_333/"]
                     }
              }
             )
           ; fieldの値は、別のfieldの値に含まれる場合、IF式が成立させて後ろのURLを出力する
           (specification
             {:given "テスト（8）\n
                      :record (DBから抽出したデータ) \n
                      :input (ある field の値は、別のfieldまたは別collection field値の一部であることがIFの条件となる) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordの部分を値で入れ替えして出力する) を期待される"
              :data {:record {:news {:_key "t_333" :_id 123} :news_series {:cf_p_series [12 23 123]}}
                     :input [:if [:news->_id "in" :news_series->cf_p_series] "/news/in/group/:news->_key"]
                     :result ["/news/in/group/t_333"]
                     }
              }
             )
           ; 値は配列のfieldをURLにある場合、配列数毎にURLを出力する
           (specification
             {:given "テスト（9）\n
                      :record (ある field の値は配列なので、このfieldを含むURLは配列元素の数毎にURLを作成する) \n
                      :input (IF文と出力URLのテンプレート) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordの部分を値で入れ替えして出力する) を期待される"
              :data {:record {:news {:_key "t_333" :_id 123  :type 6 :cf_n_regions ["ab" "cd" "ef"]}}
                     :input [:if [:news->type "==" 6] "/:news->cf_n_regions/news/category/info/"]
                     :result ["/ab/news/category/info/"
                              "/cd/news/category/info/"
                              "/ef/news/category/info/"
                              ]
                     }
              }
             )
           ; URLが複数出力する予定で、:listを利用するパターンのテスト
           (specification
             {:given "テスト（10）\n
                      :record (DBから抽出したデータ) \n
                      :input (:listで複数URL出力する構文) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeywordの部分を値で入れ替えして出力する) を期待される"
              :data {:record {:news {:_key "50" :_id 123  :type 20 :cf_n_regions ["ab" "cd" "ef"] :cf_n_site {:cf_s_code "portal"}}}
                     :input [:if [:news->type "==" 20]
                             [:list
                              [:if [:news->cf_n_site->cf_s_code "==" "portal"] "/a/b/:news->_key"]
                              [:if [:news->_key #"^cd\d+" true] "/en/product/:news->_key"]
                              [:if [:news->_key #"^cd\d+" false] "/jp/product/:news->_key"]
                              ]
                             ]
                     :result ["/a/b/50"
                              "/jp/product/50"
                              ]
                     }
              }
             )
           ; 値が配列のfieldをURLにあって、そのfieldの先頭１個値を利用する
           (specification
             {:given "テスト（11）\n
                      :record (ある fieldの値は配列ですが、その中の先頭１個のみURLに埋め込む)\n
                      :input (IF文と出力URLのテンプレート) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeyword値の先頭１個のみURLに埋め込む) を期待される"
              :data {:record {:news {:category {:_key 30} :tags ["first" "second" "ef"]}}
                     :input [:if [:news->category->_key ">" 20] "/en/product/:news->tags->__first"]
                     :result ["/en/product/first"]
                     }
              }
             )
           ; 複数条件のテスト || の左側が効く
           (specification
             {:given "テスト（12）\n
                      :record (DBから抽出したデータ)\n
                      :input (複数条件で組み合わせて利用する ||の左部分が効く) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURL) を期待される"
              :data {:record {:news {:type 3 :category {:rank 100}}}
                     :input [:if [[[:news->type "==" 3] "&&" [:news->category->rank ">" 90]] "||" [:news->type "==" 20]] "/news/product/first"]
                     :result ["/news/product/first"]
                     }
              }
             )
           ; 複数条件のテスト || の右側が効く
           (specification
             {:given "テスト（13）\n
                      :record (DBから抽出したデータ)\n
                      :input (複数条件で組み合わせて利用する、||の右部分が効く) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeyword値の先頭１個のみURLに埋め込む) を期待される"
              :data {:record {:news {:type 20 :category {:rank 100}}}
                     :input [:if [[[:news->type "==" 3] "&&" [:news->category->rank ">" 90]] "||" [:news->type "==" 20]] "/news/product/first"]
                     :result ["/news/product/first"]
                     }
              }
             )
           ; elseのテスト
           (specification
             {:given "テスト（14）\n
                      :record (DBから抽出したデータ)\n
                      :input (複数条件で組み合わせて利用する、||の右部分が効く) "
              :when  "#url-creator.core/get-url-from-condition を使って、IF文は成立の場合"
              :then  ":result (IF文後ろのURLにあるkeyword値の先頭１個のみURLに埋め込む) を期待される"
              :data {:record {:news {:type 20 :category {:rank 100}}}
                     :input [:if [:news->type "==" 6] "/en/news/category/info/"
                             :else "/jp/news/category/info/"
                             ]
                     :result ["/jp/news/category/info/"]
                     }
              }
             )
           )
         )



;(def user_input
;  [[:if [:news->type "==" 5] "/news/[:news->cf_n_date->__str_date [\"YYYY\"]].html"
;    :elif [:news->type "==" 6 "&&" :news->include_day "==" true] "/news/[:news->cf_n_date->__str_date [\"YYYY-MM-dd\"]].html"
;    :elif [:news->_key #"\d+" true] "/news/regex/true/:news->_key"
;    :elif [:news->category->_key "==" 15] "/tennis/products/[:news->category->__loop [:cf_pc_parent_category, \"==\", \"null\", :cf_pc_code]]"
;    :elif [:news->category->_key "==" 18] "/tennis/products/search?name=@:news->category->c_url->__urlencode"
;    :elif [:news->category->_key "==" 16] "/[:news->cf_a_regions->__map [\"us\" \"usa\" \"as\" \"asia\"]]/tennis/products/"
;    :elif [:news->cf_s_add_business_type "in" #{"ミキサロン" ,"エンターテインメント", "ベルエポック" , "ロッジ", "クーリエ"}] "/shop/business/:news->_key/"
;    :elif [:news->_id "in" :news_series->cf_p_series] "/news/in/group/:news->_key"
;    :elif [:news->type "==" 6] "/:news->cf_n_regions/news/category/info/"
;    :elif [[[:news->type "==" 3] "&&" [:news->category->rank ">" 90]] "||" [:news->type "==" 20]]
;    [:list
;     [:if [:news->cf_n_site->cf_s_code "==" "portal"] "/a/b/:news->_key"]
;     [:if [:news->category->_key ">" 20] "/en/product/:news->tags->__first"]
;     [:if [:news->_key #"^cd\d+" false] "/news/regex/false/:news->_key"]
;     ]
;    :else "/news/else/:_key"
;    ]
;   [:if [:news->status "==" 300] "/news/:news->status/:news->_key"]
;   ]
;  )


; https://www.techscore.com/tech/Java/ApacheJakarta/Velocity/index/ 承認対象ワークフロー： 
; #foreach ($workflow in ${workflows})
;    ・${workflow.name} (${workflow.path}) 
; #end 
; https://handlebarsjs.com/



;制限事項
;URLの式の中に、[.]と[/]が含まないこと　（"/news/[:news->cf_n_date->__str_date [\"YYYYMMdd\"]].html" を解析のため、/と.で各エレメントを区切りしています）
;urlに値はvectorのfieldは１つのみサポートします


;
;(deftest map-test
;  (testing "first"
;    (let [new_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")]
;      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 9, :rank 80}, :type 5 :status 1, :_key "8888", :cf_n_date new_date}}]
;        (is (= (first (get-url-from-condition data (first user_input))) "/news/2020.html"))
;        )
;      (let [data {:news {:include_day true :category {:default_type 3, :name "[ロッテ] Topバナー", :_key 9, :rank 80}, :type 5 :status 1, :_key "8888", :cf_n_date new_date}}]
;        (is (= (first (get-url-from-condition data (first user_input))) "/news/2020-08-01.html"))
;        )
;      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 9, :rank 80}, :type 4 :status 1, :_key "8888", :cf_n_date new_date}}]
;        (is (= (first (get-url-from-condition data (first user_input))) "/news/regex/true/8888"))
;        )
;      (let [data {:news {:category {:name "[ロッテ] Topバナー", :_key 15, :rank 80 :cf_pc_code "first" :cf_pc_parent_category {:cf_pc_parent_category {:cf_pc_parent_category nil :cf_pc_code "no_code"} :cf_pc_code "second"}}, :type 4 :status 1, :_key "abcd", :cf_n_date new_date}}]
;        (is (= (first (get-url-from-condition data (first user_input))) "/tennis/products/first/second/no_code"))
;        )
;      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 18, :rank 80 :c_url "テストのみ"}, :type 4 :status 1, :_key "t_333", :cf_n_date new_date}}]
;        (is (= (first (get-url-from-condition data (first user_input))) (str "/tennis/products/search?name=" (URLEncoder/encode "テストのみ" "UTF-8"))))
;        )
;      (let [data {:news {:category {:name "[ロッテ] Topバナー", :_key 16}, :cf_a_regions ["us" "as" "en"] :type 4 :status 1, :_key "t_111", :cf_n_date new_date}}
;            result (get-url-from-condition data (first user_input))]
;        (is (and (some #(= "/en/tennis/products/" %) result)
;                 (some #(= "/usa/tennis/products/" %) result)
;                 (some #(= "/asia/tennis/products/" %) result)
;                 ))
;        )
;      (let [data {:news {:type 4 :status 1, :_key "t_333" :cf_s_add_business_type "ベルエポック"}}]
;        (is (= (first (get-url-from-condition data (first user_input))) "/shop/business/t_333/"))
;        )
;      (let [data {:news {:_key "t_333" :_id 123} :news_series {:cf_p_series [12 23 123]}}]
;        (is (= (first (get-url-from-condition data (first user_input))) "/news/in/group/t_333"))
;        )
;      (let [data {:news {:_key "t_333" :type 6 :_id 123 :cf_n_regions ["ab" "cd"]} :news_series {:cf_p_series [12 23]}}
;            result (get-url-from-condition data (first user_input))]
;          (is (and (some #(= "/ab/news/category/info/" %) result)
;                   (some #(= "/cd/news/category/info/" %) result)
;                   ))
;        )
;      (let [data {:news {:category {:default_type 3, :name "[ロッテ] Topバナー", :_key 30, :rank 100}, :cf_n_site {:cf_s_code "portal"}  :type 3 :status 1, :_key "t_1023", :tags ["cannon" "toshiba" "sony"]}}
;            result (get-url-from-condition data (first user_input))
;            ]
;        (prn "###" result)
;        (is (and (some #(= "/a/b/t_1023" %) result)
;                 (some #(= "/news/regex/false/t_1023" %) result)
;                 (some #(= "/en/product/cannon" %) result)
;                 ))
;        )
;      )
;    )
;  )


