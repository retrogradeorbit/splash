(ns ^:figwheel-always splash.core
    (:require [infinitelives.pixi.canvas :as canv]
              [infinitelives.pixi.resources :as resources]
              [infinitelives.pixi.sprite :as sprite]
              [infinitelives.utils.events :as events]
              [infinitelives.utils.console :refer [log]]
              [infinitelives.pixi.texture :as texture]
              [infinitelives.pixi.font :as font]
              [infinitelives.utils.math :as math]
              ;[splash.macros :refer-macros [ignore]]
              [cljs.core.async :refer [<!]]
              [PIXI])
    (:require-macros [cljs.core.async.macros :refer [go]]
                     [splash.macros :as macros]
                     ))

(enable-console-print!)

;; makes fonts and text objects scale pixely (GL_NEAREST)
;; looks better on oldschool games
(set! (.-scaleModes.DEFAULT js/PIXI) (.-scaleModes.NEAREST js/PIXI))

(defonce canvas
  (canv/init
   {:expand true
    :engine :auto
    :layers [:stars :ui]
    :background 0x000000 ;0x505050
    }))

(defonce render-go-block (go (while true
               (<! (events/next-frame))
               ;(log "frame")
               ((:render-fn canvas)))))

(defonce fonts
  [(font/install-google-font-stylesheet! "http://fonts.googleapis.com/css?family=Indie+Flower")])

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


(def num-stars 250)
(def stars-set (sort-by
                :z
                (map (fn [n]
                       (let [depth (math/rand-between 0 8)]
                         {:x (math/rand-between 0 2048)
                          :y (math/rand-between 0 2048)
                          :z (+ depth (rand))
                          :depth depth})) (range num-stars))))

(defonce font-inconsolata (font/make-tiled-font "Indie Flower" 100 10))
(defonce dummy-text (font/make-text "100 10px Indie Flower"
                                    "DUMMY"
                                    :weight 100
                                    :fill "#ffffff"))
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
          stars (for [[x y] [[16 8] [8 8] [16 0] [8 0] [0 0] ;[24 0]
                             [0 8]
                             [0 16] [8 16] [24 24]]]
                  (texture/sub-texture text
                                       [x y] [8 8]))
          star-spr (for [{:keys [x y z depth]} stars-set]
                     (sprite/make-sprite
                      (nth stars depth)
                      :x (* 4 x)
                      :y (* 4 y)
                      :scale scale
                      :alpha 0.0))]
      (macros/with-sprite-set canvas :stars
        [sprs star-spr]
        (doseq [s sprs] (resources/fadein s :duration 15))

        (go (loop [n 2000 c 0]
              (when true ;(pos? n)
                (<! (events/next-frame))

                (let [w (.-innerWidth js/window)
                      h (.-innerHeight js/window)
                      hw (/ w 2)
                      hh (/ h 2)
                      speed -1]

                  (doall
                   (map
                    (fn [{:keys [x y z] :as old} sprite]
                      (sprite/set-pos! sprite
                                       (- (mod (- (* 4 x) (* speed c z)) w) hw)
                                       (- (mod (* 4 y) h) hh)))
                    stars-set
                    star-spr)))

                (recur (dec n)
                       (inc c)
                       ))))

        ;(macros/with-font )

        ;; wait forever
        (loop []
          (<! (events/next-frame))
          (recur))

        (<! (events/wait-time 15000))


        ;(doseq [s sprs] (resources/fadeout s :duration 10))
        (<! (events/wait-time 15000))))

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
