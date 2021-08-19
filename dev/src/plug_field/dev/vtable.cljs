(ns plug-field.dev.vtable
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
  :<- [::pfrf/row-config {:react-key "header"}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::field-value-factories
  :<- [::pfrf/value-config]
  :<- [::pfrf/common-content-config {:id-key :db/id}]
  :<- [::pfrf/field-defaults]
  :<- [::sample/target-fields]
  pfrf/create-field-factories)


;|-------------------------------------------------
;| MULTIPLE ENTITIES

(rf/reg-sub
  ::table-contents-multi
  :<- [::field-value-factories]
  :<- [:sample/entities]                                    ;; Multiple entities
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::vtable-data-multi
  :<- [::table-headers]
  :<- [::table-contents-multi]
  :<- [::pf-table/default-config]
  pfrf/as-vtable-data)


;|-------------------------------------------------
;| SINGLE ENTRY (KV LISTING)

(rf/reg-sub
  ::just-one-entity
  :<- [:sample/entities]
  (fn [entities]
    (take 1 entities)))


(rf/reg-sub
  ::table-contents-single
  :<- [::field-value-factories]
  :<- [::just-one-entity]                                   ;; Single entity (a kv listing)
  :<- [::pfrf/row-config {:id-key :db/id}]
  pfrf/produce-field-entities-with-factories)


(rf/reg-sub
  ::vtable-data-single
  :<- [::table-headers]
  :<- [::table-contents-single]
  :<- [::pf-table/default-config]
  pfrf/as-vtable-data)