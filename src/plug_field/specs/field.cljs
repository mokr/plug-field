(ns plug-field.specs.field
  "Specs for field records and (field-m) maps that are passed to Field factory"
  (:require [clojure.spec.alpha :as s]
            [plug-field.specs.core :as $]))

;|-------------------------------------------------
;| Field attrs


(s/def ::attrs (s/keys :req []
                       :opt-un [::title]
                       ))

;|-------------------------------------------------
;| Field records and field-m maps (input to Field factory)

;; Keys in the Field record
(s/def ::k ::$/field-key)
(s/def ::react-key string?)
(s/def ::tag keyword?)
;(s/def ::display (s/nilable string?))
(s/def ::display (s/or :func fn?
                       :nil nil?
                       :string string?))
(s/def ::tooltip string?)
(s/def ::render fn?)

;; The map (field-m) passed to Field factory and also the final record
(s/def ::map (s/and record?
                    (s/keys :req-un [::k
                                     ::react-key
                                     ::tag
                                     ::render]
                            :opt-un [::attrs
                                     ::display
                                     ])))

;; While map for Field factory is being assembled
(s/def ::in-the-making (s/keys :opt-un [::k
                                        ::react-key
                                        ::tag
                                        ::render
                                        ::attrs
                                        ::display]))

;; The Field Record itself
(s/def ::record (s/and record?
                       ::map))

(s/def ::records (s/coll-of ::record))                      ;; [rec rec ,,,]

;;FIXME: Nested specs not allowed so this fails?
;(s/def ::rows-of-records (s/coll-of ::records))             ;; [[rec rec ,,,] [rec rec ,,,] ,,,] E.g. content rows for a table
(s/def ::rows-of-records (s/every ::records))               ;; [[rec rec ,,,] [rec rec ,,,] ,,,] E.g. content rows for a table