package com.zebra.jamesswinton.hudstockcheckdemo;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    // Barcode & Stock Holder
    public static final Map<String, Product> StockMap = new HashMap<String, Product>()
    {
        {
            put("75934759475", new Product("6pt Milk",  10));
            put("435436543345", new Product("Hovis White Bread", 3));
            put("654654654634", new Product("Tinned Tomatoes", 0));
        }
    };


}
