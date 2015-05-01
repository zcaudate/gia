(ns gia.match.pattern
  (:require  [hara.common.checks :refer [hash-map?]]
             [gia.match regex fn [optional :as optional] [eval :as eval]]
             [clojure.core.match :as match]
             [clojure.walk :as walk]))

(defn lazy-seq? [x] (instance? clojure.lang.LazySeq x))

(defn any? [x] true)

(defn none? [x] false)

(defn transform-pattern
  {:added "0.1"}
  [template]
  (cond (:% (meta template))       (eval/eval-pattern template)
        (#{'(quote &)
           '(quote _)} template)    template
        (or (lazy-seq? template)
            (list? template))      (list (apply list (map transform-pattern template)) :seq)    
        (#{'& '_} template)        template
        (vector? template)         (vec (map transform-pattern template))
        (set? template)            (let [pats (map transform-pattern template)
                                         pats (if (empty? pats) [#'none?] pats)]
                                     (apply list :or pats))
        (hash-map? template)  (->> (map (fn [[k v]]
                                      [(transform-pattern k) (transform-pattern v)]) template)
                               (into {}))
        (symbol? template)    (list 'quote template)
        :else template))

(defn pattern-form
  [sym template]
  (let [clauses [[(transform-pattern template)] true :else false]]
    (match/clj-form [sym] clauses)))

(defn pattern-single-fn [template]
  (let [sym        (gensym)
        match-form (pattern-form sym template)
        all-fn    `(fn [form#]
                     (let [~sym form#]
                       ~match-form))]
    (eval all-fn)))

(defn pattern-fn [template]
  (let [all-fns (->> template
                     (optional/pattern-seq)
                     (mapv pattern-single-fn))]
    (fn [form]
      (or (some #(% form) all-fns)
          false))))
