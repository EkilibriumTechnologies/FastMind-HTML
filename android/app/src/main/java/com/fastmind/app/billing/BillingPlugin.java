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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CapacitorPlugin(name = "BillingPlugin")
public class BillingPlugin extends Plugin implements PurchasesUpdatedListener {

    private static final String TAG = "BillingPlugin";
    
    // VALID PRODUCT IDs - Only these two products are supported
    private static final String PRODUCT_ID_MONTHLY = "fastmind_premium_monthly";
    private static final String PRODUCT_ID_LIFETIME = "fastmind_premium_lifetime";
    private static final Set<String> VALID_PRODUCT_IDS = new HashSet<>(Arrays.asList(
        PRODUCT_ID_MONTHLY,
        PRODUCT_ID_LIFETIME
    ));
    
    private BillingClient billingClient;
    private boolean isServiceConnected = false;
    private PluginCall pendingPurchaseCall;

    // GOOGLE BILLING SYSTEM - Helper method to detect product type as string (Billing v6 uses strings)
    private String detectProductType(String productId) {
        if (productId == null) {
            return null;
        }
        // Lifetime is a one-time purchase (non-consumable in-app product)
        if (PRODUCT_ID_LIFETIME.equals(productId)) {
            return BillingClient.ProductType.INAPP;
        }
        // Monthly is a subscription
        if (PRODUCT_ID_MONTHLY.equals(productId)) {
            return BillingClient.ProductType.SUBS;
        }
        return null;
    }

    // GOOGLE BILLING SYSTEM - Validate product ID
    private boolean isValidProductId(String productId) {
        return productId != null && VALID_PRODUCT_IDS.contains(productId);
    }

    // GOOGLE BILLING SYSTEM - Check if purchase grants premium access
    private boolean isPremiumPurchase(Purchase purchase) {
        if (purchase == null || purchase.getProducts() == null) {
            return false;
        }
        
        List<String> products = purchase.getProducts();
        for (String productId : products) {
            // Only check valid product IDs
            if (isValidProductId(productId)) {
                // For subscriptions, check if purchase state is PURCHASED
                // Note: Google Play automatically marks expired subscriptions as expired,
                // but we verify purchase state here
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    return true;
                }
            }
        }
        return false;
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

            String productId = call.getString("productId");
            
            // Validate product ID
            if (!isValidProductId(productId)) {
                Log.e(TAG, "Invalid or missing productId: " + productId);
                call.reject("Invalid product ID. Must be: " + PRODUCT_ID_MONTHLY + " or " + PRODUCT_ID_LIFETIME);
                return;
            }

            pendingPurchaseCall = call;

            // GOOGLE BILLING SYSTEM - Determine product type dynamically (SUBS for monthly, INAPP for lifetime)
            final String productType = detectProductType(productId);
            if (productType == null) {
                Log.e(TAG, "Could not determine product type for: " + productId);
                call.reject("Unknown product type");
                pendingPurchaseCall = null;
                return;
            }

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

                    if (productDetailsList == null || productDetailsList.isEmpty()) {
                        Log.e(TAG, "Product not found: " + productId);
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Product not found in Google Play Console");
                            pendingPurchaseCall = null;
                        }
                        return;
                    }

                    ProductDetails productDetails = productDetailsList.get(0);
                    
                    // Verify we got the correct product
                    if (!productId.equals(productDetails.getProductId())) {
                        Log.e(TAG, "Product ID mismatch: expected " + productId + ", got " + productDetails.getProductId());
                        if (pendingPurchaseCall != null) {
                            pendingPurchaseCall.reject("Product ID mismatch");
                            pendingPurchaseCall = null;
                        }
                        return;
                    }
                    
                    // GOOGLE BILLING SYSTEM - Handle both subscription and one-time purchases
                    BillingFlowParams.ProductDetailsParams productDetailsParams;
                    
                    if (productType.equals(BillingClient.ProductType.SUBS)) {
                        // Subscription product - need offer token
                        List<SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
                        
                        if (offers == null || offers.isEmpty()) {
                            Log.e(TAG, "No subscription offers found for: " + productId);
                            if (pendingPurchaseCall != null) {
                                pendingPurchaseCall.reject("No subscription offers found");
                                pendingPurchaseCall = null;
                            }
                            return;
                        }

                        SubscriptionOfferDetails offerDetails = offers.get(0);
                        String offerToken = offerDetails.getOfferToken();
                        
                        if (offerToken == null || offerToken.isEmpty()) {
                            Log.e(TAG, "Offer token is null or empty");
                            if (pendingPurchaseCall != null) {
                                pendingPurchaseCall.reject("Invalid subscription offer");
                                pendingPurchaseCall = null;
                            }
                            return;
                        }

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
                result.put("reason", "Billing service not connected");
                call.resolve(result);
                return;
            }

            // GOOGLE BILLING SYSTEM - Query both subscription and one-time purchases
            final String[] foundProductId = {null};
            final boolean[] isPremium = {false};
            final int[] queriesCompleted = {0};
            final int totalQueries = 2;
            final PluginCall[] callRef = {call}; // Prevent race condition

            // Query subscriptions (SUBS) - only monthly subscription
            QueryPurchasesParams subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();

            billingClient.queryPurchasesAsync(subsParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (isPremiumPurchase(purchase)) {
                                // Check if this is the monthly subscription
                                List<String> products = purchase.getProducts();
                                if (products != null) {
                                    for (String product : products) {
                                        if (PRODUCT_ID_MONTHLY.equals(product)) {
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
                    } else {
                        Log.w(TAG, "Query subscriptions failed: " + billingResult.getDebugMessage());
                    }
                    
                    synchronized (callRef) {
                        queriesCompleted[0]++;
                        if (queriesCompleted[0] >= totalQueries && callRef[0] != null) {
                            finishPremiumCheck(callRef[0], isPremium[0], foundProductId[0]);
                            callRef[0] = null; // Prevent double resolution
                        }
                    }
                }
            });

            // Query one-time purchases (INAPP) - only lifetime purchase
            QueryPurchasesParams inappParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();

            billingClient.queryPurchasesAsync(inappParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (isPremiumPurchase(purchase)) {
                                // Check if this is the lifetime purchase
                                List<String> products = purchase.getProducts();
                                if (products != null) {
                                    for (String product : products) {
                                        if (PRODUCT_ID_LIFETIME.equals(product)) {
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
                    } else {
                        Log.w(TAG, "Query in-app purchases failed: " + billingResult.getDebugMessage());
                    }
                    
                    synchronized (callRef) {
                        queriesCompleted[0]++;
                        if (queriesCompleted[0] >= totalQueries && callRef[0] != null) {
                            finishPremiumCheck(callRef[0], isPremium[0], foundProductId[0]);
                            callRef[0] = null; // Prevent double resolution
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking premium status", e);
            JSObject result = new JSObject();
            result.put("isPremium", false);
            result.put("error", e.getMessage());
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
        if (isPremium && productId != null) {
            sendPurchaseToJS(true, productId);
        }
    }

    // GOOGLE BILLING SYSTEM - Acknowledge purchase
    private void acknowledgePurchase(Purchase purchase) {
        if (purchase == null || purchase.isAcknowledged() || billingClient == null) {
            return;
        }

        try {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully");
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error acknowledging purchase", e);
        }
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
            if (isPremiumPurchase(purchase)) {
                // Find the valid product ID in this purchase
                List<String> products = purchase.getProducts();
                if (products != null) {
                    for (String product : products) {
                        if (isValidProductId(product)) {
                            // Acknowledge purchase if needed
                            acknowledgePurchase(purchase);
                            hasPremium = true;
                            foundProductId = product;
                            break; // Use first valid product found
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
            if (!hasPremium) {
                result.put("error", "Purchase completed but no valid premium product found");
            }
            pendingPurchaseCall.resolve(result);
            pendingPurchaseCall = null;
        }

        // Send premium status to JS
        if (hasPremium && foundProductId != null) {
            sendPurchaseToJS(true, foundProductId);
        }
    }

    // GOOGLE BILLING SYSTEM - Send purchase status to JavaScript
    private void sendPurchaseToJS(boolean isPremium, String productId) {
        try {
            if (!isPremium || productId == null) {
                return;
            }
            
            JSObject data = new JSObject();
            data.put("isPremium", isPremium);
            data.put("productId", productId); // Use actual productId, no fallback
            
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
            final PluginCall[] callRef = {call}; // Prevent race condition

            // Query subscriptions (SUBS)
            QueryPurchasesParams subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();

            billingClient.queryPurchasesAsync(subsParams, new PurchasesResponseListener() {
                @Override
                public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        try {
                            for (Purchase purchase : purchases) {
                                List<String> products = purchase.getProducts();
                                if (products != null && !products.isEmpty()) {
                                    for (String product : products) {
                                        // Only include valid premium products
                                        if (PRODUCT_ID_MONTHLY.equals(product)) {
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
                    
                    synchronized (callRef) {
                        queriesCompleted[0]++;
                        if (queriesCompleted[0] >= totalQueries && callRef[0] != null) {
                            JSObject result = new JSObject();
                            result.put("purchases", allPurchases);
                            callRef[0].resolve(result);
                            callRef[0] = null; // Prevent double resolution
                        }
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
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        try {
                            for (Purchase purchase : purchases) {
                                List<String> products = purchase.getProducts();
                                if (products != null && !products.isEmpty()) {
                                    for (String product : products) {
                                        // Only include valid premium products
                                        if (PRODUCT_ID_LIFETIME.equals(product)) {
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
                    
                    synchronized (callRef) {
                        queriesCompleted[0]++;
                        if (queriesCompleted[0] >= totalQueries && callRef[0] != null) {
                            JSObject result = new JSObject();
                            result.put("purchases", allPurchases);
                            callRef[0].resolve(result);
                            callRef[0] = null; // Prevent double resolution
                        }
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
