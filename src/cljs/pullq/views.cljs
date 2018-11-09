(ns pullq.views
  (:require
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [pullq.events :as events]
   [pullq.subs :as subs]
   [soda-ash.core :as sa]
   [reagent.core :as reagent]
   [cljsjs.moment]))

(def exoscale-logo
  "https://www.exoscale.com/static/img/logo-exoscale-white-201711.svg")

(def enter-key
  13)

(defn epoch->age
  [epoch]
  (.fromNow (js/moment (* 1000 epoch))))

(defn header-menu
  []
  [sa/Menu {:inverted true}
   [sa/Container
    [sa/MenuItem
     [sa/Image {:size "small" :src exoscale-logo}]]
    [sa/MenuItem "Pull Request Queue"]]])

(defn repo-entry
  [repo only]
  [sa/MenuItem {:on-click #(re-frame/dispatch [::events/toggle :repo repo])
                :active   (= only repo)
                :as       "a"}
   repo])

(defn author-entry
  [[login avatar] only]
  [sa/MenuItem {:on-click #(re-frame/dispatch [::events/toggle :author login])
                :active   (= only login)
                :as       "a"}
   [:div
    [sa/Image {:avatar true :size "mini" :src avatar}]
    [:span login]]])

(defn hidden-label-menu
  []
  (let [input         (reagent/atom "")
        hidden-labels (re-frame/subscribe [::subs/hidden-labels])]
    (fn []
      [sa/MenuItem
       [sa/MenuHeader "Hidden Labels"]
       (into
        [sa/MenuMenu
         [sa/MenuItem
          [sa/Input
           {:icon         "filter"
            :placeholder  "Hide label..."
            :value        @input
            :on-change    #(reset! input (-> % .-target .-value))
            :on-key-press (fn [e]
                            (when (= enter-key (.-charCode e))
                              (let [val @input]
                                (reset! input "")
                                (re-frame/dispatch
                                 [::events/hide-label val]))))}]]]
        (for [label @hidden-labels]
          [sa/MenuItem label [sa/Icon {:on-click #(re-frame/dispatch
                                                   [::events/show-label label])
                                       :name     "delete"}]]))])))

(def filter-colors
  {:bug   "red"
   :open  "yellow"
   :ready "blue"})

(defn filter-counter
  [current-filter filter count]
  [sa/MenuItem
   {:as       "a"
    :active   (= current-filter filter)
    :on-click #(re-frame/dispatch [::events/set-filter filter])}
   [sa/Label {:class (when (pos? count) (get filter-colors filter))} count]
   (-> filter name str/capitalize)])

(defn right-menu
  []
  (let [stats (re-frame/subscribe [::subs/menu-stats])]
    (fn []
      (let [{:keys [filter search open bug ready repos order authors only]} @stats]
        [:div
         [sa/Menu {:vertical true :fluid true}
          [filter-counter filter :bug bug]
          [filter-counter filter :open open]
          [filter-counter filter :ready ready]
          [sa/MenuItem
           [sa/Input
            {:icon        "search"
             :placeholder "Search pull requests..."
             :on-change   #(re-frame/dispatch [::events/set-search
                                               (-> % .-target .-value)])
             :value       search}]]]
         [sa/Menu {:vertical true :fluid true}
          [sa/MenuItem
           {:as "a"}
           "Refresh"
           [sa/Icon {:name "refresh"
                     :on-click #(re-frame/dispatch [::events/refresh-db])}]]
          [sa/MenuItem
           [sa/MenuHeader "Sort Order"]
           [sa/MenuMenu
            [sa/MenuItem
             {:as       "a"
              :active   (= order :age)
              :on-click #(re-frame/dispatch [::events/set-order :age])}
             "Age"]
            [sa/MenuItem
             {:as       "a"
              :active   (= order :updated)
              :on-click #(re-frame/dispatch [::events/set-order :updated])}
             "Updated"]]]
          [hidden-label-menu]
          [sa/MenuItem
           [sa/MenuHeader "Repos"]
           (into [sa/MenuMenu] (map repo-entry repos (repeat (:repo only))))]
          [sa/MenuItem
           [sa/MenuHeader "Authors"]
           (into [sa/MenuMenu] (map author-entry authors (repeat (:author only))))]]]))))

(defn state->icon
  [state]
  (get {:comment "discussions"
        :needs-changes "delete"
        :approved "checkmark"}
       state
       "help"))

(defn review-entry
  [{:keys [avatar state url]}]
  [sa/ListItem {:as "a" :href url}
   [sa/Image {:avatar true :src avatar}]
   [sa/ListContent
    [sa/ListHeader [sa/Icon {:name (state->icon state)}]]]])

(defn request-row
  [{:keys [repo avatar url title status reviews created status updated login labels]}]
  [sa/TableRow
   [sa/TableCell [sa/CommentGroup
                  [sa/CommentSA
                   [sa/CommentAvatar {:src avatar :size "mini" :alt login}]
                   [sa/CommentContent
                    [sa/CommentAuthor {:as "a" :href (:url repo)}
                     (:name repo)]
                    [sa/CommentMetadata [:div (epoch->age created)]]
                    [sa/CommentText
                     [:a {:href url} title]]]]]]
   [sa/TableCell
    (into [sa/ListSA {:size "mini" :horizontal true :divided true}]
          (map review-entry reviews))]
   [sa/TableCell
    (let [{:keys [open? counter type color]} status
          button-opts                        {:color color
                                              :compact true
                                              :as "div"
                                              :fluid true
                                              :size "mini"}
          type-button                        [sa/Button button-opts
                                              [:small type]]]
      [:div
       (if open?
         [sa/Button {:as "div" :label-position "right" :fluid true}
          [sa/Label {:basic true :pointing "right"} [:small (str counter)]]
          type-button]
         type-button)
       [:div {:style {:color "grey"}}
        [:small (epoch->age updated)]]])]])

(defn request-table
  []
  (let [pulls (re-frame/subscribe [::subs/pulls])]
    (fn []
      [sa/Table {}
       [sa/TableHeader
        [sa/TableRow
         [sa/TableHeaderCell "Title"]
         [sa/TableHeaderCell "Reviewers"]
         [sa/TableHeaderCell "Status"]]]
       (into [sa/TableBody] (map request-row @pulls))])))

(defn main-panel []
  [:div
   [header-menu]
   [sa/Container
    [sa/Grid
     [sa/GridRow
      [sa/GridColumn {:width 13} [request-table]]
      [sa/GridColumn {:width 3} [right-menu]]]]]])
