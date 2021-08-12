(ns plug-field.specs.config
  "Specs related to fields and field value config"
  (:require [clojure.spec.alpha :as s]
            [plug-field.specs.core :as $]))

;; TODO: k-tooltip vs v-tooltip. Applies to other keys as well. HAve a separtea config for the field as header instead? Do the latter!
;; IDEA: Introduce: :treat-a? But, that would require access to config for all fields, not just the current one. Make do with :lookup-as?

(def config-keys #{
                   :class
                   :custom-value-formatter
                   :description
                   :display                                 ;; A plain value to set it statically. E.g. for masking sensitive info with with "***"
                   ;:id
                   :lookup-as                               ;; This value should be looked up as eg :mobile (a phone number), :gt (Global title) or something other that
                   :lookup-formatter
                   :render
                   :source-field
                   :tag
                   :tooltip
                   :value-formatter
                   })

(s/def ::display (s/or :func fn?
                       ;:nil nil?
                       :string string?))
(s/def ::tooltip (s/or :func fn?
                       :string string?))
(s/def ::class (s/or :string string?
                     :coll (s/coll-of string?)))            ;; Reagent joins into string
(s/def ::lookup-as ::$/field-key)
(s/def ::dom-event-handler fn?)
(s/def ::on-click ::dom-event-handler)
;; The config applying to a single field. Se re-frame spec ns for config for multiple keys
(s/def ::field-config (s/keys :req-un []
                              :opt-un [::lookup-as
                                       ::class
                                       ::tooltip
                                       ::display
                                       ::on-click]))

(s/def ::field-configs (s/coll-of ::field-config))

(s/def ::multi-field-config (s/map-of ::$/field-key ::field-config))