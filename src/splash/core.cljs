(ns ^:figwheel-always splash.core
    (:require [infinitelives.pixi.canvas :as canv]
              [infinitelives.pixi.resources :as resources]
              [infinitelives.pixi.sprite :as sprite]
              [infinitelives.utils.events :as events]
              [infinitelives.utils.console :refer [log]]
              [infinitelives.pixi.texture :as texture]
              [infinitelives.utils.math :as math]
              ;[splash.macros :refer-macros [ignore]]
              [cljs.core.async :refer [<!]])
    (:require-macros [cljs.core.async.macros :refer [go]]
                     [splash.macros :as macros]
                     ))

(enable-console-print!)

(defonce canvas
  (canv/init
   {:expand true
    :engine :auto
    :layers [:stars :ui]
    :background 0x505050}))

(defonce render-go-block (go (while true
               (<! (events/next-frame))
               ;(log "frame")
               ((:render-fn canvas)))))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn main []
  (go
    (<! (resources/load-resources
         (-> canvas :layer :ui)
         [
          "img/stars.png"
          "http://www.goodboydigital.com/pixijs/examples/1/bunny.png"
          ]
         :full-colour 0x302060
         :highlight 0x8080ff
         :lowlight 0x101030
         :empty-colour 0x202020
         :debug-delay 0.2
         :width 400
         :height 32
         :fade-in 0.2
         :fade-out 0.5))

    (macros/with-sprite canvas :stars
      [spr (sprite/make-sprite

            (texture/sub-texture
             (resources/get-texture :stars :nearest)
             [0 0] [8 8]
             )
            :scale [4 4]
            :alpha 0.0)]

      (<! (resources/fadein spr :duration 0.5))
      (<! (events/wait-time 2000))
      (<! (resources/fadeout spr :duration 0.5))
      )))


(main)
