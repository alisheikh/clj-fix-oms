(ns clj-fix-oms.core)

(def active-orders (ref {}))
(def canceled-orders (ref {}))
(def positions (ref {}))

(defn add-order [order]
  (let [{:keys [symbol side price client-order-id]} order]
    (dosync
      (if-let [orders-at-price (get-in @active-orders [symbol side price])]
        (alter active-orders update-in [symbol side price]
          into [{:order-ids
            (conj (:order-ids orders-at-price) client-order-id)}
            {client-order-id order}])
        (if (get-in @active-orders [symbol side])
          (alter active-orders update-in [symbol side]
            into {price {:order-ids [client-order-id] client-order-id order}})
          (alter active-orders assoc-in [symbol side]
            (sorted-map price {:order-ids [client-order-id]
                               client-order-id order})))))))

(defn locate-order-pos [order-id orders]
  (let [idx (.indexOf orders order-id)]
    (if (not= -1 idx) idx nil)))

(defn cancel-order [order]
  (let [{:keys [symbol side price client-order-id]} order]
    (dosync
      (if-let [active-order
              (get-in @active-orders [symbol side price client-order-id])]
        (let [orders (get-in @active-orders [symbol side price :order-ids])
              order-pos (locate-order-pos client-order-id orders)]
          (alter active-orders assoc-in [symbol side price :order-ids]
            (vec (concat (subvec orders 0 order-pos)
                         (subvec orders (inc order-pos)))))
          (alter active-orders update-in [symbol side price]
            dissoc client-order-id))))))

