(ns plug-field.dev.repl
  (:require [clojure.string :as str]
            [plug-field.defaults :as defaults]
            [plug-field.core :as pf :refer [field-value-config->field-factory]]))




(comment
  (let [entity   {:user/name "Bob" :age 42}
        ;config   {:field :user/name :class "sel" :display "FOO"}
        config   {:k       :user/name :class "sel"
                  :display (fn [this entity cfg]
                             (str/upper-case (:v this))
                             ;(str entity)
                             )}
        defaults defaults/field-defaults
        ]
    ;(field-value-config->field-maker config defaults)
    ((field-value-config->field-factory config defaults) entity)
    ;(((field-value-config->field-maker config defaults) entity))
    )

  (map->Field {})

  ((field-value-config->field-factory {} {}) {})
  ((field-value-config->field-factory {} {:tag :div}) {})

  ((field-value-config->field-factory {:field :user/name :class "sel" :display "FOO"} {:tag :div}) {:user/name "Bob"})

  (field-value-configs->field-makers [:foo :bar] {:foo {} :bar {}})


  (let [{funcs true
         maps  false} (group-by fn? [#(hash-map :a 1) (constantly ":)") {:id "foo"}])]
    (println "funcs" funcs)
    (println "maps" maps)
    )

  )