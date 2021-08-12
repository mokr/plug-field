(ns plug-field.defaults
  "Some defaults that are handy as a starting point when working with Fields.
  Notes:
  - Subscriptions to these are available when requiring [plug-field.re-frame]
  - Defaults for e.g. [plug-field.ui.table] is in that specific ns (incl re-frame subsriptions).")


(def field-defaults
  "Basis for field of any kind"
  {:tag     :div
   :tooltip (fn [this entity cfg]
              (or (:description this) (:v this)))
   :render  (fn [this attrs]                                ;; NOTE: Implementation of -invoke (IFn) in Record takes care of passing in attrs (incl merging in any fresh attrs not set by field config)
              [(:tag this) attrs (:display this)])})


(def common-content-config
  "Config that is common to all content fields.
  That is, fields that represent the value of a given key/attr in a map"
  {:tag :td})


(def common-header-config
  "Config that is common to all content fields.
  That is, fields that represent the key/attr itself (typically as a header)"
  {:tag :th})
