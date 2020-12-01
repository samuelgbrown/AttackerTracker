package to.us.suncloud.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class PurchaseHandler implements Serializable {
    String PURCHASE_TAG = "ADS";

    BillingClient client;
    PurchasesUpdatedListener purchaseListener;
    purchaseHandlerInterface parent;
    HashMap<String, SkuDetails> allSKUDetailsMap = new HashMap<>(); // The SKU details of the in-app purchase option (retrieved from the asynchronous call to the billing client)

    int billConnectionTries = 0;
    final static int BILL_MAX_TRIES = 5;
    String TEST_SKU = "android.test.purchased";

    PurchaseHandler(final purchaseHandlerInterface parent, final List<String> allSKUs) {
        this.parent = parent;
        final List<String> TEST_SKU_LIST = new ArrayList<>();
        TEST_SKU_LIST.add(TEST_SKU);

        purchaseListener = new PurchasesUpdatedListener() { // A listener that receives updates on purchasing
            // Handle brand new purchases
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                    // Allow parent to handle each purchase SKU
                    handlePurchases(purchases);
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Log.d(PURCHASE_TAG, "User canceled purchase");
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

                    // TO_DO PURCHASE TEST
                    List<String> skuList = new ArrayList<>(allSKUs);
//                    List<String> skuList = TEST_SKU_LIST;
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
                                        }
                                    } else {
                                        Log.w(PURCHASE_TAG, "Could not retrieve SKU details: " + billingResult.getDebugMessage());
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
                    Log.w(PURCHASE_TAG, "Disconnected from Billing Service (try " + billConnectionTries + ")");
                } else {
                    Log.e(PURCHASE_TAG, "Could not connect to Billing Service.");
//                        Toast.makeText(getContext(), "Could not connect to billing service", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handlePurchases(List<Purchase> purchases) {
        // Do everything required to handle each purchase as its details get resolved
        // Send a Set of purchased SKU's to the parent
        parent.handlePurchases(purchasesToSKUSet(purchases));

        // Ensure that each purchase has been acknowledged
        postProcessPurchases(purchases);
    }

    public void startPurchase(String sku) {
        // TO_DO PURCHASE TEST
//        sku = TEST_SKU;
        if (haveContext()) {
            if (allSKUDetailsMap.containsKey(sku)) {
                // First, double check that we should still be allowed to make this purchase (we haven't purchased it before already)
                boolean canBuy = true; // Default to true, in case the purchase does not appear in the returned purchases list (meaning remove_ads has not been purchased, so we can buy it)
                Purchase.PurchasesResult result = client.queryPurchases(BillingClient.SkuType.INAPP);
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // We now have a list of purchases
                    List<Purchase> purchasesList = result.getPurchasesList();
                    for (Purchase p : purchasesList) {
                        if (p.getSku().equals(sku)) {
                            // We found the remove ads Purchase
                            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED && p.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                                canBuy = false; // If it is purchased (or a purchase is pending), we cannot buy the entitlement
                                break;
                            }
                        }
                    }
                } else {
                    Log.w(PURCHASE_TAG, "Could not query purchases, got result: " + result.getBillingResult().getDebugMessage());
                    Toast.makeText(parent.getContext(), "There was an error connecting to the Play store", Toast.LENGTH_SHORT).show();
                }

                if (canBuy) {
                    // The entitlement is ready to be purchased
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
    }

    private void onNewPurchase(Purchase purchase) {
        if (haveContext()) {
            // A new purchase has been made!
            Toast.makeText(parent.getContext(), "Thank you for supporting the app! Enjoy!", Toast.LENGTH_SHORT).show();
        }
    }

    public Purchase.PurchasesResult getQueriedPurchases() {
        // Get the purchases returned when querying the BillingClient
        if (client != null) {
            return client.queryPurchases(BillingClient.SkuType.INAPP);
        } else {
            return null;
        }
    }

    public void consumePurchase(final Purchase p) {
        // Consume the purchase, so it can be bought again
        // USE CAREFULLY
        ConsumeParams params = ConsumeParams.newBuilder().setPurchaseToken(p.getPurchaseToken()).build();
        client.consumeAsync(params, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                if (haveContext()) {
                    Toast.makeText(parent.getContext(), "Successfully consumed purchase SKU: " + p.getSku() + ".", Toast.LENGTH_SHORT).show();
                    Log.d(PURCHASE_TAG, "Consumed purchase SKU " + p.getSku() + ", purchase token " + p.getPurchaseToken() + ".");

                    // Now, query the purchases to refresh the display if needed
                    queryPurchases();
                }
            }
        });
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
                handlePurchases(pList);
//                for (Purchase p : pList) {
//                    handlePurchase(p); // Go through each one and handle them as needed
//                }
            } else {
                Log.w(PURCHASE_TAG, "Could not query purchases, got result: " + result.getBillingResult().getDebugMessage());
            }
        }
    }

    public void postProcessPurchases(List<Purchase> purchases) {
        if (haveContext()) {
            // Post-process all purchases (send token, send acknowledgement, thank user)
            SharedPreferences prefs = parent.getContext().getSharedPreferences(parent.getContext().getString(R.string.iap_shared_prefs_file), Context.MODE_PRIVATE);
            Set<String> wasPurchased = prefs.getStringSet(parent.getContext().getString(R.string.was_purchased), new HashSet<String>());
            Set<String> wasAcknowledged = prefs.getStringSet(parent.getContext().getString(R.string.was_acknowledged), new HashSet<String>());
            Set<String> tokenSent = prefs.getStringSet(parent.getContext().getString(R.string.token_sent), new HashSet<String>());
            if (!purchases.isEmpty()) {
                for (Purchase purchase : purchases) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        // If this item has been purchased, do any required post-processing (if they haven't been done already)
                        final String thisSKU = purchase.getSku();

                        // Is this a new purchase?
                        if (!wasPurchased.contains(thisSKU)) {
                            // If has not yet been purchased before, then record it, and use this opportunity to perform any just-purchased actions
                            onNewPurchase(purchase);

                            setPref(R.string.was_purchased, thisSKU); // Note that we have made this purchase
                        }

                        // Check if the purchase was acknowledged
                        if (!wasAcknowledged.contains(thisSKU)) {
                            // If this SKU has not yet been acknowledged with Google, then do so
                            AcknowledgePurchaseParams acknowledgePurchaseParams =
                                    AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase.getPurchaseToken())
                                            .build();
                            client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                                @Override
                                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        // If the acknowledgement process worked, record it
                                        setPref(R.string.was_acknowledged, thisSKU);
                                    }
                                }
                            });
                        }

                        // Check if we sent the purchase token
                        if (!tokenSent.contains(thisSKU)) {
                            // If the purchase token has not yet been sent, then do so
                            sendPurchaseToken(purchase); // tokenSent will be updated upon successful email delivery
                        }
                    }
                }
            }

            // Clean up refunded purchases
            HashSet<String> refundedPurchases = new HashSet<>(wasPurchased);
            refundedPurchases.removeAll(purchasesToSKUSet(purchases)); // Find all purchases that were previously own (are in wasPurchased) but are no longer owned (are NOT in purchaseSKUSet), and clear them
            clearAllPrefs(refundedPurchases); // Clear these purchases from the SharedPreference object that tracks our acknowledgement progress (we will need to re-acknowledge after a new purchase, if it happens) and our app's default behavior
        }
    }

    private HashSet<String> purchasesToSKUSet(List<Purchase> purchases) {
        HashSet<String> purchaseSKUSet = new HashSet<>();
        for (Purchase p : purchases) {
            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                purchaseSKUSet.add(p.getSku());
            }
        }

        return purchaseSKUSet;
    }

    private void sendPurchaseToken(final Purchase p) {
        if (haveContext()) {
            final String username = parent.getContext().getString(R.string.mail_address);
            final String password = parent.getContext().getString(R.string.mail_p);

            // Attempt to send an email with the purchase token (and other meta-data) on a new thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Properties props = new Properties();
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.port", "587");

                    Session session = Session.getInstance(props,
                            new javax.mail.Authenticator() {
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(username, password);
                                }
                            });

                    try {
                        Message message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(username));
                        message.setRecipients(Message.RecipientType.TO,
                                InternetAddress.parse(username));
                        message.setSubject("Purchase Info");
                        message.setText("purchase_SKU: " + p.getSku() + "\n" +
                                "purchase_token: " + p.getPurchaseToken() + "\n" +
                                "order_id: " + p.getOrderId());

                        Transport.send(message);

                        setPref(R.string.token_sent, p.getSku());

                    } catch (AddressException e) {
                        Log.e(PURCHASE_TAG, "Got AddressException when attempting to send purchase token: " + e.toString());
                    } catch (MessagingException e) {
                        Log.e(PURCHASE_TAG, "Got MessagingException when attempting to send purchase token: " + e.toString());
                    }
                }
            }).start();
        }
    }

    private void setPref(int prefID, String SKUtoSet) {
        if (haveContext()) {
            // Add the given SKU to the Set<String> held by the shared preferences object with key prefID (used to keep track of which purchases have been already purchased, acknowledged, and have had the token sent to me)
            SharedPreferences prefs = parent.getContext().getSharedPreferences(parent.getContext().getString(R.string.iap_shared_prefs_file), Context.MODE_PRIVATE);
            Set<String> thisPrefSet = new HashSet<>(prefs.getStringSet(parent.getContext().getString(prefID), new HashSet<String>())); // Create a new HashSet, because the Set being returned from the SharedPreferences cannot be modified
            thisPrefSet.add(SKUtoSet);

            // Save the newly modified string set
            prefs.edit()
                    .putStringSet(parent.getContext().getString(prefID), thisPrefSet)
                    .apply();
        }
    }

    private void clearAllPrefs(Set<String> SKUToClear) {
        // Called when the app detects that a previously purchased item has been refunded
        // Remove these SKU's from the was_purchased, was_acknowledged, and token_sent SharedPreferences
        if (haveContext()) {
            if (!SKUToClear.isEmpty()) {
                SharedPreferences prefs = parent.getContext().getSharedPreferences(parent.getContext().getString(R.string.iap_shared_prefs_file), Context.MODE_PRIVATE);
                Set<String> wasPurchased = new HashSet<>(prefs.getStringSet(parent.getContext().getString(R.string.was_purchased), new HashSet<String>())); // Get modifiable copy of the wasPurchased Set
                Set<String> wasAcknowledged = new HashSet<>(prefs.getStringSet(parent.getContext().getString(R.string.was_acknowledged), new HashSet<String>())); // Get modifiable copy of the wasAcknowledged Set
                Set<String> tokenSent = new HashSet<>(prefs.getStringSet(parent.getContext().getString(R.string.token_sent), new HashSet<String>())); // Get modifiable copy of the tokenSent Set

                // Remove the refunded SKU's
                wasPurchased.removeAll(SKUToClear);
                wasAcknowledged.removeAll(SKUToClear);
                tokenSent.removeAll(SKUToClear);

                // Save these Sets to the SharedPreferences
                prefs.edit()
                        .putStringSet(parent.getContext().getString(R.string.was_purchased), wasPurchased)
                        .putStringSet(parent.getContext().getString(R.string.was_acknowledged), wasAcknowledged)
                        .putStringSet(parent.getContext().getString(R.string.token_sent), tokenSent)
                        .apply();
            }
        }
    }

    private boolean haveContext() {
        // Check if the parent is connected to a Context
        return parent.getContext() != null;
    }

    public boolean isPurchaseFlowReady(String sku) {
        // If the details map contains the key, then we have the details that we need to begin the purchasing flow
        // TO_DO PURCHASE TEST
//        sku = TEST_SKU;
        return allSKUDetailsMap.containsKey(sku);
    }

    public boolean wasPurchased(String SKU) {
        if (haveContext()) {
            // Was the item with the given SKU purchased *as of the last time we checked the billing client*
            // NOTE: This method is fast, but may not be accurate if a purchase was recently made/refunded. Use this to optimally initialize the Activity based on what we expect, but not to make decisions on whether or not a purchase has been made
            SharedPreferences prefs = parent.getContext().getSharedPreferences(parent.getContext().getString(R.string.iap_shared_prefs_file), Context.MODE_PRIVATE);

            // If the wasPurchased String Set contains the given SKU, then (as of the last time we connected with Google Play) this entitlement has been purchased and is active
            return prefs.getStringSet(parent.getContext().getString(R.string.was_purchased), new HashSet<String>()).contains(SKU);
        } else {
            // Default to false if all else fails
            return false;
        }
    }

    interface purchaseHandlerInterface {
        Context getContext(); // Receive a context from the parent

        Activity getActivity(); // Receive an Activity from the parent

        void handlePurchases(HashSet<String> purchase);  // Handle each purchase that is found by queryPurchases or onPurchasesUpdated (a new purchase bought)
    }
}
