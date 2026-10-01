;; Copyright © 2026 JoinIn AI, Inc. Released under the MIT License (see LICENSE).
(ns example.log
  "Structured logging — (log level event-name meta-map).
   JSON lines in production, readable Clojure maps in local dev.

   This is the whole \"logging utility\" from the talk: agents parse JVM stack
   traces fine, but one line per event with a data map beats a wall of text —
   for the agent grepping a log file exactly as much as for you."
  (:require [taoensso.timbre :as timbre]
            [clojure.data.json :as json]))

(defn log
  "Log a structured event.

   Usage:
     (log :info  \"event_stored\"     {:event-id eid :entity-id id})
     (log :warn  \"validation_failed\" {:error msg})
     (log :error \"store_failed\"     {:event-id eid :error (.getMessage e)})"
  [level event-name meta-map]
  (timbre/log level (assoc meta-map :event event-name)))

(defonce ^:private service-name-ref (atom "unknown"))

(defn- json-output-fn
  "Production output: one JSON object per line (Cloud Logging friendly)."
  [{:keys [level msg_ ?err]}]
  (let [data (force msg_)]
    (json/write-str
      (cond-> (if (map? data) data {:msg (str data)})
        true (assoc :level (name level)
                    :service @service-name-ref
                    :timestamp (.toString (java.time.Instant/now)))
        ?err (assoc :error (.getMessage ?err)
                    :stack (with-out-str (.printStackTrace ?err)))))))

(defn- dev-output-fn
  "Local dev output: readable Clojure maps."
  [{:keys [level msg_ ?err]}]
  (let [data (force msg_)]
    (str (cond-> (if (map? data) data {:msg data})
           true (assoc :level (name level) :service @service-name-ref)
           ?err (assoc :error (.getMessage ?err))))))

(defn init!
  "Configure Timbre for the given service name.
   Call once at startup before any logging. APP_ENV=local switches to the
   readable dev output."
  [service-name]
  (reset! service-name-ref service-name)
  (let [local? (= "local" (System/getenv "APP_ENV"))]
    (timbre/merge-config!
      {:min-level :info
       :output-fn (if local? dev-output-fn json-output-fn)})))

(defn wrap-request-logging
  "Ring middleware that logs method, path, status, and duration per request."
  [handler]
  (fn [request]
    (let [start    (System/nanoTime)
          response (handler request)
          duration (/ (- (System/nanoTime) start) 1e6)]
      (log :info "http_request"
           {:method   (name (:request-method request))
            :path     (:uri request)
            :status   (:status response)
            :duration (format "%.1fms" duration)})
      response)))
