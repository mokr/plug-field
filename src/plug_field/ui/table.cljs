(ns plug-field.ui.table
  (:require
    [re-frame.core :as rf]))

;|-------------------------------------------------
;| HELPERS

(defn- class-when [cfg k]
  (when-let [classes (get cfg k)]
    {:class classes}))


;|-------------------------------------------------
;| DEFAULT TABLE CONFIGS (styling, ...)
;| - Note: plug-field.re-frame have subscriptions for easy access to this data

(def default-config-bulma
  "Default config for a plug-field table using Bulma for styling"
  {:wrapper-classes     ["table-container"]
   :table-classes       ["table" "is-narrow" "is-striped" "is-hoverable"]
   :tbody-classes       nil
   :thead-classes       nil
   :content-row-classes nil
   :header-row-classes  nil})


(defn- config-selection-with-override-support
  "Performs flexible config selection
  []               => use defaults for Bulma
  [type]           => LATER: Pass e.g :bootstrap to get Bootstrap styling instead of Bulma
  [overrides]      => Use Bulma defaults, but with some modifications
  [type overrides] => LATER: Use e.g. Bootstrap but with some modifications

  So:
  'type' is a keyword like :bulma and :bootstrap
  'overrides' is a config map that will be merged in to the defaults "
  [_ [_ arg1 arg2]]
  (cond
    (nil? arg1) default-config-bulma                        ; use default config
    (map? arg2) (case arg1                                  ; use specific default with overrides merged in.
                  :bulma (merge default-config-bulma arg2)
                  ;:bootstrap (merge default-config-bootstrap arg2)
                  (merge default-config-bulma arg2))
    (map? arg1) (merge default-config-bulma arg1)))


(rf/reg-sub
  ::default-config
  config-selection-with-override-support)                   ; use default with overrides merged in


;|-------------------------------------------------
;| TABLE PARTS

(defn header-row [fields cfg]
  [:tr (class-when cfg :header-row-classes)
   (for [field fields]
     ^{:key (:react-key field)}
     [field])])


(defn head-section [{:keys [fields react-key]} cfg]
  [:thead (class-when cfg :thead-classes)
   ^{:key react-key}
   [header-row fields]])


(defn content-row [fields cfg]
  [:tr (class-when cfg :content-row-classes)
   (for [field fields]
     ^{:key (:react-key field)}
     [field])])


(defn body-section [row-entities cfg]
  [:tbody (class-when cfg :tbody-classes)
   (for [{:keys [fields react-key]} row-entities]
     ^{:key react-key}
     [content-row fields])])


;|-------------------------------------------------
;| TABLE COMPONENT

(defn component [{:keys [header-row content-rows cfg]}]
  [:div (class-when cfg :wrapper-classes)
   [:table (class-when cfg :table-classes)
    [head-section header-row cfg]
    [body-section content-rows cfg]]])

