(ns plug-field.dev.pages.main
  (:require [plug-field.core :as pf]
            [plug-field.dev.sample]
            [plug-field.dev.table :as dev-table]
            [plug-field.ui.table :as pf-table]
            [plug-utils.re-frame :refer [<sub >evt]]))


(defn page []
  [:section.section>div.container>div.content
   [:h1 "plug-field DEV"]
   ;[:div "Hi, from example!"]
   [:h2 "Table demo"]
   [pf-table/component (<sub [::dev-table/table-data])]
   ])
