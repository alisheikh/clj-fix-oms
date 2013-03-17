(ns clj-fix-oms.core
  (:use ordered.set))

(def open-orders (ref {}))
(def closed-orders (ref {}))
(def positions (ref {}))

(defn add-order [order collection]
  (let [{:keys [symbol side price client-order-id]} order]
    (if-let [orders-at-price (get-in @collection [symbol side price])]
      (alter collection update-in [symbol side price]
        into [{:order-ids
          (conj (:order-ids orders-at-price) client-order-id)}
          {client-order-id order}])
      (if (get-in @collection [symbol side])
        (alter collection update-in [symbol side]
          into {price {:order-ids (ordered-set client-order-id)
                       client-order-id order}})
        (alter collection assoc-in [symbol side]
          (sorted-map price {:order-ids (ordered-set client-order-id)
                             client-order-id order}))))))

(defn remove-order [order collection]
  (let [{:keys [symbol side price]} order
        order-id (:client-order-id order)]
    (if-let [active-order
            (get-in @collection [symbol side price order-id])]
      (let [orders (get-in @collection [symbol side price :order-ids])]
        ; Remove the order id from the price's order list.
        (alter collection update-in [symbol side price :order-ids]
          disj order-id)
        ; Remove the order from collection.
        (alter collection update-in [symbol side price]
          dissoc order-id)
        ; Remove a price completely if there are no orders for it.
        (if (empty? (get-in @collection [symbol side price :order-ids]))
          (alter collection update-in [symbol side]
            dissoc price))))))

; This needs to be cleaned-up and search closed-orders as well.
(defn find-order [order-id]
  (let [orders (filter map? (for [[_ os] @open-orders [_ oos] os [_ ooos] oos
                             [_ oooos] ooos] oooos))]
    (first (for [o orders :when (= order-id (:client-order-id o))] o))))

(defn modify-order [order]
  (let [{:keys [symbol side price client-order-id]} order]
    (alter open-orders assoc-in [symbol side price client-order-id] order)))

(defn get-order-ids [sym side price]
  (get-in @open-orders [sym side price :order-ids]))

(defn get-best-priced-order [sym side]
  ; If there is more than one order at the best price, returns the order first
  ; in the queue. Because of various cancel-replace policies, this does not
  ; guarantee the order has best time priority.
  (if-let [best-price (if (= :buy side) 
                        (ffirst (reverse (get-in @open-orders [sym side])))
                        (ffirst (get-in @open-orders [sym side])))]
    (let [order-ids (get-order-ids sym side best-price)]
      (get-in @open-orders [sym side best-price (first order-ids)]))))

(defn order-accepted [order]
  (dosync (add-order order open-orders)))

(defn order-canceled [order]
  (dosync
    (remove-order order open-orders)
    (add-order order closed-orders)))

(defn order-partially-filled [order]
  (dosync
    (modify-order order)))

(defn order-filled [order]
  (dosync
    (order-canceled order)))

(defn order-replaced [order]
  (dosync
    (remove-order (find-order (:client-order-id order)) open-orders)
    (add-order order open-orders)))

(defn order-pending-replace [order]
  (let [{:keys [symbol side price orig-client-order-id]} order]
  (dosync
    (remove-order (get-in @open-orders [symbol side price orig-client-order-id])
                     open-orders)
    (add-order order open-orders))))

(defn update-oms [order]
  (if-let [status (:order-status order)]
    (do
      ; Temporary output function for certification
      ; (println (map #(% order)
      ;   [:order-status :symbol :order-qty :price :cumulative-qty :leaves-qty
      ;    :last-price :avg-price]))
      (case status
        :pending-new nil ; (println "CLJ-FIX-OMS: Pending New")
        :new (order-accepted order)
        :partial-fill (order-partially-filled order)
        :filled (order-filled order)
        :canceled (order-canceled order)
        :replace (order-replaced order)
        :pending-cancel nil;(println "CLJ-FIX-OMS: Pending Cancel")
        :rejected nil;(println "CLJ-FIX-OMS: Order Rejected")
        :pending-replace (order-pending-replace order)))))