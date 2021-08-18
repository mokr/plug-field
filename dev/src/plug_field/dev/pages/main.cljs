(ns plug-field.dev.pages.main
  (:require [plug-field.core :as pf]
            [plug-field.dev.sample]
            [plug-field.dev.table :as dev-table]
            [plug-field.dev.vtable :as dev-vtable]
            [plug-field.ui.table :as pf-table]
            [plug-field.ui.vtable :as vtable]
            [plug-utils.re-frame :refer [<sub >evt]]
            [plug-utils.reagent :refer [err-boundary]]))


(defn page []
  [:section.section>div.container>div.content

   [:h1 "Table demo"]
   [err-boundary
    [pf-table/component (<sub [::dev-table/table-data])]]

   [:h1 "vTable single (kv)"]
   [err-boundary
    [:div.columns
     [:div.column.is-one-third
      [vtable/component (<sub [::dev-vtable/vtable-data-single])]]]]

   [:h1 "vTable multi"]
   [err-boundary
    [:div.columns
     [:div.column.is-two-thirds
      [vtable/component (<sub [::dev-vtable/vtable-data-multi])]]]]])