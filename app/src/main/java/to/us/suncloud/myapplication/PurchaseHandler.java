package to.us.suncloud.myapplication;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PurchaseHandler {
    String AD_TAG = "ADS";

    BillingClient client;
    PurchasesUpdatedListener purchaseListener;
    purchaseHandlerInterface parent;
    HashMap<String, SkuDetails> allSKUDetailsMap = new HashMap<>(); // The SKU details of the in-app purchase option (retrieved from the asynchronous call to the billing client)

    int billConnectionTries = 0;
    final static int BILL_MAX_TRIES = 5;

    PurchaseHandler(final purchaseHandlerInterface parent, final List<String> allSKUs) {
        this.parent = parent;

        purchaseListener = new PurchasesUpdatedListener() { // A listener that receives updates on purchasing
            // Handle brand new purchases
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (Purchase purchase : purchases) {
                        purchase.getPurchaseToken(); // TODO: Do something with the token.  Email it to me, or dedicated email address?
                        handlePurchase(purchase);
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            // If the user just bought this, thank them!
                            onNewPurchase(purchase);
                        }
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // TODO: Handle an error caused by a user cancelling the purchase flow
                } else {
                    // Handle any other error codes.
                }
            }
        };

        client = BillingClient.newBuilder(parent.getContext())
                .setListener(purchaseListener)
                .enablePendingPurchases()
                .build(); // Build the client through which we will make purchases

        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready, query available purchases (damn well better be only one...)

                    List<String> skuList = new ArrayList<>(allSKUs);
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                    client.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                                    // Process the result
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        if (skuDetailsList != null) {
                                            for (SkuDetails details : skuDetailsList) {
                                                allSKUDetailsMap.put(details.getSku(), details); // Save each details object, mapped according to its SKU
                                            }
//TODO:                                            purchasePref.setEnabled(true); // We got info from the Play store, so we can allow the purchase flow to begin
                                        }
                                    } else {
                                        Log.w(AD_TAG, "Could not retrieve SKU details: " + billingResult.getDebugMessage());
                                    }

                                    // Now that we have SKU details, update the GUI
                                    queryPurchases();
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // ...Uh-oh
                billConnectionTries++;
                if (billConnectionTries < BILL_MAX_TRIES) {
                    // Try connecting again, up to 5 times
                    client.startConnection(this);
                } else {
                    Log.e(AD_TAG, "Could not connect to Billing Service.");
//                        Toast.makeText(getContext(), "Could not connect to billing service", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        // Do everything required to handle each purchase as its details get resolved
        parent.handlePurchase(purchase);
        ackPurchase(purchase);
    }

    public void startPurchase(String sku) {
        if (allSKUDetailsMap.containsKey(sku)) {
            // First, double check that we should still be allowed to make this purchase (we haven't purchased it before already)
            boolean canBuy = false;
            Purchase.PurchasesResult result = client.queryPurchases(BillingClient.SkuType.INAPP);
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // We now have a list of purchases
                for (Purchase p : result.getPurchasesList()) {
                    if (p.getSku().equals(sku)) {
                        // We found the remove ads Purchase
                        if (p.getPurchaseState() != Purchase.PurchaseState.PURCHASED && p.getPurchaseState() != Purchase.PurchaseState.PENDING) {
                            canBuy = true; // If it is NOT purchased (and a purchase isn't pending for), we can buy the entitlement
                        }
                    }
                }
            } else {
                Log.w(AD_TAG, "Could not query purchases, got result: " + result.getBillingResult().getDebugMessage());
                Toast.makeText(parent.getContext(), "There was an error connecting to the Play store", Toast.LENGTH_SHORT).show();
            }

            if (canBuy) {
                // The removeAds entitlement is ready to be purchased
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(allSKUDetailsMap.get(sku))
                        .build();

                int responseCode = client.launchBillingFlow(parent.getActivity(), billingFlowParams).getResponseCode();

                if (responseCode != BillingClient.BillingResponseCode.OK) {
                    // Something has gone wrong
                    Toast.makeText(parent.getContext(), "Payment screen could not be launched, please check your internet connection.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // If the removeAds entitlement was already purchased, then let the user know they are silly
                Toast.makeText(parent.getContext(), "Ads have already been removed!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(parent.getContext(), "There was an error connecting to the Play store", Toast.LENGTH_SHORT).show();
        }
    }

    private void onNewPurchase(Purchase purchase) {
        // A new purchase has been made!
        Toast.makeText(parent.getContext(), "Thank you for supporting the app! Enjoy!", Toast.LENGTH_SHORT).show();
    }

    public void queryPurchases() {
        // Query the client for purchases, and react accordingly

        // Check the purchase status
        if (client != null) {
            Purchase.PurchasesResult result = client.queryPurchases(BillingClient.SkuType.INAPP);
            int responseCode = result.getResponseCode();
            if (responseCode == BillingClient.BillingResponseCode.OK) {
                // We now have a list of purchases
                List<Purchase> pList = result.getPurchasesList();
                for (Purchase p : pList) {
                    handlePurchase(p); // Go through each one and handle them as needed
                }
            } else {
                Log.w(AD_TAG, "Could not query purchases, got result: " + result.getBillingResult().getDebugMessage());
            }
        }
    }

    public void ackPurchase(Purchase purchase) {
        // Acknowledge the purchase
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams =
                    AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
            client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {

                }
            });
        }
    }

    public boolean isPurchaseFlowReady(String sku) {
        // If the details map contains the key, then we have the details that we need to begin the purchasing flow
        return allSKUDetailsMap.containsKey(sku);
    }

    interface purchaseHandlerInterface{
        Context getContext(); // Receive a context from the parent
        Activity getActivity(); // Receive an Activity from the parent
        void handlePurchase(Purchase purchase);  // Handle each purchase that is found by queryPurchases or onPurchasesUpdated (a new purchase bought)
    }
}
