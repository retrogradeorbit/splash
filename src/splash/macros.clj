(ns splash.macros)

(defmacro ^:private assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))

(defmacro get-layer [canvas layer]
  `(-> ~canvas :layer ~layer))

(defmacro with-sprite [canvas layer bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")

  (if (pos? (count bindings))
    (let [symb (first bindings) val (second bindings)]
      `(let [~symb ~val]
         (.addChild (get-layer ~canvas ~layer) ~symb)
         (with-sprites ~canvas ~layer ~(subvec bindings 2) ~@body)
         (.removeChild (get-layer ~canvas ~layer) ~symb)))
    `(do ~@body)))


(macroexpand '(with-sprites canv lay [a aaa] (do1) (do2)))
