(ns riemann.zabbix-test
  (:require [riemann.zabbix :refer :all]
            [clojure.test :refer :all])
  (:import (java.io ByteArrayOutputStream)
           (java.nio ByteBuffer ByteOrder)
           (java.net Socket)))

(def test-event {:host "riemann.local"
                 :service "zabbix.test"
                 :state "ok"
                 :description "Successful test"
                 :metric 2
                 :time (/ (System/currentTimeMillis) 1000)
                 :tags ["riemann" "zabbix"]})
(def other-event {:host "riemann.local"
                  :service "zabbix.other"
                  :state "ok"
                  :description "Successful test"
                  :metric 5
                  :time (/ (System/currentTimeMillis) 1000)
                  :tags ["other"]})

(deftest ^:zabbix zabbix-datapoint-tests
  (is (= (:host (generate-datapoint test-event)) "riemann.local"))
  (is (= (:key (generate-datapoint test-event)) "zabbix.test"))
  (is (= (:value (generate-datapoint test-event)) (str (:metric test-event))))
  (is (= (:clock (generate-datapoint test-event)) (:time test-event))))

(deftest ^:zabbix zabbix-request-tests
  (is (= (:request (make-request [test-event other-event])) "sender data"))
  (is (= (count (:data (make-request [test-event other-event]))) 2)))

(deftest ^:zabbix zabbix-frame-tests
  (let [f (.toByteArray (make-frame (make-request test-event)))]
    (is (= (String. (byte-array (take 4 f))) "ZBXD"))
    (is (= (nth f 5)) 1)
    (let [l (-> (ByteBuffer/wrap (byte-array (drop 5 (take 13 f))))
                (.order ByteOrder/LITTLE_ENDIAN)
                (.getLong))
          b (drop 13 f)]
      (is (= l (count b))))))
