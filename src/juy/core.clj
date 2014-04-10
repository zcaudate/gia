(ns juy.core
  (:require [rewrite-clj.zip :as z]
            [juy.walk :refer [matchwalk postwalk prewalk levelwalk]]
            [juy.match :refer [compile-matcher]]))

(defn juy
  ([f templates]
     (juy f templates nil))
  ([f templates update-fn]
      (let [zloc (z/of-file f)
            atm  (atom [])]
        (levelwalk zloc
                        (map compile-matcher templates)
                        (fn [zloc]
                          (let [nloc (if update-fn
                                       (update-fn zloc)
                                       zloc)]
                            (swap! atm conj nloc)
                            nloc)))
        @atm)))

(defn root-sexp [zloc]
  (if-let [nloc (z/up zloc)]
    (condp = (z/tag nloc)
      :forms   zloc
      :branch? zloc
      (recur nloc))
    zloc))

(defn parent-sexp [zloc]
  (if-let [nloc (z/up zloc)]
    (condp = (z/tag nloc)
      :forms   zloc
      :branch? zloc
      nloc)
    zloc))

(comment
  (>pst)

  '[defn :> {:type :vector}]
  :replace '(1 2 3 4)
  :update
  :insert
  :delete

  [:tag {:attr 1}
   [:tag {:attr 1}]
   [:tag {:attr 2}]]

  [:right 2 :delete 1 :]

  [:replace-code "\n   (1 2 3 4)  \n"]
  [:replace-code "\n   (1 2 3 4)  \n"]
  [:replace-code "\n   (1 2 3 4)  \n"]
  :insert [1 "   code    "] :code
  :insert [1 "   code    "]

  (->> (juy "src/juy/match.clj" [#_{:is    '(defn & _)
                                  :child 'if}
                                 'defn ;;:> 'if
                                 {:is 'defn}]
            [:top-level :up 1 :update (fn [sym] (symbol "+" sym)) :replace 'defn :move 2 :delete 1]
            (fn [n]
              (-> n (z/replace 'defn++))))
       (map root-sexp)
       (map z/sexpr))

  (->> (juy "src/juy/match.clj" ['(defn _ ^:+ string? & _)
                                 ])
       (map root-sexp)
       (map z/sexpr))

  ((compile-matcher '(defn _ ^:+ vector? & _))
   (z/of-string "(defn x [])"))

  (>pst)
  (mapv (juxt (comp meta z/node)
              (comp z/sexpr z/node root-sexp)))


  (comment
   {:order (< 2)
    :right {:right string?}}

   (def sample
     (z/of-string
      "
(do 1 2 3)
(defn      id        [a] b)
(defn      id        [c] d)
(defn lister [e]
   (list 1 2 3 4 f))"
      ))


   (matchwalk sample
              [(compile-matcher  #{'defn})
               (compile-matcher  {:right vector?})
               ]
              (fn [node]
                (println (z/sexpr node))
                node
                ))

   (matchlevelwalk sample
                   [(compile-matcher  #{'defn})]
                   (fn [node]
                     (println (z/sexpr node))
                     node
                     ))

   (prewalk sample
            (compile-matcher {:is number?})
            (fn [node]
              (println (z/sexpr node))
              node
              ))


   (prewalk sample
            (compile-matcher 'defn)
            (fn [node]
              (println (z/sexpr node))
              node
              ))

   (postwalk sample
             (compile-matcher 'defn)
             (fn [node]
               (println (z/sexpr node))
               node
               ))

   (defn postwalk
     [zloc pred? f level direction]
     (condp = direction
       :down (if-let [zdown (z/down)]
               (postwalk zdown pred? f (inc level) :down))))

   (defn postwalk
     ([zloc f]
        ())
     ([zloc p f]))


   (comment

     (matchwalk sample
                [(compile-matcher 'defn)
                 (compile-matcher {:contains number?})]
                (fn [node]
                  ;;(z/replace node "hello")
                  (println (z/sexpr node))
                  node
                  ))

     (def a (let [atm (atom [])]
              (-> (postwalk sample
                            (compile-matcher {:right number?})
                            (fn [node]
                              (z/replace node "hello")
                              ;;(println (z/sexpr node))
                              ;;node
                              ))
                  (z/sexpr))
              ))

     (def a (let [atm (atom [])]
              (-> (postwalk sample
                            (compile-matcher 'defn)
                            (fn [node]
                              (postwalk node
                                        (compile-matcher {:right number?})
                                        (fn [node]
                                          (z/replace node "hello")))))

                  (z/->root-string))))

     (println a)

     (let [atm (atom [])]
       (-> (z/prewalk sample
                      (?-right (?-right number?))
                      (fn [x]
                        (swap! atm conj x)
                        x)))
       (map (juxt (comp meta z/node) z/sexpr) @atm))

     ([{:row 4, :col 5} list] [{:row 4, :col 10} 1] [{:row 4, :col 12} 2])

     (let [atm (atom [])]
       (-> (z/prewalk sample
                      (?-left (?-left number?))
                      (fn [x]
                        (swap! atm conj x)
                        x)))
       (map (juxt (comp meta z/node) z/sexpr) @atm))

     (>pst)

     => ([{:row 4, :col 5} list] [{:row 4, :col 10} 1] [{:row 4, :col 12} 2] [{:row 4, :col 14} 3])


     (let [atm (atom [])]
       (-> (z/prewalk sample
                      (?-left number?)
                      (fn [x]
                        (swap! atm conj x)
                        x)))
       (map (juxt (comp meta z/node) z/sexpr) @atm))

     => ([{:row 4, :col 12} 2] [{:row 4, :col 14} 3] [{:row 4, :col 16} 4]))




















   (comment
     (let [atm (atom [])]
       (-> (z/prewalk sample
                      (?-is '(defn _ & _))
                      (fn [x]
                        (swap! atm conj x)
                        x)))
       (map (juxt (comp meta z/node) z/sexpr) @atm))

     (let [atm (atom [])]
       (-> (z/prewalk sample
                      (?-right string?)
                      (fn [x]
                        (swap! atm conj x)
                        x)))
       (map (juxt (comp meta z/node) z/sexpr) @atm))

     (let [atm (atom [])]
       (-> (z/prewalk sample
                      (?-symbol 'defn)
                      (fn [x]
                        (swap! atm conj x)
                        x)))
       (map (juxt (comp meta z/node) z/->string) @atm))

     (comment



       (((quote defn) (quote id) (((quote if) true & _) :seq) & _) :seq)


       (list 'let ['x ]
             (clojure.core.match/clj-form
              '[x]
              '[[(((quote defn) (quote id) "oeuoeuo" [1 2 3 4] & _) :seq)] true :else false]))


       '(defn id & _)
       => [[(((quote defn) (quote id) & _) :seq)] true :else false]


       (z/sexpr (z/of-string (with-out-str (prn '(defn id [x] x)))))


       (def data (z/of-file "../hara/src/hara/common/error.clj"))

       (z/find-value data z/right 'defn)

       (def prj-map (z/find-value data z/next 'defproject))


       (-> sample z/tag)
       (-> sample z/next z/value)
       (z/next)
       (z/next)
       (z/node)
       meta

       ($ sample [{:is id
                   :contains "hello"}])

       ($ sample [{:is   id}])


       (defn ?left-of [sym])


       (defn ?right-of [sym])

       (-> (z/prewalk sample
                      (fn [x] (= 'id (z/sexpr x)))
                      (fn [x] (z/replace x 'hello)))
           (z/print-root))

       (def pair
         (let [atm (atom [])]
           (-> (z/prewalk sample
                          (symbol-pred 'defn)
                          (fn [x]
                            (swap! atm conj x)
                            x)))
           @atm))

       (map (juxt (comp meta z/node) z/sexpr) pair)


       (def pair-is
         (let [atm (atom [])]
           (-> (z/prewalk sample
                          (is-pred 'defn)
                          (fn [x]
                            (swap! atm conj x)
                            x)))
           @atm))

       (map (juxt (comp meta z/node) z/sexpr) pair-is)

       (def data (z/of-string "(1 (2 3))"))

       (-> data
           (z/next)
           (z/replace 2)
           (z/insert-left 1)
           (z/insert-left  [:whitespace "        "])
           (z/up)
           (z/sexpr))

       (z/sexpr sample))

     (z/value sample)

     (z/node (-> sample z/next z/next ))

     (z/node (z/right* sample))

     (z/sexpr (z/right* (z/right sample)))

     (defn $ [form & ])

     (def data '((defn id  [x] x)
                 (defn id2 [x] x)
                 (defn listr [x]
                   (list 1 2 3 4 x))))
     (comment
       ($ data [(defn is ^:data [] ...)
                {:contains :list}
                {:value    :vector}
                hello
                [{is :whitespace}]])

       (filter

        {:type      :vector
         :value     [1 _ 3]
         :has-child hello
         :next
         :prev
         :sibling   {:type :vector}}



        )


       (-> ($ ' [(-> )])
           (fn [x] 4))
       )
     )
   )
)
