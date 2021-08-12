(ns plug-field.dev.table
  (:require [re-frame.core :as rf]
            [plug-field.dev.sample :as sample]
            [plug-field.re-frame :as pfrf]
            [plug-field.ui.table :as pf-table]))


(rf/reg-sub
  ::header-factories
  :<- [::pfrf/key-config]
  :<- [::pfrf/common-header-config]
  :<- [::pfrf/field-defaults]
  :<- [::sample/target-fields]
  pfrf/create-field-factories)


(rf/reg-sub
  ::table-headers
  :<- [::header-factories]
  pfrf/produce-with-factories)


(rf/reg-sub
  ::field-value-factories
  :<- [::pfrf/value-config]
  :<- [::pfrf/common-content-config]
  :<- [::pfrf/field-defaults]
  :<- [::sample/target-fields]
  pfrf/create-field-factories)


(rf/reg-sub
  ::table-contents
  :<- [::field-value-factories]
  :<- [:sample/entities]
  pfrf/produce-with-factories)


(rf/reg-sub
  ::table-data
  :<- [::table-headers]
  :<- [::table-contents]
  :<- [::pf-table/default-config]
  (fn [[headers contents table-config]]
    {:headers      headers
     :content-rows contents
     :cfg          table-config}))

