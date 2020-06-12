(ns pullq.test-main
  (:require [clojure.test :refer :all]
            [pullq.main :refer :all]))

(defn make-raw-review
  [username time state]
  {:state state
   :html_url "https://example.com"
   :submitted_at "2019-02-15T10:50:34Z"
   :user {:login username
          :avatar_url (format "https://example.com/avatar/%s" username)}})

(deftest pull-reviews-test
  (testing "approving overrides needs fixing"
    (is (= :approved
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "CHANGES_REQUESTED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "APPROVED")]))))))

  (testing "needs fixing overrides approvals"
    (is (= :needs-changes
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "APPROVED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "CHANGES_REQUESTED")]))))))

  (testing "comment-only reviews count as commenting"
    (is (= :comment
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "COMMENTED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "COMMENTED")]))))))

  (testing "commenting is overwritten by approvals"
    (is (= :approved
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "COMMENTED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "APPROVED")]))))))

  (testing "commenting is overwritten by needs fixing"
    (is (= :needs-changes
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "COMMENTED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "CHANGES_REQUESTED")]))))))

  (testing "commenting doesn't override approvals"
    (is (= :approved
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "APPROVED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "COMMENTED")]))))))

  (testing "commenting doesn't override needs fixing"
    (is (= :needs-changes
           (:state (first (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "CHANGES_REQUESTED")
                                         (make-raw-review "testuser" "2019-02-15T10:50:35Z" "COMMENTED")]))))))

  (testing "multiple reviews from multiple users"
    (let [result (pull-reviews [(make-raw-review "testuser" "2019-02-15T10:50:34Z" "APPROVED")
                                (make-raw-review "testuser2" "2019-02-15T10:50:35Z" "CHANGES_REQUESTED")
                                (make-raw-review "testuser" "2019-02-15T10:51:35Z" "CHANGES_REQUESTED")
                                (make-raw-review "testuser" "2019-02-15T10:51:35Z" "COMMENTED")
                                (make-raw-review "testuser" "2019-02-15T10:52:35Z" "APPROVED")
                                (make-raw-review "testuser2" "2019-02-15T10:52:33Z" "APPROVED")
                                (make-raw-review "testuser3" "2019-02-15T10:52:33Z" "COMMENTED")
                                (make-raw-review "testuser3" "2019-02-15T10:52:33Z" "APPROVED")
                                (make-raw-review "testuser3" "2019-02-15T10:52:33Z" "COMMENTED")])]

      (is (= 3 (count result)))
      (is (= [:approved :approved :approved] (map :state result))))))
