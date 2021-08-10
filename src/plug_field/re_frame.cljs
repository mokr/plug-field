(ns plug-field.re-frame
  (:require [clojure.spec.alpha :as s]
    ;[plug-debug.core :as d]
            [plug-field.core :as pf]
            [plug-field.defaults :as defaults]
            [plug-field.specs.core :as $]
            [plug-field.specs.config :as $cfg]
            [plug-field.specs.field :as $field]
            [plug-utils.spec :refer [valid?]]
            [re-frame.core :as rf]))


;|-------------------------------------------------
;| EVENTS

(rf/reg-event-db
  ::add-field-config
  [rf/trim-v]
  (fn [db [field-config]]
    (update db ::field-config merge field-config)))


;|-------------------------------------------------
;| SUBSCRIPTION HELPERS

(defn create-field-value-factories
  "re-frame 'reg-sub' computation fn

  Produces a collection of factory functions for field values

  NOTE: Takes a vector of args:"
  [[target-fields field-value-configs common-field-value-config :as args-vector]]
  {:pre  [(sequential? args-vector)
          ;(sequential? target-fields)
          (s/valid? ::$/target-fields target-fields)
          (map? field-value-configs)
          (map? common-field-value-config)]
   :post [(s/valid? ::$/factories %)]}
  ;(js/console.info "Making factories")
  (pf/field-value-configs->field-factories
    target-fields
    field-value-configs
    common-field-value-config
    defaults/field-defaults))


;|-------------------------------------------------
;| EVENT HELPERS

(defn produce-value-fields-with-factories
  "re-frame 'reg-sub' computation fn

  Produces field value records
  using factories on entities

  NOTE: Takes a vector of args"
  [[factories entities :as args-vector]]
  {:pre [(sequential? args-vector)
         (valid? ::$/factories factories)
         (valid? ::$/entities entities)]
   ;:post [(valid? ::$field/rows-of-records %)]
   }                                                        ;; [[rec rec ,,,] [rec rec ,,,] ,,,]
  ;; TODO: Change :post to check for Field
  (let [produce-field-records (apply juxt factories)]       ;; Produce content by ..
    ;(js/console.info "FIELDS" (map produce-field-records entities))
    (map produce-field-records entities)))                  ;; .. passing each entity to every factory


