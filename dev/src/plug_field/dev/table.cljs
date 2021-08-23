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
  :<- [::pfrf/no-entities]
  :<- [::pfrf/row-config {:react-key "header-for-sample-table"}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::field-value-factories
  :<- [::pfrf/value-config]
  :<- [::pfrf/common-content-config {:id-key :db/id}]
  :<- [::pfrf/field-defaults]
  :<- [::sample/target-fields]
  pfrf/create-field-factories)


(rf/reg-sub
  ::table-contents
  :<- [::field-value-factories]
  :<- [:sample/entities]
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::table-data
  :<- [::table-headers]
  :<- [::table-contents]
  :<- [::pf-table/default-config]
  pfrf/as-table-data)


;|-------------------------------------------------
;| EMPTY TABLE

(rf/reg-sub
  ::no-content
  (fn []
    nil))


(rf/reg-sub
  ::table-contents-empty
  :<- [::field-value-factories]
  :<- [::no-content]
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::table-data-no-content
  :<- [::table-headers]
  :<- [::table-contents-empty]
  :<- [::pf-table/default-config]
  pfrf/as-table-data)