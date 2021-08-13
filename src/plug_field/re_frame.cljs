(ns plug-field.re-frame
  "Everything re-frame related when creating and using Fields.

  Like:
  - Registering and retrieving config for field creation for both keys and values
  - Event handlers for e.g. factory creation and usage
  - Subscriptions to default data that goes into the creation"
  (:require
    [plug-field.core :as pf]
    [plug-field.defaults :as default]
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
;| SUBSCRIBE TO (DEFAULT) CONFIGS

(rf/reg-sub
  ::common-content-config
  (fn [db [_ overrides]]
    (merge default/common-content-config
           overrides)))


(rf/reg-sub
  ::common-header-config
  (fn [db [_ overrides]]
    (merge default/common-header-config
           overrides)))

(rf/reg-sub
  ::field-defaults
  (fn [db [_ overrides]]
    (merge default/field-defaults
           overrides)))

;; Config for a row
(rf/reg-sub
  ::row-config
  (fn [db [_ specific-config]]                              ;; Here you might want to pass in e.g. {:react-key :db/id}
    specific-config))


;; We have no real entities, so a vector with an empty map is used for factories to run on.
(rf/reg-sub
  ::no-entities
  (fn [_ _]
    [{}]))


;|-------------------------------------------------
;| SUBSCRIPTION HELPERS

(defn create-field-factories
  "re-frame 'reg-sub' computation fn for creating factories from args vector:
  [[configs common-config field-defaults fields]]

  Where configs is configs for multiple keys or key-values"
  [args]
  (apply pf/make-factories args))


(defn as-table-data
  "re-frame 'reg-sub' computation fn for turning data from multiple subscriptions
  into the map taken as input by plug-field.ui.table/component"
  [[headers contents table-config]]
  {:pre  [(sequential? headers) (sequential? contents) (map? table-config)]
   :post [(map? %)]}
  {:header-row   (first headers)                            ;; There is only one header-entity, so we pass just that to table.
   :content-rows contents
   :cfg          table-config})


;|-------------------------------------------------
;| EVENT HELPERS

(defn produce-field-entities-with-factories
  "re-frame 'reg-sub' computation fn for creating maps containing
  the Field records produced by running all factories for a given entity
  and also the react-key that can be used for e.g. a row presenting these Fields

  entity-config supports customizing how :rect-key is created.

  RETURNS:
  [{:react-id ___ :fields [Field Field ,,,]} ,,,]"
  [[factories entities entity-config]]
  (let [entities (or (seq entities) [{}])                   ;; Ensure nil end [] => [{}] to run factories on an empty entity (typically for producing headers)
        config   (or entity-config {})]                     ;; Just to allow nil if there is no entity-config
    (pf/produce-with-factories factories entities config)))
