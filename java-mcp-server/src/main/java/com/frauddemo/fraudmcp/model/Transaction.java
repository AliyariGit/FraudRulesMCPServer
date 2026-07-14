package com.frauddemo.fraudmcp.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

public class Transaction {

    private String transactionId;
    private double amount;
    private String country;
    private String merchant;
    private int customerAge;
    private String deviceRisk;
    private CustomerInfo customer;

    public static class CustomerInfo {
        private String country;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    /** Converts to the plain nested Map the condition tree evaluator walks
     * via dotted-path field lookups (mirrors Transaction.model_dump() in Python). */
    public Map<String, Object> toMap(ObjectMapper objectMapper) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.convertValue(this, LinkedHashMap.class);
        return map;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public int getCustomerAge() {
        return customerAge;
    }

    public void setCustomerAge(int customerAge) {
        this.customerAge = customerAge;
    }

    public String getDeviceRisk() {
        return deviceRisk;
    }

    public void setDeviceRisk(String deviceRisk) {
        this.deviceRisk = deviceRisk;
    }

    public CustomerInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerInfo customer) {
        this.customer = customer;
    }
}
