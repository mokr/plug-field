(ns plug-field.dev.table
  (:require [re-frame.core :as rf]
            [plug-field.core :as pf]
            [plug-field.dev.sample :as sample]
            [plug-field.re-frame :as pfrf]
            [plug-utils.spec :refer [valid?]]))


(rf/reg-sub
  ::header-factories
  :<- [::sample/fields-config]
  :<- [::sample/common-header-config]
  :<- [::sample/field-defaults]
  :<- [::sample/target-fields]
  pfrf/create-field-value-factories)


(rf/reg-sub
  ::table-headers
  :<- [::header-factories]
  ;:<- [:sample/entries]
  pfrf/produce-with-factories)


;(rf/reg-sub
;  ::field-value-factories
;  :<- [::sample/target-fields]
;  :<- [::sample/field-values-config]
;  :<- [::sample/common-content-config]
;  :<- [::sample/field-defaults]
;  pfrf/create-field-value-factories)

(rf/reg-sub
  ::field-value-factories
  :<- [::sample/field-values-config]
  :<- [::sample/common-content-config]
  :<- [::sample/field-defaults]
  :<- [::sample/target-fields]
  pfrf/create-field-value-factories)


(rf/reg-sub
  ::table-contents
  :<- [::field-value-factories]
  :<- [:sample/entities]
  ;pfrf/produce-value-fields-with-factories
  pfrf/produce-with-factories
  )


(rf/reg-sub
  :table/data
  :<- [::table-headers]                                     ;; Use this
  ;:<- [::sample/target-fields]                              ;;DEBUG
  :<- [::table-contents]
  (fn [[headers contents]]
    ;(js/console.info "HEADERS:\n" headers)
    ;(js/console.info contents)
    {:headers      headers
     :content-rows contents}))


;|-------------------------------------------------
;| UI

(defn row-of-fields [fields]
  [:tr
   (for [field fields]
     ^{:key (:react-key field)}
     [field])])

;;TODO: Go with 'header-rows' to allow for e.g. filter input below each header?
(defn head-section [header-fields]
  [:thead
   [row-of-fields header-fields]
   #_[:tr
      (for [field header-fields]
        ^{:key (str field)}
        [:th (str field)])]])


(defn content-row [fields]
  [:tr
   (for [field fields]
     ^{:key (:react-key field)}
     [field])])


(defn body-section [content-rows]
  [:tbody
   (for [entries content-rows]
     ^{:key (str entries)}
     [content-row entries])])


(def table-cfg
  {:classes {:wrapper     ["table-container"]
             :table       ["table is-narrow is-striped"]
             :tbody       []
             :thead       []
             :content-row []
             :header-row  []}})


(defn component [{:keys [headers content-rows]}]
  [:div.table-container
   [:table
    [head-section headers]
    [body-section content-rows]]])

