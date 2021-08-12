(ns plug-field.specs.re-frame
  "Specs for the re-frame utils of plug-field"
  (:require [clojure.spec.alpha :as s]
            [plug-field.specs.core :as $]
            [plug-field.specs.config :as $cfg]))


(s/def ::domain (s/or :keyword keyword?
                      :string string?))

(s/def ::maybe-domain (s/nilable ::domain))

(s/def ::domain-or-multi-field-config (s/or :domain ::domain
                                            :multi-field-config ::$cfg/multi-field-config))

(s/def ::maybe-multi-field-config (s/nilable ::$cfg/multi-field-config))