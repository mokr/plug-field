(ns plug-field.re-frame
  (:require
    [plug-debug.core :as d]
    [plug-field.core :as pf]
    [plug-field.defaults :as defaults]
    [clojure.spec.alpha :as s]
    [plug-field.specs.core :as $]
    [plug-field.specs.config :as $cfg]
    [plug-field.specs.field :as $field]
    [plug-field.specs.re-frame :as $rf]
    [plug-utils.spec :refer [valid?]]
    [re-frame.core :as rf]))


;|-------------------------------------------------
;| DEFINITIONS

(def ^:private KEY-CONFIG ::key-config)
(def ^:private VALUE-CONFIG ::value-config)
(def ^:private DEFAULT-DOMAIN :_default)


;|-------------------------------------------------
;| HELPERS

(defn- coll-of-colls? [arg]
  (every? sequential? arg))


;|-------------------------------------------------
;| SET / GET HANDLING

(defn- merge-in-key-config [db domain cfg]
  (update-in db [KEY-CONFIG domain] merge cfg))


(defn- merge-in-value-config [db domain cfg]
  (update-in db [VALUE-CONFIG domain] merge cfg))


(defn- update-key-config
  "Register field config for keys in db.
  Two arities:
  - [cfg]         - Just a map of new config {:key1 {<cfg for key1>} :keyX {<cfg for keyX>} ,,,}
  - [domain cfg]  - 'domain' (last piece of path) to store config under + config as above "
  [db [arg1 arg2]]
  {:pre [(valid? ::$rf/domain-or-multi-field-config arg1)
         (valid? ::$rf/maybe-multi-field-config arg2)]}
  (if (some? arg2)
    (merge-in-key-config db arg1 arg2)
    (merge-in-key-config db DEFAULT-DOMAIN arg1)))


(defn- update-value-config
  "Register field config for key values in db.
  Two arities:
  - [cfg]         - Just a map of new config {:key1 {<cfg for key1 values>} :keyX {<cfg for keyX values>} ,,,}
  - [domain cfg]  - 'domain' to store config under. cfg as for arity 1 above"
  [db [arg1 arg2]]
  {:pre [(valid? ::$rf/domain-or-multi-field-config arg1)
         (valid? ::$rf/maybe-multi-field-config arg2)]}
  (if (some? arg2)
    (merge-in-value-config db arg1 arg2)
    (merge-in-value-config db DEFAULT-DOMAIN arg1)))


(defn- get-value-config
  "Retrieve config for (key) values
  Note: domain arg is optional"
  [db [_ domain]]
  {:pre [(s/valid? ::$rf/maybe-domain domain)]}
  (let [path [VALUE-CONFIG (or domain DEFAULT-DOMAIN)]]
    (get-in db path {})))


(defn- get-key-config
  "Retrieve config for keys.
  Note: domain arg is optional"
  [db [_ domain]]
  {:pre [(s/valid? ::$rf/maybe-domain domain)]}
  (let [path [KEY-CONFIG (or domain DEFAULT-DOMAIN)]]
    (get-in db path {})))


;|-------------------------------------------------
;| EVENTS & SUBSCRIPTION - public interface

(rf/reg-event-db
  ::add-key-config
  [rf/trim-v]
  update-key-config)


(rf/reg-event-db
  ::add-value-config
  [rf/trim-v]
  update-value-config)


(rf/reg-sub
  ::key-config
  get-key-config)


(rf/reg-sub
  ::value-config
  get-value-config)


;|-------------------------------------------------
;| SUBSCRIPTION HELPERS

(defn create-field-factories [args]
  (apply pf/make-factories args))


;|-------------------------------------------------
;| EVENT HELPERS

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