(ns splash.macros)

;(def ^:dynamic *canvas* nil)
;(def ^:dynamic *layer* nil)

(defmacro ignore [& body]
  '())


(defmacro with-sprite [canvas layer [symb bind] & body]
  `(let [~symb ~bind]
     (.addChild (-> ~canvas :layer ~layer) ~symb)
     (do ~@body)
     (.removeChild (-> ~canvas :layer ~layer) ~symb)))

;; todo: reqrite these two macros recursively
(defmacro with-sprites [canvas layer binds & body]
  (let [symbols (map first (partition 2 binds))]
    (list 'let binds
          (concat (map (fn [symb] `(.addChild (-> ~canvas :layer ~layer) ~symb)) symbols)
                  [(cons 'do body)]
                  (map (fn [symb] `(.removeChild (-> ~canvas :layer ~layer) ~symb)) (reverse symbols))))))


(macroexpand '(with-sprites canv lay [a my-a b my-b] (do1) (do2)))
