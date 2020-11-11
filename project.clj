(defproject url-creator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]
                 [eggplant "0.2.0"]
                 ]
  :main ^:skip-aot url-creator.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test-paths ["test"]
             :test-selectors {:default (complement :all)
                              :user001 :user001
                              :user002 :user002
                              :user003 :user003
                              :all (fn[_] true)}
             }
  )
