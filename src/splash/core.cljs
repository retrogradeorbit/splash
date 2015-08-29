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
(defonce test-text (font/make-text "100 24px Indie Flower"
                                    "-=retrogradeorbit=-\n"
                                    :weight 100
                                    :fill "#ff00ff"))
(defonce scroll-text
  (font/make-text "100 24px Indie Flower"
                  ". .. ... This will eventually be the index page for my game jam submissions. \"Alien Forest Explorer\" from Global Game Jam, and \"Marty Funk\" and \"Monster Simulator?\" from ludum dare competitions. These HTML5 games were all developed with free and open source software on a linux platform. I use pixi.js as the underlying framework but target it through infinitelives.pixi using clojurescript. If you haven't tried clojure or clojurescript you should definitely try them. This of course wouldn't have been possible without the work of so many other people. Those involved in the creation of clojure and clojurescript, in pixi.js and in the many tools I use, like grafx2, impulse tracker and sfxr. Best wishes to everyone out there and enjoy! ...  .. .\n")

)

(defonce render! (go
                   (<! (events/wait-time 1500))
                   (sprite/set-pos! dummy-text -100000 -10000)
                   (sprite/set-pos! test-text 0 0)
                   (.addChild (-> canvas :layer :ui) dummy-text)
                   (<! (events/next-frame))
                   (.removeChild (-> canvas :layer :ui) dummy-text)
                   (<! (events/next-frame))))


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

    (<! render!)

    (sprite/set-alpha! test-text 0.0)
    (sprite/set-scale! test-text 6)
    (.addChild (-> canvas :layer :ui) test-text)
    (resources/fadein test-text :duration 5)
    (go (loop [n 0]
          (let [h (.-innerHeight js/window)
                hh (/ h 2)
                qh (/ h 4)]
            (sprite/set-pos! test-text 0 (+ 200 (- (* 0.6 qh (Math/sin (* 0.04 n))) qh))))
          (<! (events/next-frame))
          (recur (inc n))))

    (sprite/set-scale! scroll-text 4)
    (.addChild (-> canvas :layer :ui) scroll-text)
    (go (loop [n -3200]
          (let [h (.-innerHeight js/window)
                hh (/ h 2)
                qh (/ h 4)]
            (sprite/set-pos! scroll-text (* n -5) (- hh 80)))
          (<! (events/next-frame))
          (recur (if (> n 3200) -3200 (inc n)))))

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
