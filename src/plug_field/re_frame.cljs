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

(defn create-field-factories [args]
  (apply pf/make-factories args))

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


(defn coll-of-colls? [arg]
  (every? sequential? arg))


(defn produce-with-factories
  "re-frame 'reg-sub' computation fn

  Produces field value records
  using factories on entities

  NOTE: Takes a vector of args"
  [arg]
  ;; TODO: Change :post to check for Field
  ;{:pre [(sequential? args-vector)
  ;       (valid? ::$/factories factories)
  ;       (valid? ::$/entities entities)]
  ;:post [(valid? ::$field/rows-of-records %)]
  ;}                                                        ;; [[rec rec ,,,] [rec rec ,,,] ,,,]
  ;(js/console.info "produce-with-factories arg" arg)
  (if (coll-of-colls? arg)
    ;; TYPICALLY FIELD/KEY VALUE
    (let [[factories entities] arg
          produce-field-records (apply juxt factories)]     ;; Make a function that will apply each factory to input
      ;(js/console.info "entities" entities)
      (map produce-field-records entities))                 ;; Pass each entity to all factories to produce actual Fields
    ;; TYPICALLY PLAIN FIELD/KEY
    (let [factories arg]
      ;(println "HEAD factories" factories)
      (map #(% {}) factories)))                             ;; Just run factories with an empty map
  )