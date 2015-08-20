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


(def num-stars 50)
(def stars-set (map (fn [n]
                      (let [depth (math/rand-between 0 8)]
                        {:x (math/rand-between 0 400)
                         :y (math/rand-between 0 300)
                         :z (+ depth (rand))
                         :depth depth})) (range num-stars)))

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

    (let [scale [4 4]
          text (resources/get-texture :stars :nearest)
          stars (for [[x y] [[0 0] [8 0] [16 0] ;[24 0]
                             [0 8] [8 8] [16 8]
                             [0 16] [8 16] [24 24]]]
                  (texture/sub-texture text
                                        [x y] [8 8]))
          star-spr (for [z (range (count stars)) n (range 50)]
                     (sprite/make-sprite
                      (nth stars z)
                      :x (* 4 (math/rand-between -200 200))
                      :y (* 4 (math/rand-between -150 150))
                      :scale scale
                      :alpha 0.0))]
      (macros/with-sprite-set canvas :stars
        [sprs star-spr]
        (doseq [s sprs] (resources/fadein s :duration 1.5))
        (<! (events/wait-time 2000))
        (doseq [s sprs] (resources/fadeout s :duration 1.5))
        (<! (events/wait-time 1500))))

    (macros/with-sprite canvas :stars
      [spr (sprite/make-sprite

            (texture/sub-texture
             (resources/get-texture :stars :nearest)
             [0 0] [8 8])

            :scale [4 4]
            :alpha 0.0)]

      (<! (resources/fadein spr :duration 0.5))
      (<! (events/wait-time 2000))
      (<! (resources/fadeout spr :duration 0.5)))))


(main)
