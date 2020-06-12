(ns pullq.db)

(def default-db
  {:only          {:author nil :repo nil}
   :hidden-labels ["wip" "hold" "content"]
   :filter        :open
   :order         :updated
   :sort-dir      <
   :search        ""
   :pulls         []})
