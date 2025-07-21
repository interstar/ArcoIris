(ns arcoiris.core
  (:require [arcoiris.mcp-server :as mcp]
            [arcoiris.web-server :as web]
            [arcoiris.chat-store :as store])
  (:gen-class))

(defn -main [& args]
  (let [[mcp-port web-port] (map #(Integer/parseInt %) args)]
    (println "Starting Arcoiris chat server...")
    (println "MCP server on port:" mcp-port)
    (println "Web server on port:" web-port)
    
    ;; Initialize the chat store
    (store/init!)
    
    ;; Start both servers
    (mcp/start-server mcp-port)
    (web/start-server web-port)
    
    (println "Servers started successfully!")
    (println "Press Ctrl+C to stop"))) 