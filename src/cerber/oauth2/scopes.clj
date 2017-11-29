(ns cerber.oauth2.scopes
  (:require [cerber.config :refer [app-config]]
            [cerber.helpers :refer [str->vec]]
            [clojure.string :as str]))

(defn- defined-fn? [sym]
  (when-let [dfn (and (symbol? sym) (resolve sym))]
    (and (fn? dfn) dfn)))

(defn- mapper-fn [sym arg]
  (if (defined-fn? sym)
    (sym arg)
    (if (string? sym)
      (hash-set sym)
      (when (set? sym) sym))))

(defn- distinct-scope
  "Returns falsey if scopes contain given scope or any of its parents.
  Returns scope otherwise."

  [scopes ^String scope]
  (let [v (.split scope ":")]
    (loop [s v]
      (if (empty? s)
        scope
        (when-not (contains? scopes (str/join ":" s))
          (recur (drop-last s)))))))

(defn scope->roles [scope]
  (mapper-fn (:scope->roles app-config)
             (str->vec scope)))

(defn scope->permissions [scope]
  (mapper-fn (:scope->permisssions app-config)
             (str->vec scope)))

(defn normalize-scope
  "Normalizes scope string by removing duplicated and overlapping scopes."

  [scope]
  (->> scope
       (str->vec)
       (sort-by #(count (re-seq #":" %)))
       (reduce (fn [reduced scope]
                 (if-let [s (distinct-scope reduced scope)]
                   (conj reduced s)
                   reduced))
               #{})))

(defn allowed-scopes?
  "Checks whether all given scopes appear in a set of allowed-scopes."
  [scopes allowed-scopes]
  (let [filtered (->> scopes
                      (map #(contains? allowed-scopes %))
                      (filter true?))]

    (= (count filtered)
       (count scopes))))
