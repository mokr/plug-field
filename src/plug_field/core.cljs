(ns plug-field.core
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [rename-keys]]
            [plug-field.defaults :as defaults]
            [plug-field.specs.core :as $]
            [plug-field.specs.config :as $cfg]
            [plug-field.specs.field :as $field]))


(defrecord Field [k display render tag]
  IFn
  (-invoke [{:keys [render attrs] :as this}]
    (render this attrs))
  (-invoke [{:keys [render attrs] :as this} attrs-override]
    (render this (merge attrs attrs-override))))


;|-------------------------------------------------
;| CONFIG COMPILATION

(defn- add-raw-value
  "A function that will ensure raw value :v is added to every field (Field) so that 'deciders' can utilize its value"
  [{:keys [k] :as field-m} entity]
  {:pre  [(s/valid? ::$/entity entity)
          (s/valid? ::$field/in-the-making field-m)]
   :post [(s/valid? ::$field/in-the-making %)]}
  (assoc field-m :v (get entity k)))


(defn- decide-tooltip
  "Return either a function that will add a :title attr to field map,
  or nil to signal that tooltip creation can be skipped for this field."
  [{:keys [tooltip source-field] :as field-value-cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (cond
    (fn? tooltip) (fn [field-m entity]
                    (assoc-in field-m [:attrs :title] (tooltip entity)))
    (some? tooltip) (fn [field-m _]
                      (assoc-in field-m [:attrs :title] tooltip))
    :else nil))


(defn- decide-display-value
  "Decide on the value that will actually be displayed when this field is rendered."
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
                    ;(println "v field-m" field-m)
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
      (fn [field-m _]
        (assoc-in field-m [:attrs :class] class)))))


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


(defn- arity
  "Find the arity of a function.
  Works on both named and anonymous functions"
  [function]
  (.-length function))


(defn- decide-handler
  "Allow for custom event handler functions with multiple arities
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
        (fn [field-m entity]
          (assoc-in field-m [:attrs handler-key] handler-fn))))))


(defn- decide-renderer
  "Typically we use the default renderer, but here we allow for a custom one.
  Typically for things like identicon and synthetic fields"
  [{:keys [render] :as cfg}]
  {:pre  []
   :post [(s/valid? ::$/factory-part %)]}
  (when (some? render)
    {:render render}))                                      ;; Call the render provided in config.


(defn- make-factory-parts-from-a-field-value-config
  "Turn the config for a given field value into collection of parts (maps & fns) needed to
  assemble a field according to config."
  [field-value-cfg field-defaults]
  (->> [field-defaults
        {:k (:k field-value-cfg)}                           ;; Note: It's called :k instead of :field to avoid confusing with the Field we create for it
        add-raw-value                                       ;; Allways. Do early so others can use it
        (decide-display-value field-value-cfg)
        (decide-tooltip field-value-cfg)                    ;; After :display as we might want to use info that was looked up
        (decide-class field-value-cfg)
        (decide-react-key-attr field-value-cfg)
        (decide-tag field-value-cfg)
        (decide-handler :on-click field-value-cfg)
        (decide-renderer field-value-cfg)]
       (remove nil?)                                        ;; Remove nils caused by config possibilities not utilized by the field in question.
       (group-by fn?)
       ;(rename-keys)
       ))


;|-------------------------------------------------
;| PUBLIC

(defn field-value-config->field-factory
  "Create factory function for producing a single field.
  That is, a function that will return and instance of Field when passed an entity (map).

  This avoids re-evaluating the config pieces over and over for each entity that is to be processed.

  Note: See the implementation of the functions used to see how config settings are combined and utilized."
  [field-value-cfg field-defaults]
  {:pre  []
   :post [(s/valid? ::$/factory %)]}
  ;; NOTE: Sequence of deciders will matter here as later fns will see updated field-m (the map that we will pass to Field factory)
  (let [{functions true                                     ;; Destructuring output of grouped result into 'functions' and 'maps'.
         maps      false} (make-factory-parts-from-a-field-value-config field-value-cfg field-defaults) ;; Separate functions from maps so that the latter can go into the field-basis (accumulator map) used in reduce below.
        field-m (apply merge maps)]                         ;; Base field on generic field defaults and whatever maps where returned above, before letting the returned functions alter it further.
    ;; Return a factory function
    (fn [entity]
      (->> functions                                        ;; Take all the functions that will help prepare a field according to config. We use functions when we need data from the entity itself.
           (reduce (fn [field-m func]                       ;; And apply each of them to the field map we have so far and the entity this field belongs to.
                     (func field-m entity))
                   field-m)                                 ;; TODO: Look into making this a transient and update all decide-* fns to use assoc! OBS: there is no assoc-in!
           (map->Field)))))


(defn field-value-configs->field-factories
  "Create collection of field factories.

  Compiles config for each of the desired fields into fields makers (functions creating a Field from an entity).

  For each field, turn its config into a description of how to create Fields from based on entity maps."
  [fields field-value-configs common-field-value-config field-defaults]
  {:pre  [(sequential? fields)
          (map? field-value-configs)
          (map? common-field-value-config)
          ;(s/valid? ::$cfg/field-value-configs field-value-configs) ;;FIXME: The full config map, not just for a single field
          ]
   :post [(s/valid? ::$/factories %)]}
  (let [xf (comp
             (map #(-> (merge common-field-value-config     ;; Contains e.g. :lookup and other features all configs should have access to. Note: A specific config can override are it it merge on top.
                              (get field-value-configs %))  ;; Get the config for the field in question ..
                       (assoc :k %)))                       ;; add update that config with the field name.
             (map #(field-value-config->field-factory % defaults/field-defaults)))]
    ;; Create a collection of maps and functions that together will describe how to produce a given field from its config
    (into [] xf fields)))

