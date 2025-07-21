(ns arcoiris.chat-store
  (:require [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.string :as str]))

;; In-memory storage
(def rooms (atom {}))

(defn generate-uid []
  (str (System/currentTimeMillis) "-" (rand-int 10000)))

(defn format-timestamp [timestamp]
  (time-format/unparse (time-format/formatters :date-time) timestamp))

(defn init! []
  (reset! rooms {}))

(defn get-room [room-id]
  (get @rooms room-id))

(defn ensure-room [room-id]
  (when-not (get @rooms room-id)
    (swap! rooms assoc room-id {:id room-id :messages []}))
  (get @rooms room-id))

(defn add-message [room-id poster-id body]
  (let [message {:uid (generate-uid)
                 :poster-id poster-id
                 :timestamp (time/now)
                 :body body}]
    (swap! rooms update-in [room-id :messages] conj message)
    message))

(defn get-latest-message [room-id]
  (let [room (get-room room-id)]
    (when room
      (last (:messages room)))))

(defn get-messages-since-last-by-poster [room-id poster-id]
  (let [room (get-room room-id)]
    (when room
      (let [messages (:messages room)
            last-poster-message (last (filter #(= (:poster-id %) poster-id) messages))]
        (if last-poster-message
          (let [last-index (.indexOf messages last-poster-message)]
            (subvec messages (inc last-index)))
          messages)))))

(defn get-all-messages [room-id]
  (let [room (get-room room-id)]
    (when room
      (:messages room))))

(defn get-all-rooms []
  (keys @rooms))

(defn export-room-transcript [room-id]
  (let [room (get-room room-id)]
    (when room
      (str "Room: " room-id "\n"
           "Messages: " (count (:messages room)) "\n"
           "---\n\n"
           (str/join "\n\n"
                     (map (fn [msg]
                            (str "[" (format-timestamp (:timestamp msg)) "] "
                                 (:poster-id msg) ": "
                                 (:body msg)))
                          (:messages room))))))) 