(ns clj-fix-oms.core)

(def active-orders (ref {}))
(def canceled-orders (ref {}))
(def positions (ref {}))

(defn add-order [order]
  (let [{:keys [symbol side price]} order]
    (dosync
      (if-let [orders-at-price (get-in @active-orders [symbol side price])]
        (alter active-orders assoc-in [symbol side price]
          (conj orders-at-price order))
        (if-let [orders-on-side (get-in @active-orders [symbol side])]
          (alter active-orders update-in [symbol side] into {price [order]})
          (alter active-orders assoc-in [symbol side]
                                        (sorted-map price [order])))))))
