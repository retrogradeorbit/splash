(ns ^:figwheel-always splash.core
    (:require [infinitelives.pixi.canvas :as canv]
              [infinitelives.pixi.resources :as resources]
              [infinitelives.pixi.sprite :as sprite]
              [infinitelives.utils.events :as events]
              [infinitelives.utils.console :refer [log]]
              [infinitelives.pixi.texture :as texture]
              [infinitelives.pixi.font :as font]
              [infinitelives.utils.math :as math]
              [infinitelives.utils.sound :as sound]
              [infinitelives.utils.dom :as dom]
              [splash.shaders :as shaders]
              [cljs.core.async :refer [<!]]
              [PIXI])
    (:require-macros [cljs.core.async.macros :refer [go]]
                     [splash.macros :as macros]
                     ))

(enable-console-print!)

;; makes fonts and text objects scale pixely (GL_NEAREST)
;; looks better on oldschool games
(set! (.-scaleModes.DEFAULT js/PIXI) (.-scaleModes.NEAREST js/PIXI))

;; remove spinner div
(go
  (<! (events/next-frame))
  (let [div (aget (.getElementsByTagName js/document "div") 0)]
    (.log js/console div)
    (dom/remove! js/document.body div)))

(defonce canvas
  (canv/init
   {:expand true
    :engine :auto
    :layers [:stars :ui]
    :background 0x000000 ;0x505050
    }))

(defonce render-go-block (go (while true
               (<! (events/next-frame))
               ((:render-fn canvas)))))

(defonce load-fonts (font/google ["Indie Flower"]))

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
(defonce test-text (font/make-text "500 24px Indie Flower"
                                    "-=retrogradeorbit=-\n"
                                    :weight 500
                                    :fill "#ff00ff"
                                    ;:stroke "#505050"
                                    ;:strokeThickness 1
                                    ))

(defonce left "←")
(defonce right "→")
(defonce scroll-text
  (font/make-text "100 24px Indie Flower"
                  "Old School Demo on a New School platform??! Crispin, here, paying homage to the 8-bit and 16-bit wonderboxes of our childhood. Commodore lives! A big shout out to Team Farnarkle and to the Inversion crew. Best wishes to everyone out there and enjoy the games!\n"))

(def menu
  [
   {:title "Alien Forest Explorer"
    :date "Global Game Jam, Feb 2015"
    :desc "You are an alien away team of one, sent on a reconnosaince mission to recover a stranded alien new born. Your captain, Rex, is there to talk you through the adventure and help you find poor Ruddiger. Unique graphical style makes this one a visual treat, but a bit of a stomach churner."
    :url "http://forest.procrustes.net"}
   {:title "Marty Funk"
    :date "Ludum Dare 32 Jam, April 2015"
    :desc "The townsfolk grow angry, and you as the only man of cloth, must soothe their strained nerves by chanting to them. You can fly around by using the great trouser-breath yoga to help get in ear shot. But don't get hit by stones. The years of sitting have made you frail and weak."
    :url "http://marty-funk.procrustes.net"}
])

(def selected (atom 0))

(defn main []
  (go
    ;; load assets with loader bar
    (<! (resources/load-resources
         (-> canvas :layer :ui)
         [
          "sfx/splash-screen.ogg"
          "img/stars.png"
          "http://fonts.gstatic.com/s/indieflower/v8/10JVD_humAd5zP2yrFqw6ugdm0LZdjqr5-oayXSOefg.woff2"
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

    ;;
    ;; play music on a loop
    ;;
    (go (let [tune (<! (sound/load-sound "/sfx/splash-screen.ogg"))
              [source gain] (sound/play-sound tune 0.7 true)]))

    ;;
    ;; arrows
    ;;
    (let [left-arrow (font/make-text "100 48px Indie Flower" left)
          right-arrow (font/make-text "100 48px Indie Flower" right)]
      (sprite/set-pos! left-arrow -200 0.0)
      (sprite/set-pos! right-arrow 200 0.0)
      (sprite/set-scale! left-arrow 2)
      (sprite/set-scale! right-arrow 2)
      (sprite/set-alpha! left-arrow 0.5)
      (sprite/set-alpha! right-arrow 0.5)
      (.addChild (-> canvas :layer :ui) left-arrow)
      (.addChild (-> canvas :layer :ui) right-arrow)
      (go (loop [n 0]
            (let [w (.-innerWidth js/window)
                  hw (/ w 2)
                  x (* 0.9 hw)]
              (sprite/set-pos! left-arrow (- x) 0)
              (sprite/set-pos! right-arrow x 0)
              (<! (events/next-frame))
              (recur (inc n))))))

    ;;
    ;; bouncing name
    ;;
    (sprite/set-alpha! test-text 0.0)
    (sprite/set-scale! test-text 5)
    (set! (.-filters test-text) #js [(shaders/make-colour-bars)])
    (.addChild (-> canvas :layer :ui) test-text)
    (resources/fadein test-text :duration 5)
    (go (loop [n 0]
          (let [h (.-innerHeight js/window)
                hh (/ h 2)
                qh (/ h 4)]
            (sprite/set-pos! test-text 0 ;-200
                             (+ 0 (- (* 0.1 qh (Math/sin (* 0.1 n))) qh))
                             ))
          (<! (events/next-frame))
          (recur (inc n))))

    ;;
    ;; scrolling message
    ;;
    (go
      (sprite/set-scale! scroll-text 4)
      (set! (.-filters scroll-text) #js [(shaders/make-wavy)])

      (.addChild (-> canvas :layer :ui) scroll-text)
      (sprite/set-pos! scroll-text 10000 10000)

      (let [w (.-width scroll-text)
            hw (/ w 2 5)
            buff 300
            off (+ hw buff)]
        (go (loop [n (- off)]
              (let [h (.-innerHeight js/window)
                    win-w (.-innerWidth js/window)
                    hh (/ h 2)
                    qh (/ h 4)]
                (set! (.-filterArea scroll-text) (PIXI/Rectangle. 0 hh win-w h))
                (sprite/set-pos! scroll-text (* n -5) (- hh 40)))
              (<! (events/next-frame))
              (recur (if (> n off) (- off) (inc n)))))))

    ;;
    ;; parallax stars
    ;;
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
                      :alpha 1.0))]
      (macros/with-sprite-set canvas :stars
        [sprs star-spr]
        (loop [c 0]
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

          (recur (inc c)))))))

(main)
