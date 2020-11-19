(ns url-creator.parse-template
  (:gen-class)
  ;(:refer-clojure :exclude [first])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    )
  (:import
    (org.apache.velocity VelocityContext Template)
    (org.apache.velocity.app VelocityEngine Velocity)
    (java.util HashMap ArrayList)
    (java.text SimpleDateFormat)
    (org.apache.velocity.runtime RuntimeConstants)
    (org.apache.velocity.tools.generic DateTool)
    (org.apache.commons.codec.net URLCodec)
    (java.io StringWriter)
    (custom.velocity Functions)
    (java.net URLEncoder))
  )


(defprotocol CustomClass
  (__contains [this coll search])
  (__map [this coll map-data])
  (__format [this date str-format])
  (__split [this str-data str-seperator])
  (__encode [this str-data])
  (__first [this coll])
  (__find [this target coll search])
  )

(defn smart-call []
  (reify CustomClass
    (__contains [this coll search]
      (some #(= search %) coll)
      )
    (__map [this coll map-data]
      (map #(get map-data % %) coll)
      )
    (__format [this str-format date]
      (. (SimpleDateFormat. str-format) format date)
      )
    (__split [this str-data str-seperator]
      (str/split str-data (re-pattern str-seperator))
      )
    (__encode [this str-data]
      (URLEncoder/encode str-data "UTF-8")
      )
    (__first [this coll]
      (first coll)
      )
    (__find [this target coll search]
      (reduce (fn [result item]
                (when (= (get item search) target)
                  (reduced item)
                  )
                )
              nil coll)
      )
    )
  )

(defn fill-template-with-record [template record]
  (let [writer (StringWriter.)]
    (Velocity/evaluate (VelocityContext. (java.util.HashMap. (walk/stringify-keys record))) writer "test-template" template)
    (prn (.toString writer))
    )
  )

(def user_input
  [[:if [:news->type "==" 5] "/news/[:news->cf_n_date->__str_date [\"YYYY\"]].html"
    :elif [:news->type "==" 6 "&&" :news->include_day "==" true] "/news/[:news->cf_n_date->__str_date [\"YYYY-MM-dd\"]].html"
    :elif [:news->_key #"\d+" true] "/news/regex/true/:news->_key"
    :elif [:news->category->_key "==" 15] "/tennis/products/[:news->category->__loop [:cf_pc_parent_category, \"==\", \"null\", :cf_pc_code]]"
    :elif [:news->category->_key "==" 18] "/tennis/products/search?name=@:news->category->c_url->__urlencode"
    :elif [:news->category->_key "==" 16] "/[:news->cf_a_regions->__map [\"us\" \"usa\" \"as\" \"asia\"]]/tennis/products/"
    :elif [:news->cf_s_add_business_type "in" #{"ミキサロン" ,"エンターテインメント", "ベルエポック" , "ロッジ", "クーリエ"}] "/shop/business/:news->_key/"
    :elif [:news->_id "in" :news_series->cf_p_series] "/news/in/group/:news->_key"
    :elif [:news->type "==" 6] "/:news->cf_n_regions/news/category/info/"
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

(defn test-general []
  (try
    (fill-template-with-record "${param1} and ${param2}" {:param1 "tom" :param2 "jerry"})
    (fill-template-with-record "${param1.param2.param3}" {:param1 {:param2 {:param3 "deep"}}})
    (fill-template-with-record "${param1.param2._key}" {:param1 {:param2 {:_key "with _key deep"}}})
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-if-else []
  (try
    (fill-template-with-record
      "#if( $a == $b )\n${a}=${b} is true#end" {:a 1 :b 1})
    (fill-template-with-record
      "#if( $a == $b )\ntrue#else\n${a}=${b} is False#end" {:a 1 :b 2})
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-foreach []
  (try
    (fill-template-with-record
      "
#foreach( ${element} in ${product.cf_n_regions})
/${element}/products/${product.cf_pjp_cm_category_id._key}/${product._key}.html
#end
      "
      {:product {:_key "p_0101" :cf_n_regions ["asia" "en" "jp" "cn"] :cf_pjp_cm_category_id {:_key "3333"}}}
      )
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-format-date []
  (try
    (fill-template-with-record
      "/news/${customClass.format('yyyy', ${news.cf_n_date})}.html"
      (assoc {:news {:cf_n_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")}}
        :customClass (DateTool.)))
    (fill-template-with-record
      "/news/${customClass.format('yyyyMMdd', ${news.cf_n_date})}.html"
      (assoc {:news {:cf_n_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")}}
        :customClass (DateTool.)))
    (fill-template-with-record
      "/news/${customClass.format('yyyy-MM-dd', ${news.cf_n_date})}.html"
      (assoc {:news {:cf_n_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")}}
        :customClass (DateTool.)))
    (fill-template-with-record
      "clojure class
      /news/${customClass.__format('yyyy', ${news.cf_n_date})}.html"
      (assoc {:news {:cf_n_date (.parse (SimpleDateFormat. "yyyy/MM/dd") "2020/08/01")}}
        :customClass (smart-call)))
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-urlencode []
  (try
    (fill-template-with-record
      "/words/emterms/search_result.html?keyword=${customClass.encode( ${tem_glossary.g_ja_term} )}"
      (assoc {:tem_glossary {:g_ja_term "新商品"}}
        :customClass (URLCodec. "UTF-8")))
    (fill-template-with-record
      "clojure class
      /words/emterms/search_result.html?keyword=${customClass.__encode( ${tem_glossary.g_ja_term} )}"
      (assoc {:tem_glossary {:g_ja_term "新商品"}}
        :customClass (smart-call)))
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-contains []
  (try
    (fill-template-with-record
      "#set( ${is_include} = ${customClass.contains( [\"1\", \"4\"], ${product.cf_p_class} )} )
      [1 4] contains ${product.cf_p_class} ${is_include}"
      (assoc {:product {:cf_p_class "1"}}
        :customClass (Functions.)))
    (fill-template-with-record
      "#set( ${is_include} = ${customClass.contains( [\"1\", \"4\"], ${product.cf_p_class} )} )
      [1 4] contains ${product.cf_p_class} ${is_include}"
      (assoc {:product {:cf_p_class "2"}}
        :customClass (Functions.)))
    (fill-template-with-record
      "#set( ${is_include} = ${customClass.contains( [1, 4], ${product.cf_p_class} )} )
      [1 4] contains ${product.cf_p_class} ${is_include}"
      (assoc {:product {:cf_p_class 1}}
        :customClass (Functions.)))
    (fill-template-with-record
      "#set( ${is_include} = ${customClass.contains( [1, 4], ${product.cf_p_class} )} )
      [1 4] contains ${product.cf_p_class} ${is_include}"
      (assoc {:product {:cf_p_class (int 4)}}
        :customClass (Functions.)))
    (fill-template-with-record
      "clojure class
      #set( ${is_include} = ${customClass.__contains( [1, 4], ${product.cf_p_class} )} )
      [1 4] contains 4 ${is_include}"
      (assoc {:product {:cf_p_class 4}}
        :customClass (smart-call)))
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-map []
  (try
    (fill-template-with-record
      "
      #set( ${area} = ${customClass.map( ${news.cf_n_regions},  {\"us\":\"usa\", \"as\":\"asia\"})} )
      #foreach( ${element} in ${area} )
        /$element/news/detail/${news._key}.html
      #end
         "
      (assoc {:news {:_key "10008" :cf_n_regions ["us" "as" "en" "jp"]}}
        :customClass (Functions.)))
    (fill-template-with-record
      "clojure class
      #set( ${area} = ${customClass.__map( ${news.cf_n_regions},  {\"us\":\"usa\", \"as\":\"asia\"})} )
      #foreach( ${element} in ${area} )
        /$element/news/detail/${news._key}.html
      #end
         "
      (assoc {:news {:_key "10008" :cf_n_regions ["us" "as" "en" "jp"]}}
        :customClass (smart-call)))
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-first []
  (try
    (fill-template-with-record
      "#set( ${wave} = ${customClass.first( ${product_base.ent_pb_wavelengths} )} )
      /jp/products/keyword_details.html?wavelength=${wave._key}\n
            ${wave}
      "
      (assoc {:product_base {:ent_pb_wavelengths [{:_key "100"} {:_key "200"}]}}
        :customClass (Functions.)))
    (fill-template-with-record
      "#set( ${wave} = ${customClass.first( ${product_base.ent_pb_wavelengths} )} )
      /jp/products/keyword_details.html?wavelength=${wave}\n
            ${wave}
      "
      (assoc {:product_base {:ent_pb_wavelengths ["100" "200"]}}
        :customClass (Functions.)))
    (fill-template-with-record
      "#set( ${wave} = ${customClass.first( ${product_base.ent_pb_wavelengths} )} )
      /jp/products/keyword_details.html?wavelength=${wave}\n
            ${wave}
      "
      (assoc {:product_base {:ent_pb_wavelengths [100 200]}}
        :customClass (Functions.)))
    (fill-template-with-record
      "clojure class
      #set( ${wave} = ${customClass.__first( ${product_base.ent_pb_wavelengths} )} )
      /jp/products/keyword_details.html?wavelength=${wave}\n
            ${wave}
      "
      (assoc {:product_base {:ent_pb_wavelengths [100 200]}}
        :customClass (smart-call)))    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-find []
  (try
    (fill-template-with-record
      "#set( ${target_product} = ${customClass.find(  ${product_tennis_series._id} , ${product_tennis}, \"cf_p_series\" )} )
      #if( ${target_product} )
          /tennis/products/${target_product.cf_p_category.cf_pc_code}/${target_product._key}.html
      #end
      "
      (assoc {:product_tennis [{:cf_p_series "a01" :_key "key_a001" :cf_p_category {:cf_pc_code "a00001"}}
                               {:cf_p_series "a02" :_key "key_a002"  :cf_p_category {:cf_pc_code "a00002"}}]
              :product_tennis_series {:_id "a02"}}
        :customClass (Functions.)))
    (fill-template-with-record
      "clojure class
      #set( ${target_product} = ${customClass.__find(  ${product_tennis_series._id} , ${product_tennis}, \"cf_p_series\" )} )
      #if( ${target_product} )
          /tennis/products/${target_product.cf_p_category.cf_pc_code}/${target_product._key}.html
      #end
      "
      (assoc {:product_tennis [{:cf_p_series "a01" :_key "key_a001" :cf_p_category {:cf_pc_code "a00001"}}
                               {:cf_p_series "a02" :_key "key_a002"  :cf_p_category {:cf_pc_code "a00002"}}]
              :product_tennis_series {:_id "a02"}}
        :customClass (smart-call)))
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-split []
  (try
    (fill-template-with-record
      "
      #set( ${result} = ${customClass.split( ${product.cf_p_sex}, \"\\|\" )} )
      ${result}
      ${customClass.first( ${result} )}
      "
      (assoc {:product {:cf_p_sex "男|女"}}
        :customClass (Functions.)))
    (fill-template-with-record
      "clojure class
      #set( ${result} = ${customClass.__split( ${product.cf_p_sex}, \"\\|\" )} )
      ${customClass.__first( ${result} )}
      "
      (assoc {:product {:cf_p_sex "男|女"}}
        :customClass (smart-call)))
    (catch Exception e
      (prn "### error" e)
      ))
  )

(defn test-loop []
  (try
    (fill-template-with-record
      "
      #set( ${result} = \"\" )
      #set( ${current_category} = ${product_tennis.cf_p_category} )
      #foreach( $num in [1..5] )
          #if( $result == \"\" )
            #set( ${result} = ${current_category.cf_pc_code} )
          #else
            #set( ${result} = \"${result}/${current_category.cf_pc_code}\" )
          #end
          #if( ! ${current_category.cf_pc_parent_category} )
            #break;
          #else
            #set( ${current_category} = ${current_category.cf_pc_parent_category} )
          #end
      #end
      /tennis/products/${result}/${product_tennis._key}.html
      "
      (assoc {:product_tennis {:_key "key_01" :cf_p_category {:cf_pc_code "first" :cf_pc_parent_category {:cf_pc_code "second" :cf_pc_parent_category {:cf_pc_code "no_code" :cf_pc_parent_category nil}}}}}
        :customClass (Functions.)))
    (catch Exception e
      (prn "### error" e)
      ))
  )
