(ns arcoiris.mcp-server
  (:require [arcoiris.chat-store :as store]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes POST GET]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn handle-post [args id]
  (let [{:keys [room_id poster_id body]} args]
    (when (and room_id poster_id body)
      (store/ensure-room room_id)
      (let [message (store/add-message room_id poster_id body)]
        {:jsonrpc "2.0"
         :id id
         :result {:content [{:type "text"
                           :text (str "Message posted with UID: " (:uid message))}]}}))))

(defn handle-read-latest [args id]
  (let [{:keys [room_id]} args]
    (when room_id
      (if-let [message (store/get-latest-message room_id)]
        {:jsonrpc "2.0"
         :id id
         :result {:content [{:type "text"
                           :text (str "Latest message in " room_id ":\n"
                                    "[" (:timestamp message) "] "
                                    (:poster-id message) ": "
                                    (:body message))}]}}
        {:jsonrpc "2.0"
         :id id
         :result {:content [{:type "text"
                           :text (str "No messages in room: " room_id)}]}}))))

(defn handle-read-since-last [args id]
  (let [{:keys [room_id poster_id]} args]
    (when (and room_id poster_id)
      (let [messages (store/get-messages-since-last-by-poster room_id poster_id)]
        (if (seq messages)
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                             :text (str "Messages since your last post in " room_id ":\n"
                                      (str/join "\n"
                                                (map (fn [msg]
                                                       (str "[" (:timestamp msg) "] "
                                                            (:poster-id msg) ": "
                                                            (:body msg)))
                                                     messages)))}]}}
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                             :text (str "No new messages since your last post in " room_id)}]}})))))

(defn handle-read-all [args id]
  (let [{:keys [room_id]} args]
    (when room_id
      (let [messages (store/get-all-messages room_id)]
        (if (seq messages)
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                             :text (str "All messages in " room_id ":\n"
                                      (str/join "\n"
                                                (map (fn [msg]
                                                       (str "[" (:timestamp msg) "] "
                                                            (:poster-id msg) ": "
                                                            (:body msg)))
                                                     messages)))}]}}
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                             :text (str "No messages in room: " room_id)}]}})))))

(defn handle-tools-call [params id]
  (let [tool-name (:name params)
        args (:arguments params)]
    (case tool-name
      "post" (handle-post args id)
      "read_latest" (handle-read-latest args id)
      "read_since_last" (handle-read-since-last args id)
      "read_all" (handle-read-all args id)
      {:jsonrpc "2.0"
       :id id
       :error {:code -32601
               :message "Method not found"}})))

(defn handle-mcp-request [request]
  (try
    (let [parsed (json/read-str request :key-fn keyword)]
      (case (:method parsed)
        "tools/call" (handle-tools-call (:params parsed) (:id parsed))
        "tools/list" {:jsonrpc "2.0"
                     :id (:id parsed)
                     :result {:tools [{:name "post"
                                     :description "Post a new message to a chat room"
                                     :inputSchema {:type "object"
                                                  :properties {:room_id {:type "string"
                                                                        :description "The room/channel ID where to post the message"}
                                                               :poster_id {:type "string"
                                                                          :description "Your identifier (e.g., agent name or user ID)"}
                                                               :body {:type "string"
                                                                     :description "The message content (supports markdown formatting)"}}
                                                  :required ["room_id" "poster_id" "body"]}}
                                    {:name "read_latest"
                                     :description "Get the most recent message from a room"
                                     :inputSchema {:type "object"
                                                  :properties {:room_id {:type "string"
                                                                        :description "The room/channel ID to read from"}}
                                                  :required ["room_id"]}}
                                    {:name "read_since_last"
                                     :description "Get all messages posted since your last message in a room"
                                     :inputSchema {:type "object"
                                                  :properties {:room_id {:type "string"
                                                                        :description "The room/channel ID to read from"}
                                                               :poster_id {:type "string"
                                                                          :description "Your identifier to check for your last message"}}
                                                  :required ["room_id" "poster_id"]}}
                                    {:name "read_all"
                                     :description "Get all messages from a room"
                                     :inputSchema {:type "object"
                                                  :properties {:room_id {:type "string"
                                                                        :description "The room/channel ID to read from"}}
                                                  :required ["room_id"]}}]}}
        "server/info" {:jsonrpc "2.0"
                      :id (:id parsed)
                      :result {:name "Arcoiris Chat Server"
                               :version "1.0.0"
                               :description "A multi-channel chat server with MCP and web interfaces"
                               :capabilities {:tools {:enabled true}}
                               :features ["Multi-channel chat rooms"
                                         "Markdown message support"
                                         "Automatic room creation"
                                         "Message persistence with timestamps"
                                         "Web interface on port 4001"
                                         "Real-time message sharing between MCP and web clients"]
                               :usage {:overview "This server provides tools for AI agents to participate in chat rooms alongside human users. Rooms are created automatically when the first message is posted."
                                      :room_management "Rooms are identified by string IDs and created automatically when needed. No explicit room creation is required."
                                      :message_format "Messages support markdown formatting and include timestamps, poster IDs, and unique message UIDs."
                                      :read_options "Three reading options are available: latest message, all messages since your last post, or all messages in a room."
                                      :web_interface "A web interface is available at http://localhost:4001 for human users to interact with the same chat rooms."}}}
        "initialize" {:jsonrpc "2.0"
                     :id (:id parsed)
                     :result {:protocolVersion "2024-11-05"
                              :capabilities {:tools {}}}}
        {:jsonrpc "2.0"
         :id (:id parsed)
         :error {:code -32601
                 :message "Method not found"}}))
    (catch Exception e
      {:jsonrpc "2.0"
       :error {:code -32700
               :message "Parse error"
               :data (.getMessage e)}})))

(defn mcp-handler [request]
  (let [body (:body request)
        body-str (slurp body)
        response (handle-mcp-request body-str)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str response)}))

(defn overview-page [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:jsonrpc "2.0"
                         :id 0
                         :result {:name "Arco Iris MCP Server"
                                 :version "1.0.0"
                                 :description "Multi-channel chat server with MCP interface"
                                 :endpoints {:mcp "POST / - JSON-RPC 2.0 endpoint"}
                                 :tools {:post {:description "Post a new message to a chat room"
                                               :parameters {:room_id "string - The room/channel ID where to post the message"
                                                           :poster_id "string - Your identifier (e.g., agent name or user ID)"
                                                           :body "string - The message content (supports markdown formatting)"}
                                               :required ["room_id" "poster_id" "body"]}
                                        :read_latest {:description "Get the most recent message from a room"
                                                     :parameters {:room_id "string - The room/channel ID to read from"}
                                                     :required ["room_id"]}
                                        :read_since_last {:description "Get all messages posted since your last message in a room"
                                                         :parameters {:room_id "string - The room/channel ID to read from"
                                                                     :poster_id "string - Your identifier to check for your last message"}
                                                         :required ["room_id" "poster_id"]}
                                        :read_all {:description "Get all messages from a room"
                                                  :parameters {:room_id "string - The room/channel ID to read from"}
                                                  :required ["room_id"]}}
                                 :web_interface "http://localhost:4001"
                                 :explanation {:overview "This server provides tools for AI agents to participate in chat rooms alongside human users. Rooms are created automatically when the first message is posted."
                                             :room_management "Rooms are identified by string IDs and created automatically when needed. No explicit room creation is required."
                                             :message_format "Messages support markdown formatting and include timestamps, poster IDs, and unique message UIDs."
                                             :read_options "Three reading options are available: latest message, all messages since your last post, or all messages in a room."
                                             :real_time "Messages posted via MCP appear immediately in the web interface and vice versa."}
                                 :current_rooms (vec (store/get-all-rooms))}})})

(defroutes mcp-routes
  (POST "/" [] mcp-handler)
  (GET "/" [] overview-page))

(def mcp-app mcp-routes)

(defn start-server [port]
  (println "MCP server starting on port" port)
  (jetty/run-jetty mcp-app {:port port :join? false})) 