(ns clj-fix-oms.test.core
  (:use [clj-fix-oms.core])
  (:use [clojure.test]))

(def orders {
  :order-a {:symbol "ABC" :side :buy :price 1.0 :client-order-id "a"}
  :order-b {:symbol "ABC" :side :sell :price 1.01 :client-order-id "b"}
  :order-c {:symbol "ABC" :side :buy :price 1.0 :client-order-id "c"}
  :order-d {:symbol "ABC" :side :buy :price 0.99 :client-order-id "d"}
  :order-e {:symbol "XYZ" :side :buy :price 1.0 :client-order-id "e"}
  :order-f {:symbol "XYZ" :side :sell :price 1.01 :client-order-id "f"}
})

(deftest order-accepted-t

  (order-accepted (:order-a orders))
  (is (= {"ABC"
           {:buy
             {1.0
               {:order-ids #{"a"}
                "a" (:order-a orders)}}}} @open-orders))

  (order-accepted (:order-b orders))
  (is (= {"ABC"
           {:buy
             {1.0
               {:order-ids #{"a"}
                "a" (:order-a orders)}}
            :sell
              {1.01
                {:order-ids #{"b"}
                 "b" (:order-b orders)}}}} @open-orders))

  (order-accepted (:order-c orders))
  (is (= {"ABC"
           {:buy
             {1.0
               {:order-ids #{"a" "c"}
                "a" (:order-a orders) "c" (:order-c orders)}}
            :sell
              {1.01
                {:order-ids #{"b"}
                 "b" (:order-b orders)}}}} @open-orders))

  (order-accepted (:order-d orders))
  (is (= {"ABC"
           {:buy
             {0.99
               {:order-ids #{"d"}
                "d" (:order-d orders)}
              1.0
               {:order-ids #{"a" "c"}
                "a" (:order-a orders) "c" (:order-c orders)}}
            :sell
              {1.01
                {:order-ids #{"b"}
                 "b" (:order-b orders)}}}} @open-orders))

  (order-accepted (:order-e orders))
  (is (= {"ABC"
           {:buy
             {0.99
               {:order-ids #{"d"}
                "d" (:order-d orders)}
              1.0
               {:order-ids #{"a" "c"}
                "a" (:order-a orders) "c" (:order-c orders)}}
            :sell
              {1.01
                {:order-ids #{"b"}
                 "b" (:order-b orders)}}}
          "XYZ"
            {:buy
              {1.0
                {:order-ids #{"e"}
                 "e" (:order-e orders)}}}} @open-orders))

  (order-accepted (:order-f orders))
  (is (= {"ABC"
           {:buy
             {0.99
               {:order-ids #{"d"}
                "d" (:order-d orders)}
              1.0
               {:order-ids #{"a" "c"}
                "a" (:order-a orders) "c" (:order-c orders)}}
            :sell
              {1.01
                {:order-ids #{"b"}
                 "b" (:order-b orders)}}}
          "XYZ"
            {:buy
              {1.0
                {:order-ids #{"e"}
                 "e" (:order-e orders)}}
             :sell
               {1.01
                {:order-ids #{"f"}
                 "f" (:order-f orders)}}}} @open-orders)))

; cancel order

; find-order

; modify-order

; get-order-ids

; get-best-priced-order

; update


