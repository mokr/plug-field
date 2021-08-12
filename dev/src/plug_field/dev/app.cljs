(ns plug-field.dev.app
  (:require [plug-field.dev.pages.main :as main]
            [plug-field.re-frame :as pfrf]
            [plug-utils.re-frame :refer [<sub >evt]]
            [plug-field.dev.sample :as sample]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [re-frame.core :as rf]))



(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'main/page] (.getElementById js/document "app")))


(defn init-config []
  (rf/dispatch-sync [::pfrf/add-key-config sample/field-config])
  (rf/dispatch-sync [::pfrf/add-value-config sample/field-value-config])
  )


(defn start []
  (init-config)
  (mount-components))