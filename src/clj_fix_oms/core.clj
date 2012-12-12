(ns clj-fix-oms.core
  (:use ordered.set))

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
            into {price {:order-ids (ordered-set client-order-id)
                         client-order-id order}})
          (alter open-orders assoc-in [symbol side]
            (sorted-map price {:order-ids (ordered-set client-order-id)
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
        (let [orders (get-in @open-orders [symbol side price :order-ids])]
          ; Remove the order id from the price's order list.
          (alter open-orders update-in [symbol side price :order-ids]
            disj client-order-id)
          ; Remove the order from open-orders.
          (alter open-orders update-in [symbol side price]
            dissoc client-order-id)
          ; Remove a price completely if there are no orders for it.
          (if (empty? (get-in @open-orders [symbol side price :order-ids]))
            (alter open-orders update-in [symbol side]
              dissoc price)))))))

; This needs to be cleaned-up and search closed-orders as well.
(defn find-order [order-id]
  (let [orders (filter map? (for [[_ os] @open-orders [_ oos] os [_ ooos] oos
                             [_ oooos] ooos] oooos))]
    (first (for [o orders :when (= order-id (:client-order-id o))] o))))

(defn get-order-ids [sym side price]
  (get-in @open-orders [sym side price :order-ids]))

(defn get-best-priced-order [sym side]
  ; If there is more than one order at the best price, returns the order with
  ; best time priority.
  (if-let [best-price (if (= :buy side) 
                        (ffirst (reverse (get-in @open-orders [sym side])))
                        (ffirst (get-in @open-orders [sym side])))]
    (let [order-ids (get-order-ids sym side best-price)]
      (get-in @open-orders [sym side best-price (first order-ids)]))))

