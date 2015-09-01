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
(defonce test-text (font/make-text "500 24px Indie Flower"
                                    "-=retrogradeorbit=-\n"
                                    :weight 500
                                    :fill "#ff00ff"
                                    ;:stroke "#505050"
                                    ;:strokeThickness 1
                                    ))
(defonce scroll-text
  (font/make-text "100 24px Indie Flower"
                  "Old School Demo on a New School platform??! Crispin, here, paying homage to the 8-bit and 16-bit wonderboxes of our childhood. Commodore lives! A big shout out to Team Farnarkle and to the Inversion crew. Best wishes to everyone out there and enjoy the games!\n"))

;;
;; font preloader channel
;;
(defonce render! (go
                   (<! (events/wait-time 2000))
                   (sprite/set-pos! dummy-text -100000 -10000)
                   (sprite/set-pos! test-text 0 0)
                   (.addChild (-> canvas :layer :ui) dummy-text)
                   (<! (events/next-frame))
                   (.removeChild (-> canvas :layer :ui) dummy-text)
                   (<! (events/next-frame))))


;;
;; experimental shaders
;;

(defn make-test []
  (let [f
        (PIXI/AbstractFilter.
         #js [
              "
precision mediump float;
varying vec2 vTextureCoord;
varying vec4 vColor;
uniform sampler2D uSampler;
uniform float time;

// hue,saturation,value -> red,green,blue
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// shader mainline
void main( void ) {
    vec4 col = texture2D(uSampler, vTextureCoord);

    float r=col.r;
    float g=col.g;
    float b=col.b;

    // is our hue magenta?
    bool mag = ((r-b)<0.1) && r>0.1 && g<0.1;

    if(mag)
    {
         gl_FragColor = vec4(
             hsv2rgb(
                 vec3(4.0 * vTextureCoord.y + (-0.02 * time),
                      1.0,
                      1.0)),
             1.0) * r;
    }
    else
    {
        gl_FragColor = col;
    }
}
"]
         #js {"time" #js {"type" "1f" "value" 0.0}})]
    (go
      (loop [frame 0]
        (<! (events/next-frame))
        (set! (.-uniforms.time.value f) (float frame))
        (recur (inc frame))))
    f))

(defn make-bounce []
  (let [f
        (PIXI/AbstractFilter.
         #js [
              "
precision mediump float;
varying vec2 vTextureCoord;
varying vec4 vColor;
uniform sampler2D uSampler;
uniform float time;

// shader mainline
void main( void ) {
    float x=vTextureCoord.x;
    float y=vTextureCoord.y;
    vec4 col = texture2D(uSampler,
                         vec2(x,
                              y - 0.05 * sin(0.1 * time + 10.0 * x))
                         //vec2(x,y)
                         );

    gl_FragColor = col;
}
"]
         #js {"time" #js {"type" "1f" "value" 0.0}})]
    (go
      (loop [frame 0]
        (<! (events/next-frame))
        (set! (.-uniforms.time.value f) (float frame))
        (recur (inc frame))))
    f))


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

    ;; wait for fonts to be ready
    (<! render!)

    (go (let [tune (<! (sound/load-sound "/sfx/splash-screen.ogg"))
              [source gain] (sound/play-sound tune 0.7 true)
              ])
        )

    ;;
    ;; bouncing name
    ;;
    (sprite/set-alpha! test-text 0.0)
    (sprite/set-scale! test-text 6)
    (set! (.-filters test-text) #js [(make-test)])
    (.addChild (-> canvas :layer :ui) test-text)
    (resources/fadein test-text :duration 5)
    (go (loop [n 0]
          (let [h (.-innerHeight js/window)
                hh (/ h 2)
                qh (/ h 4)]
            (sprite/set-pos! test-text 0 ;-200
                             (+ (* 0.5 qh) (- (* 0.1 qh (Math/sin (* 0.1 n))) qh))
                             ))
          (<! (events/next-frame))
          (recur (inc n))))

    ;;
    ;; scrolling message
    ;;
    (go
      (sprite/set-scale! scroll-text 4)
      (set! (.-filters scroll-text) #js [(make-bounce)])

      (.addChild (-> canvas :layer :ui) scroll-text)
      (sprite/set-pos! scroll-text 10000 10000)

      ;; we have to wait a frame or the font is wrong? huh?
      (<! (events/next-frame))
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
                (sprite/set-pos! scroll-text (* n -5) (- hh 120)))
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
        ;(doseq [s sprs] (resources/fadein s :duration 15))

        (go (loop [n 2000 c 0]
              (when true                ;(pos? n)
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

        ;; wait forever
        (loop []
          (<! (events/next-frame))
          (recur))))))

(main)
