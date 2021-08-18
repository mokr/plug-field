(ns plug-field.ui.vtable)

;|-------------------------------------------------
;| HELPERS

(defn- class-when [cfg k]
  (when-let [classes (get cfg k)]
    {:class classes}))


;|-------------------------------------------------
;| VTABLE PARTS

(defn- vtable-row [fields cfg]
  [:tr (class-when cfg :row-classes)
   (for [field fields]
     ^{:key (:react-key field)}
     [field])])


;|-------------------------------------------------
;| VERTICAL TABLE COMPONENT

(defn component
  "Create a vertical table using a HTML table structure"
  [{:keys [rows cfg]}]
  [:div (class-when cfg :wrapper-classes)
   [:table (class-when cfg :table-classes)
    [:tbody
     (for [{:keys [fields react-key]} rows]
       ^{:key react-key}
       [vtable-row fields cfg])]]])