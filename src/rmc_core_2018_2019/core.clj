(ns rmc-core-2018-2019.core
   (:require aleph.tcp
             [manifold.stream :as s]
             pyro.printer
             [clojure.java.io :as io])
   (:gen-class))

(set! *warn-on-reflection* true)
(pyro.printer/swap-stacktrace-engine!)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; bytes->num
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn bytes->num [data]
   (reduce bit-or
           (map-indexed
              (fn [i x]
                 (bit-shift-left
                    (bit-and x 0x0FF)
                    (* 8
                       (-
                          (count data)
                          i 1))))
              data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; handle-new-message
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-new-message
   "Handles a new incoming message that was sent by the GUI."
   [bytes]
   {:pre [(seq? bytes)]}
   (let
      [command (first bytes)
       args    (rest bytes)]
      (case command
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ; case 16
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         16 (println args)

         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ; case 17
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         17 (->
               args
               byte-array
               String.
               println)

         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ; case 6
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         6 (with-open [logger (io/writer "log.log" :append true)]
              (.write logger (str args "\n")))

         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ; default
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         (println "Received unknown message"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; make-connection-handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn make-connection-handler [messageHandler clientStreams]
   {:pre  [(ifn? messageHandler)
           ;(seq? @clientStreams)
           ]
    :post [(ifn? %)]}
   (fn [stream _]
      {:post [;(contains? @clientStreams stream)
              ]}
      (do
         (dosync
            (alter clientStreams conj stream))
         (io!
            (s/consume-async
               (fn [bytes]
                  (-> bytes
                      byte-streams/to-byte-array
                      seq
                      messageHandler))
               stream)))))

(defn -main []
   (let
      [clientStreams (ref [])
       handler       (make-connection-handler handle-new-message clientStreams)]
      (aleph.tcp/start-server handler {:port 2401})))