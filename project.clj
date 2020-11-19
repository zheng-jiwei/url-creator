(defproject url-creator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]
                 [eggplant "0.2.0"]
                 [org.apache.velocity/velocity "1.7"]
                 [org.apache.velocity/velocity-engine-core "2.2"]
                 [org.apache.velocity.tools/velocity-tools-generic "3.0"]
                 [commons-codec/commons-codec "1.11"]
                 ]
  :main ^:skip-aot url-creator.core
  :java-source-paths ["src/custom/velocity/"]
  :target-path "target/"
  :aot :all
  :profiles {:uberjar {:aot :all}
             :test-paths ["test"]
             :test-selectors {:default (complement :all)
                              :user001 :user001
                              :user002 :user002
                              :user003 :user003
                              :all (fn[_] true)}
             }
  )
