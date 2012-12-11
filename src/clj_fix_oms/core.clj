(ns clj-fix-oms.core)

(def open-orders (ref {}))
(def closed-orders (ref {}))
(def positions (ref {}))

(defn add-order [order]
  (let [{:keys [symbol side price client-order-id]} order]
    (dosync
      (if-let [orders-at-price (get-in @open-orders [symbol side price])]
        (alter open-orders update-in [symbol side price]
          into [{:order-ids
            (conj (:order-ids orders-at-price) client-order-id)}
            {client-order-id order}])
        (if (get-in @open-orders [symbol side])
          (alter open-orders update-in [symbol side]
            into {price {:order-ids [client-order-id] client-order-id order}})
          (alter open-orders assoc-in [symbol side]
            (sorted-map price {:order-ids [client-order-id]
                               client-order-id order})))))))

(defn locate-order-pos [order-id orders]
  (let [idx (.indexOf orders order-id)]
    (if (not= -1 idx) idx nil)))

; A removed order needs to be added to closed-orders.
(defn remove-order [order]
  (let [{:keys [symbol side price client-order-id]} order]
    (dosync
      (if-let [active-order
              (get-in @open-orders [symbol side price client-order-id])]
        (let [orders (get-in @open-orders [symbol side price :order-ids])
              order-pos (locate-order-pos client-order-id orders)]
          (alter open-orders assoc-in [symbol side price :order-ids]
            (vec (concat (subvec orders 0 order-pos)
                         (subvec orders (inc order-pos)))))
          (alter open-orders update-in [symbol side price]
            dissoc client-order-id))))))

; This needs to be cleaned-up and search closed-orders as well.
(defn find-order [order-id]
  (let [orders (filter map? (for [[_ os] @open-orders [_ oos] vs [_ ooos] oos
                             [_ oooos] ooos] oooos))]
    (first (for [o orders :when (= order-id (:client-order-id o))] o))))
