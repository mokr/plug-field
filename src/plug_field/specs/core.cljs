(ns plug-field.specs.core
  (:require [clojure.spec.alpha :as s]))


;; Mote:
;;   Technically an entities key/field/attr could be any type allowed as a map key,
;;   but in practice it is likely to be more limited and hence easier to spec
(s/def ::field-key (s/or :keyword keyword?
                         :string string?))
(s/def ::entity (s/or :map map?                             ;; Typically a map, but ..
                      :record record?))                     ;; .. could be a record if we generate Fields from e.g. domain entities
(s/def ::entities (s/coll-of ::entity))
(s/def ::target-fields (s/coll-of ::field-key))

;|-------------------------------------------------
;| FACTORY (parts)
;| - The parts needed in a given factory to create a Field according to config

;; When we compile config into factory parts, we get either, map, fn or nil back.
(s/def ::factory-part (s/nilable
                        (s/or :map map?                     ;; Merged into default/acc for a field. Same data for multiple entities
                              :func fn?)))                  ;; Function who's output depends on actual entity

(s/def ::factory-parts (s/coll-of ::factory-part))

(s/def ::factory fn?)
(s/def ::factories (s/coll-of ::factory))