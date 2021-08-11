(ns plug-field.core
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [rename-keys]]
            [plug-debug.core :as d]
            [plug-field.defaults :as defaults]
            [plug-field.specs.core :as $]
            [plug-field.specs.config :as $cfg]
            [plug-field.specs.field :as $field]))


;|-------------------------------------------------
;| RECORD

(defrecord Field [k display render tag]
  IFn
  (-invoke [{:keys [render attrs] :as this}]
    (render this attrs))
  (-invoke [{:keys [render attrs] :as this} attrs-override]
    (render this (merge attrs attrs-override))))


;|-------------------------------------------------
;| HELPERS

(defn- arity
  "Find the arity of a function.
  Works on both named and anonymous functions"
  [function]
  (when (fn? function)
    (.-length function)))


;|-------------------------------------------------
;| FUNCTIONS RESPONSIBLE FOR EVALUATING PARTS OF A CONFIG
;|
;| - What they return is considered raw parts for a Field factory

(defn- add-raw-value
  "A function that will ensure raw value :v is added to every field (Field) so that 'deciders' can utilize its value"
  [{:keys [k] :as field-m} entity]
  {:pre  [(s/valid? ::$/entity entity)
          (s/valid? ::$field/in-the-making field-m)]
   :post [(s/valid? ::$field/in-the-making %)]}
  (assoc field-m :v (get entity k)))


(defn- decide-description
  "Set :description according to config for this entity.
  Note: Might be overwritten by later lookup"
  [{:keys [description] :as cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (when (some? description)
    {:description description}))


(defn- decide-tooltip
  "Return either a function that will add a :title attr to field map,
  or nil to signal that tooltip creation can be skipped for this field."
  [{:keys [tooltip description source-field] :as field-value-cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (cond
    (fn? tooltip) (fn [field-m entity]
                    (assoc-in field-m [:attrs :title]
                              (case (arity tooltip)         ;; Note: No default case, as other arities are invalid and should cause error.
                                1 (tooltip entity)
                                2 (tooltip entity field-m)
                                3 (tooltip entity field-m field-value-cfg))))
    (some? tooltip) [:attrs :title tooltip]                 ;; Add inside field-m's :attrs
    (some? description) [:attrs :title description]         ;; Add inside field-m's :attrs
    :else nil))


(defn- decide-display
  "Decide on the value that will actually be displayed when this field is rendered.
  At least when using the default render function."
  [{:keys [k
           lookup                                           ;; This comes in from common-config (unless field specific config has overridden it)
           display
           lookup-as                                        ;; This must be defined for lookup to happen. Set it equal to :field itself to look up with "raw" key.
           lookup-formatter
           value-formatter]
    :as   cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (cond
    ;;LOOKUP
    (some? lookup-as) (fn [{:keys [v] :as field-m} entity]  ;; should common-config (with :lookup) be a third arg here instead? Would that avoid some recompilation of configs?
                        (let [k (cond-> (or lookup-as k)
                                        (some? lookup-formatter) lookup-formatter)
                              v (cond-> v
                                        (some? value-formatter) value-formatter) ;; Use formatter here?
                              {:keys [display description] :as looked-up} (lookup k v)]
                          (assoc field-m :display (or display v)
                                         :description description) ;; Should we add :description here?
                          (get-in lookup [k v])))
    ;; FUNCTION
    (fn? display) (fn [field-m entity]
                    (if-let [v (display field-m entity cfg)]
                      (assoc field-m :display v)
                      field-m))
    ;; PLAN, STATIC STRING
    (string? display) {:display display}
    ;; GENERIC using value of k and optional formatter
    :else (fn [field-m entity]
            (if-let [v (cond-> (get entity k)
                               (some? value-formatter) value-formatter)]
              (assoc field-m :display v)
              field-m))))


(defn- decide-class
  "Config key :class can be either a plain string, a colletion of string or a function.
   If it's a function it is passed the entity and its config and should return either:
    - a string
    - a collection of strings
    - nil"
  [{:keys [class] :as cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (when class
    (if (fn? class)
      (fn [field-m entity]
        (assoc-in field-m [:attrs :class] (class entity cfg)))
      [:attrs :class class])))                              ;; Path in field-m where we want this value


(defn- decide-react-key-attr
  ":key attr that works as a React ID when redering"
  [{:keys [react-key k] :as cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (cond
    ;; React key generated per entity
    (fn? react-key) (fn [field-m entity]
                      (assoc field-m :react-key (react-key entity cfg)))
    ;; React key is statically defined in config for this field
    (some? react-key) {:react-key react-key}
    ;; Creating a static value from the field name
    :else (fn [field-m _]
            (assoc field-m :react-key (str k)))))


(defn- decide-tag
  "Set :tag according to config for this entity (only if special handling is needed)"
  [{:keys [tag] :as cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (when (some? tag)
    {:tag tag}))


(defn- decide-handler
  "Allow for custom event handler functions with multiple arities in case more than the event itself is needed.
  [event]                  -- Plain handler getting just the js event
  [entity event]           -- + full entity map this Field was created from
  [entity field-cfg event] -- + the field config used to create Field"
  [handler-key cfg]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (when-let [handler-fn (get cfg handler-key)]              ;; When there is configured a handler for this kind..
    (when (fn? handler-fn)                                  ;; .. and it is a function ...
      (case (arity handler-fn)                              ;; .. return an appropriate config maker for that arity.
        2 (fn [field-m entity]
            (assoc-in field-m [:attrs handler-key]
                      (partial handler-fn entity)))
        3 (fn [field-m entity]
            (assoc-in field-m [:attrs handler-key]
                      (partial handler-fn entity cfg)))
        [:attrs handler-key handler-fn]))))                 ;; Add inside field-m's :attrs


(defn- decide-render
  "Typically we use the default renderer, but here we allow for a custom one.
  Typically for things like identicon and synthetic fields"
  [{:keys [render] :as cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (when (some? render)
    {:render render}))                                      ;; Call the render provided in config.


;|-------------------------------------------------
;| TYPE SPECIFIC PARTS MAKERS (FIELD VS FIELD VALUE)

(defn- produce-factory-parts-for-field-value
  "Create collection of factory parts for a Field based on 'value' in {:key value}
  Will aid in producing Fields that are typically used as cells/contents in tables.
  These raw parts are functions, maps and nils"
  [field-defaults field-value-cfg]
  [field-defaults
   {:k (:k field-value-cfg)}                                ;; Note: It's called :k instead of :field to avoid confusing with the Field we create for it
   add-raw-value                                            ;; Allways. Do early so others can use it
   (decide-description field-value-cfg)
   (decide-display field-value-cfg)
   (decide-tooltip field-value-cfg)                         ;; After :display as we might want to use info that was looked up
   (decide-class field-value-cfg)
   (decide-react-key-attr field-value-cfg)
   (decide-tag field-value-cfg)
   (decide-handler :on-click field-value-cfg)
   (decide-render field-value-cfg)])


(defn- produce-factory-parts-for-field
  "Create collection of factory parts for a Field based on ':key' in {:key value}
  Will aid in producing Fields that are typically used as headers in tables.
  These raw parts are functions, maps and nils"
  [field-defaults field-cfg]
  [field-defaults
   {:k (:k field-cfg)}                                      ;; Note: It's called :k instead of :field to avoid confusing with the Field we create for it
   add-raw-value                                            ;; Useful for plain field?
   (decide-description field-cfg)
   (decide-display field-cfg)
   (decide-tooltip field-cfg)                               ;; After :display as we might want to use info that was looked up
   (decide-class field-cfg)
   (decide-react-key-attr field-cfg)
   (decide-tag field-cfg)
   (decide-handler :on-click field-cfg)
   (decide-render field-cfg)])


;|-------------------------------------------------
;| ASSEMBLE FACTORY FROM PARTS

(defn- parts-group
  "What variant is the part returned from config compilation (typically by decide-* functions)?"
  [part]
  (cond
    (fn? part) :functions                                   ;; Work on field-m with data from an entity
    (map? part) :maps                                       ;; Top level value in field-m (merge)
    (coll? part) :colls                                     ;; Nested value in field-m (update-in)
    :else :ignored))                                        ;; E.g. nils


(def ^:private group-factory-parts
  (partial group-by parts-group))


(defn- smart-updater
  "Combine existing and new value in a predictable way.
  'v2' is last entry from a factory part in vector form (signalling update-in)"
  [v1 v2]
  (cond
    (map? v2) (merge v1 v2)                                 ;; Combine:      Overwrites if same key. Note: Before coll? as (coll? {}) => true
    (some coll? [v1 v2]) (remove nil? (flatten [v1 v2]))    ;; Aggregate:    Vector signals that we might want more than one value, e.g. in :class
    (string? v2) v2                                         ;; Overwrite:    String signals desire to overwrite
    (some? v2) v2                                           ;; No overwrite: nil won't overwrite an existing value
    :else v1))                                              ;; Keep:         No new value


(defn- assemble-grouped-factory-parts
  "Assemble parts into what will be the base field-m and the functions that will
   work on it inside the factory when passed an actual entity map.
   'field-m'   - The map that forms the basis for a Field. Starts out with defaults and via factory parts and functions it is merged and transformed into a field map;
   'functions' - Functions that will help transform 'field-m' into a 'field' map\"
   "
  [{:keys [maps colls functions]}]
  (let [field-m (reduce (fn [field-m coll]
                          (update-in field-m (butlast coll) smart-updater (last coll)))
                        (apply merge maps)
                        colls)]
    {:field-m   field-m
     :functions functions}))


(defn- factory-parts->factory
  "Assemble a factory function from the compiled parts."
  [{:keys [functions field-m]}]
  (fn [entity]
    (->> functions                                          ;; Take all the functions that will help prepare a field according to config. We use functions when we need data from the entity itself.
         (reduce (fn [field-m func]                         ;; And apply each of them to the field map we have so far and the entity this field belongs to.
                   (func field-m entity))
                 field-m)                                   ;; TODO: Look into making this a transient and update all decide-* fns to use assoc! OBS: there is no assoc-in!
         (map->Field))))


;|-------------------------------------------------
;| CREATE FACTORIES

(defn- config->field-factory
  "Turn the config for a given field or field value into a factory that will
  assemble a field according to config."
  [field-defaults parts-producer field-value-cfg]
  {:pre  []
   :post [(s/valid? ::$/factory %)]}
  (-> field-defaults
      (parts-producer field-value-cfg)
      (group-factory-parts)
      (assemble-grouped-factory-parts)
      (factory-parts->factory)))


(defn make-factories
  "Create a collection of factory functions for given fields"
  [parts-producer configs common-config field-defaults fields]
  {:pre  [(sequential? fields)
          (fn? parts-producer)
          (map? configs)
          ;(s/valid? ::$cfg/field-value-configs field-value-configs) ;;FIXME: The full config map, not just for a single field
          (map? common-config)]
   :post [(s/valid? ::$/factories %)]}
  (let [xf (comp
             (map #(-> (merge common-config                 ;; Contains e.g. :lookup and other features all configs should have access to. Note: A specific config can override are it it merge on top.
                              (get configs %))              ;; Get the config for the field in question ..
                       (assoc :k %)))                       ;; update config with the target field/key name.
             (map #(config->field-factory
                     field-defaults
                     parts-producer
                     %)))]
    (into [] xf fields)))


;|-------------------------------------------------
;| CONVENIENCE

(def ^{:doc "Make field factories for map keys"}
  make-field-factories
  (partial make-factories produce-factory-parts-for-field))


(def ^{:doc "Make field factories for map values"}
  make-field-value-factories
  (partial make-factories produce-factory-parts-for-field-value))

