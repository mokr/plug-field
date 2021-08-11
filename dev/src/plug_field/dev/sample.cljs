(ns plug-field.dev.sample
  (:require [plug-field.defaults :as defaults]
            [re-frame.core :as rf]
            [clojure.string :as str]))

(def target-fields
  [
   :user/name
   :user/age
   :company/city
   :company/name
   :user/vaccinated
   :some/details
   ])


(rf/reg-sub
  ::target-fields
  (fn [_ _]
    target-fields))


(def entries
  [{:db/id 1 :user/name "Bob" :user/age 23 :company/name "Acme" :company/city "Oslo" :user/vaccinated "no"}
   {:db/id 2 :user/name "Alice" :user/age 27 :company/name "Acme" :company/city "Oslo" :user/vaccinated "yes"}
   {:db/id 3 :user/name "Chuck" :user/age 16 :user/vaccinated "no"}
   {:db/id 4 :user/name "Liz" :user/age 51 :company/name "Google" :company/city "London" :user/vaccinated "yes"}])


(rf/reg-sub
  :sample/entities
  (fn [_ _]
    entries))


(def field-config
  {:company/city    {:display     "City"
                     :description "The city where user works"}
   :company/name    {:display "Company"}
   :user/age        {:display "Age"}
   :user/name       {:display "Name"}
   :some/details    {:display "Details"}
   :user/vaccinated {:display "Vaccinated?"}})


(rf/reg-sub
  ::fields-config
  (fn [_ _]
    field-config))


(def field-value-config
  {:user/age        {:tooltip "This is how old I am"}
   :user/name       {:tooltip (fn [entity]
                                (str "Name in uppercase: " (str/upper-case (:user/name entity))))}
   :company/city    {:tooltip  (fn [entity field-m]
                                 (str "field-m:\n" field-m))
                     :on-click #(js/console.info "Click event:" %)}
   :company/name    {:class    "has-text-primary"
                     ;:on-click #(js/console.info "Clicked" %)
                     :on-click (fn [entity event]
                                 (js/console.info "Clicked entity" entity))
                     }
   :user/vaccinated {:_custom  "Data we might need in custom handler"
                     :on-click (fn [entity cfg event]
                                 (js/console.info "Clicked entity" entity "with cfg:" cfg)
                                 )
                     :tooltip  "Private info"
                     :display  "***"}
   :some/details    {:display     "debug"
                     :description "Static description"
                     ;:tooltip (fn [m]
                     ;           (str "Just a tip\n" (:description m)))
                     :render      (fn [this attrs]
                                    (vector :td attrs "<details>"))
                     }
   })


(rf/reg-sub
  ::field-values-config
  (fn [_ _]
    field-value-config))


(rf/reg-sub
  ::field-defaults
  (fn [_ _]
    defaults/field-defaults))


(rf/reg-sub
  ::common-content-config
  (fn [_ _]
    {:tag :td}))

(rf/reg-sub
  ::common-header-config
  (fn [_ _]
    {:tag :th}))

(comment
  ;(sort (keys field-value-config))
  )