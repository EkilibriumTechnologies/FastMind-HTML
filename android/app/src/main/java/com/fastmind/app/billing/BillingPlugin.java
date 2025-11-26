package com.fastmind.app.billing;

// GOOGLE BILLING SYSTEM - Billing Plugin for Capacitor
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "BillingPlugin")
public class BillingPlugin extends Plugin implements PurchasesUpdatedListener {

    private static final String TAG = "BillingPlugin";
    private BillingClient billingClient;
    private boolean isServiceConnected = false;
    private PluginCall pendingPurchaseCall;

    // GOOGLE BILLING SYSTEM - Helper method to detect product type as string (Billing v6 uses strings)
    private String detectProductType(String productId) {
        if (productId.contains("lifetime")) {
            return BillingClient.ProductType.INAPP; // "inapp"
        } else {
            return BillingClient.ProductType.SUBS; // "subs"
        }
    }

    // GOOGLE BILLING SYSTEM - Initialize Billing Client
    @PluginMethod
    public void initializeBilling(PluginCall call) {
        try {
            Activity activity = getActivity();
            if (activity == null) {
                call.reject("Activity is null");
                return;
            }

            // BILLING BRIDGE - Create BillingClient
            billingClient = BillingClient.newBuilder(activity)
                    .setListener(this)
                    .enablePendingPurchases()
                    .build();

            // BILLING LISTENER - Start connection
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    int responseCode = billingResult.getResponseCode();
                    if (responseCode == BillingClient.BillingResponseCode.OK) {
                        isServiceConnected = true;
                        Log.d(TAG, "Billing service connected");
                        JSObject result = new JSObject();
                        result.put("success", true);
                        result.put("message", "Billing initialized");
                        call.resolve(result);
                    } else {
                        isServiceConnected = false;
                        Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                        call.reject("Billing setup failed: " + billingResult.getDebugMessage());
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    isServiceConnected = false;
                    Log.w(TAG, "Billing service disconnected");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing billing", e);
            call.reject("Error initializing billing: " + e.getMessage());
        }
    }

    // GOOGLE BILLING SYSTEM - Start purchase flow
    @PluginMethod
    public void startPurchase(PluginCall call) {
        try {
            if (!isServiceConnected || billingClient == null) {
                call.reject("Billing service not connected");
                return;
            }

            String productId = call.getString("productId", "fastmind_premium");
            pendingPurchaseCall = call;

            // GOOGLE BILLING SYSTEM - Determine product type dynamically (SUBS for monthly, INAPP for lifetime)
            // Billing v6 uses strings, not enums
            final String productType = detectProductType(productId);

            // BILLING BRIDGE - Query product details
            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build();

            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(java.util.Collections.singletonList(product))
                    .build();

            billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
                @Override
                public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList) {
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "Query product details failed: " + billingResult.getDebugMessage());
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Query failed: " + billingResult.getDebugMessage());
                            pendingPurchaseCall = null;
                        }
                        return;
                    }

                    if (productDetailsList.isEmpty()) {
                        Log.e(TAG, "Product not found: " + productId);
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Product not found");
                            pendingPurchaseCall = null;
                        }
                        return;
                    }

                    ProductDetails productDetails = productDetailsList.get(0);
                    
                    // GOOGLE BILLING SYSTEM - Handle both subscription and one-time purchases
                    BillingFlowParams.ProductDetailsParams productDetailsParams;
                    
                    if (productType.equals(BillingClient.ProductType.SUBS)) {
                        // Subscription product - need offer token
                        List<SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
                        
                        if (offers == null || offers.isEmpty()) {
                            Log.e(TAG, "No subscription offers found");
                            if (pendingPurchaseCall != null) {
                                pendingPurchaseCall.reject("No subscription offers found");
                                pendingPurchaseCall = null;
                            }
                            return;
                        }

                        SubscriptionOfferDetails offerDetails = offers.get(0);
                        String offerToken = offerDetails.getOfferToken();

                        productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build();
                    } else if (productType.equals(BillingClient.ProductType.INAPP)) {
                        // One-time purchase (INAPP) - no offer token needed
                        productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build();
                    } else {
                        Log.e(TAG, "Unknown product type: " + productType);
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Unknown product type");
                            pendingPurchaseCall = null;
                        }
                        return;
                    }

                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(java.util.Collections.singletonList(productDetailsParams))
                            .build();

                    // GOOGLE BILLING SYSTEM - Check Activity is not null before launching billing flow
                    Activity activity = getActivity();
                    if (activity == null) {
                        Log.e(TAG, "Activity is null, cannot launch billing flow");
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Activity is null");
                            pendingPurchaseCall = null;
                        }
                        return;
                    }

                    BillingResult flowResult = billingClient.launchBillingFlow(activity, billingFlowParams);
                    
                    if (flowResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "Launch billing flow failed: " + flowResult.getDebugMessage());
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Purchase flow failed: " + flowResult.getDebugMessage());
                            pendingPurchaseCall = null;
                        }
                    }
                    // Purchase result will be handled in onPurchasesUpdated
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting purchase", e);
            if (pendingPurchaseCall != null) {
                pendingPurchaseCall.reject("Error starting purchase: " + e.getMessage());
                pendingPurchaseCall = null;
            }
        }
    }

    // GOOGLE BILLING SYSTEM - Check premium status from Google Play
    @PluginMethod
    public void checkPremiumStatus(PluginCall call) {
        try {
            if (!isServiceConnected || billingClient == null) {
                JSObject result = new JSObject();
                result.put("isPremium", false);
                call.resolve(result);
                return;
            }

            // GOOGLE BILLING SYSTEM - Query both subscription and one-time purchases
            final String[] foundProductId = {null};
            final boolean[] isPremium = {false};
            final int[] queriesCompleted = {0};
            final int totalQueries = 2;

            // Query subscriptions
            QueryPurchasesParams subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();

            billingClient.queryPurchasesAsync(subsParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : purchases) {
                            List<String> products = purchase.getProducts();
                            if (products != null) {
                                for (String product : products) {
                                    if (product.equals("fastmind_premium_monthly") || product.equals("fastmind_premium_lifetime")) {
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            if (!purchase.isAcknowledged()) {
                                                acknowledgePurchase(purchase);
                                            }
                                            isPremium[0] = true;
                                            foundProductId[0] = product;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    queriesCompleted[0]++;
                    if (queriesCompleted[0] >= totalQueries) {
                        finishPremiumCheck(call, isPremium[0], foundProductId[0]);
                    }
                }
            });

            // Query one-time purchases (INAPP)
            QueryPurchasesParams inappParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();

            billingClient.queryPurchasesAsync(inappParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : purchases) {
                            List<String> products = purchase.getProducts();
                            if (products != null) {
                                for (String product : products) {
                                    if (product.equals("fastmind_premium_monthly") || product.equals("fastmind_premium_lifetime")) {
                                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                            if (!purchase.isAcknowledged()) {
                                                acknowledgePurchase(purchase);
                                            }
                                            isPremium[0] = true;
                                            foundProductId[0] = product;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    queriesCompleted[0]++;
                    if (queriesCompleted[0] >= totalQueries) {
                        finishPremiumCheck(call, isPremium[0], foundProductId[0]);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking premium status", e);
            JSObject result = new JSObject();
            result.put("isPremium", false);
            call.resolve(result);
        }
    }

    // GOOGLE BILLING SYSTEM - Helper method to finish premium status check
    private void finishPremiumCheck(PluginCall call, boolean isPremium, String productId) {
        JSObject result = new JSObject();
        result.put("isPremium", isPremium);
        if (productId != null) {
            result.put("productId", productId);
        }
        call.resolve(result);

        // BILLING BRIDGE - Send premium status to JS
        if (isPremium) {
            sendPurchaseToJS(true, productId);
        }
    }

    // GOOGLE BILLING SYSTEM - Acknowledge purchase
    private void acknowledgePurchase(Purchase purchase) {
        if (purchase.isAcknowledged()) {
            return;
        }

        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged");
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
                }
            }
        });
    }

    // BILLING LISTENER - Handle purchase updates
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Purchase update error: " + billingResult.getDebugMessage());
            if (pendingPurchaseCall != null) {
                JSObject result = new JSObject();
                result.put("success", false);
                result.put("error", billingResult.getDebugMessage());
                pendingPurchaseCall.resolve(result);
                pendingPurchaseCall = null;
            }
            return;
        }

        if (purchases == null || purchases.isEmpty()) {
            Log.d(TAG, "No purchases to process");
            if (pendingPurchaseCall != null) {
                JSObject result = new JSObject();
                result.put("success", false);
                result.put("error", "No purchases found");
                pendingPurchaseCall.resolve(result);
                pendingPurchaseCall = null;
            }
            return;
        }

        // GOOGLE BILLING SYSTEM - Process purchases and check for valid product IDs
        boolean hasPremium = false;
        String foundProductId = null;
        
        for (Purchase purchase : purchases) {
            List<String> products = purchase.getProducts();
            if (products != null) {
                for (String product : products) {
                    // Check for both monthly subscription and lifetime purchase
                    if (product.equals("fastmind_premium_monthly") || product.equals("fastmind_premium_lifetime")) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            // Acknowledge purchase
                            acknowledgePurchase(purchase);
                            hasPremium = true;
                            foundProductId = product;
                            break;
                        }
                    }
                }
            }
        }

        // BILLING BRIDGE - Send result to JS
        if (pendingPurchaseCall != null) {
            JSObject result = new JSObject();
            result.put("success", hasPremium);
            result.put("isPremium", hasPremium);
            if (foundProductId != null) {
                result.put("productId", foundProductId);
            }
            pendingPurchaseCall.resolve(result);
            pendingPurchaseCall = null;
        }

        // Send premium status to JS
        if (hasPremium) {
            sendPurchaseToJS(true, foundProductId);
        }
    }

    // GOOGLE BILLING SYSTEM - Send purchase status to JavaScript
    private void sendPurchaseToJS(boolean isPremium, String productId) {
        try {
            JSObject data = new JSObject();
            data.put("isPremium", isPremium);
            // Use actual productId if provided, otherwise default
            data.put("productId", productId != null ? productId : "fastmind_premium");
            
            // BILLING BRIDGE - Notify JS via Capacitor
            notifyListeners("purchaseUpdate", data);
        } catch (Exception e) {
            Log.e(TAG, "Error sending purchase to JS", e);
        }
    }

    // GOOGLE BILLING SYSTEM - Get all purchases
    @PluginMethod
    public void getPurchases(PluginCall call) {
        try {
            if (!isServiceConnected || billingClient == null) {
                call.reject("Billing service not connected");
                return;
            }

            // GOOGLE BILLING SYSTEM - Query both subscription and one-time purchases
            final List<JSObject> allPurchases = new ArrayList<>();
            final int[] queriesCompleted = {0};
            final int totalQueries = 2;

            // Query subscriptions
            QueryPurchasesParams subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();

            billingClient.queryPurchasesAsync(subsParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        try {
                            for (Purchase purchase : purchases) {
                                List<String> products = purchase.getProducts();
                                if (products != null && !products.isEmpty()) {
                                    for (String product : products) {
                                        // Only include our premium products
                                        if (product.equals("fastmind_premium_monthly") || product.equals("fastmind_premium_lifetime")) {
                                            JSObject purchaseObj = new JSObject();
                                            purchaseObj.put("productId", product);
                                            purchaseObj.put("purchaseToken", purchase.getPurchaseToken());
                                            purchaseObj.put("purchaseState", purchase.getPurchaseState());
                                            purchaseObj.put("isAcknowledged", purchase.isAcknowledged());
                                            allPurchases.add(purchaseObj);
                                            break; // Only add once per purchase
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing purchases", e);
                        }
                    }
                    
                    queriesCompleted[0]++;
                    if (queriesCompleted[0] >= totalQueries) {
                        JSObject result = new JSObject();
                        result.put("purchases", allPurchases);
                        call.resolve(result);
                    }
                }
            });

            // Query one-time purchases (INAPP)
            QueryPurchasesParams inappParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();

            billingClient.queryPurchasesAsync(inappParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        try {
                            for (Purchase purchase : purchases) {
                                List<String> products = purchase.getProducts();
                                if (products != null && !products.isEmpty()) {
                                    for (String product : products) {
                                        // Only include our premium products
                                        if (product.equals("fastmind_premium_monthly") || product.equals("fastmind_premium_lifetime")) {
                                            JSObject purchaseObj = new JSObject();
                                            purchaseObj.put("productId", product);
                                            purchaseObj.put("purchaseToken", purchase.getPurchaseToken());
                                            purchaseObj.put("purchaseState", purchase.getPurchaseState());
                                            purchaseObj.put("isAcknowledged", purchase.isAcknowledged());
                                            allPurchases.add(purchaseObj);
                                            break; // Only add once per purchase
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing purchases", e);
                        }
                    }
                    
                    queriesCompleted[0]++;
                    if (queriesCompleted[0] >= totalQueries) {
                        JSObject result = new JSObject();
                        result.put("purchases", allPurchases);
                        call.resolve(result);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting purchases", e);
            call.reject("Error getting purchases: " + e.getMessage());
        }
    }

    // GOOGLE BILLING SYSTEM - Cleanup BillingClient connection on plugin destroy
    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (billingClient != null) {
            try {
                billingClient.endConnection();
                billingClient = null;
                isServiceConnected = false;
                Log.d(TAG, "BillingClient connection closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing BillingClient connection", e);
            }
        }
        pendingPurchaseCall = null;
    }
}

