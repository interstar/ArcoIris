(ns arcoiris.web-server
  (:require [arcoiris.chat-store :as store]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.util.response :refer [redirect]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [markdown.core :as markdown]
            [clojure.string :as str]))

(defn format-timestamp [timestamp]
  (str timestamp))

(defn message-html [message]
  [:div.message
   [:div.message-header
    [:strong (:poster-id message)]
    [:span.text-muted.ms-2 (format-timestamp (:timestamp message))]
    [:small.text-muted.ms-2 (str "UID: " (:uid message))]]
   [:div.message-body.markdown-body
    (markdown/md-to-html-string (:body message))]])

(defn room-styles []
  [:style "
    .markdown-body { box-sizing: border-box; min-width: 200px; max-width: 980px; margin: 0 auto; padding: 45px; }
    @media (max-width: 767px) { .markdown-body { padding: 15px; } }
    .message { margin-bottom: 1rem; padding: 0.5rem; border-left: 3px solid #007bff; }
    .message-header { font-size: 0.9rem; color: #666; margin-bottom: 0.5rem; }
    .message-body { margin: 0; }
  "])

(defn room-head [room-id]
  [:head
   [:title (str "Chat Room: " room-id)]
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css")
   (include-css "https://cdn.jsdelivr.net/npm/github-markdown-css@5.1.0/github-markdown.min.css")
   (room-styles)])

(defn post-form [room-id]
  [:div.mb-4
   [:form {:method "POST" :action (str "/room/" room-id "/post")}
    [:div.row
     [:div.col-md-3
      [:input.form-control {:type "text" :name "poster_id" :id "poster-id" :placeholder "Your name" :required true}]]
     [:div.col-md-7
      [:textarea.form-control {:name "body" :id "message-body" :placeholder "Your message (markdown supported)" :required true :rows 3}]]
     [:div.col-md-2
      [:button.btn.btn-primary {:type "submit"} "Post"]]]]])

(defn room-actions [room-id]
  [:div.mb-3
   [:a.btn.btn-secondary.btn-sm {:href "/"} "← Back to rooms"]
   [:a.btn.btn-outline-secondary.btn-sm.ms-2 {:href (str "/room/" room-id "/export")} "Export transcript"]])

(defn messages-section [messages]
  [:div#messages
   (if (seq messages)
     (map message-html messages)
     [:p.text-muted "No messages yet. Be the first to post!"])])

(defn room-body [room-id messages]
  [:body
   [:div.container-fluid
    [:div.row
     [:div.col-md-8.offset-md-2
      [:h1.mt-4 (str "Chat Room: " room-id)]
      (post-form room-id)
      (room-actions room-id)
      (messages-section messages)]]]
   (include-js "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js")
         [:script "
        // Load saved username from localStorage
        document.addEventListener('DOMContentLoaded', function() {
          var savedName = localStorage.getItem('arcoiris_username');
          if (savedName) {
            document.getElementById('poster-id').value = savedName;
          }
          
          // Save username when it changes
          document.getElementById('poster-id').addEventListener('change', function() {
            localStorage.setItem('arcoiris_username', this.value);
          });
          
          // Update messages without page reload
          function updateMessages() {
            fetch(window.location.href)
              .then(response => response.text())
              .then(html => {
                var parser = new DOMParser();
                var doc = parser.parseFromString(html, 'text/html');
                var newMessages = doc.getElementById('messages');
                var currentMessages = document.getElementById('messages');
                if (newMessages && currentMessages && newMessages.innerHTML !== currentMessages.innerHTML) {
                  currentMessages.innerHTML = newMessages.innerHTML;
                }
              })
              .catch(error => console.log('Error updating messages:', error));
          }
          
          // Update messages every 5 seconds without losing focus
          setInterval(updateMessages, 5000);
        });
      "]])

(defn room-page [room-id]
  (let [room (store/get-room room-id)
        messages (if room (:messages room) [])]
    (html5
     (room-head room-id)
     (room-body room-id messages))))

(defn rooms-list-page []
  (let [rooms (store/get-all-rooms)]
    (html5
     [:head
      [:title "Arco Iris Chat Rooms"]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      (include-css "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css")]
     [:body
      [:div.container
       [:div.row
        [:div.col-md-8.offset-md-2
         [:h1.mt-4 "Arco Iris Chat Rooms"]
         
         [:div.mb-4
          [:form {:method "POST" :action "/room/create"}
           [:div.row
            [:div.col-md-8
             [:input.form-control {:type "text" :name "room_id" :placeholder "Room ID" :required true}]]
            [:div.col-md-4
             [:button.btn.btn-primary {:type "submit"} "Create Room"]]]]]
         
         [:div#rooms
          (if (seq rooms)
            [:div.list-group
             (for [room-id rooms]
               [:a.list-group-item.list-group-item-action {:href (str "/room/" room-id)}
                [:h5.mb-1 room-id]
                [:small.text-muted "Click to join"]])]
            [:p.text-muted "No rooms yet. Create the first one!"])]]]]])))

(defroutes app-routes
  (GET "/" [] (rooms-list-page))
  (POST "/room/create" [room_id] 
    (do
      (store/ensure-room room_id)
      (redirect (str "/room/" room_id))))
  (GET "/room/:room-id" [room-id] (room-page room-id))
  (POST "/room/:room-id/post" [room-id poster_id body]
    (do
      (store/ensure-room room-id)
      (store/add-message room-id poster_id body)
      (redirect (str "/room/" room-id))))
  (GET "/room/:room-id/export" [room-id]
    (let [transcript (store/export-room-transcript room-id)]
      (if transcript
        {:status 200
         :headers {"Content-Type" "text/plain"
                   "Content-Disposition" (str "attachment; filename=\"" room-id "-transcript.txt\"")}
         :body transcript}
        {:status 404 :body "Room not found"})))
  (route/not-found "Page not found"))

(def app
  (-> app-routes
      wrap-keyword-params
      wrap-params))

(defn start-server [port]
  (println "Web server starting on port" port)
  (jetty/run-jetty app {:port port :join? false})) 