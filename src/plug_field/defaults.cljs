(ns plug-field.defaults)



(def field-defaults
  {:tag     :div
   :tooltip (fn [this entity cfg]
              (or (:description this) (:v this)))
   :render  (fn [this attrs]                                ;; NOTE: Implementation of -invoke (IFn) in Record takes care of passing in attrs (incl merging in any fresh attrs not set by field config)
              [(:tag this) attrs (:display this)])})

