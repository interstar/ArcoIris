(ns arcoiris.chat-store-test
  (:require [clojure.test :refer :all]
            [arcoiris.chat-store :as store]))

(deftest test-chat-store-basics
  (testing "Basic chat store operations"
    (store/init!)
    
    ;; Test room creation
    (is (nil? (store/get-room "test-room")))
    (store/ensure-room "test-room")
    (is (not (nil? (store/get-room "test-room"))))
    
    ;; Test message posting
    (let [message (store/add-message "test-room" "test-user" "Hello, world!")]
      (is (contains? message :uid))
      (is (contains? message :poster-id))
      (is (contains? message :timestamp))
      (is (contains? message :body))
      (is (= "test-user" (:poster-id message)))
      (is (= "Hello, world!" (:body message))))
    
    ;; Test getting latest message
    (let [latest (store/get-latest-message "test-room")]
      (is (not (nil? latest)))
      (is (= "Hello, world!" (:body latest))))
    
    ;; Test getting all messages
    (let [all-messages (store/get-all-messages "test-room")]
      (is (= 1 (count all-messages)))
      (is (= "Hello, world!" (:body (first all-messages))))))) 