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
