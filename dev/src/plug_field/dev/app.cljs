(ns plug-field.dev.app
  (:require [plug-field.dev.pages.main :as main]
            [reagent.dom :as rdom]
            [reagent.core :as r]
            [re-frame.core :as rf]))



(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'main/page] (.getElementById js/document "app")))


(defn init []
  (mount-components))