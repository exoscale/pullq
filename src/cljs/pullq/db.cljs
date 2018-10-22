(ns pullq.db)

(def default-db
  {:only          {:author nil :repo nil}
   :hidden-labels ["wip" "hold"]
   :filter        :open
   :order         :updated
   :search        ""
   :pulls         []})
