(defproject plug-field "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/clojurescript "1.10.879" :scope "provided"]]
  :repl-options {:init-ns plug-field.core}

  :source-paths ["src"]

  :clean-targets ^{:protect false}
  [:target-path "dev/resources/public/js"]


  :profiles
  {:dev {
         :dependencies   [[binaryage/devtools "1.0.3"]
                          [net.clojars.mokr/plug-debug "0.1.0-SNAPSHOT"]
                          [net.clojars.mokr/plug-utils "0.1.0-SNAPSHOT"]
                          [re-frame "1.2.0"]
                          [reagent "1.1.0"]
                          [re-frisk "1.5.1"]
                          [thheller/shadow-cljs "2.15.2" :scope "provided"]]

         :source-paths   ["src" "dev/src"]
         :resource-paths ["dev/resources"]

         }})