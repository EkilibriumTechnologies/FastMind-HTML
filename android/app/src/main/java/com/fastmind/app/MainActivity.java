package com.fastmind.app;

// GOOGLE BILLING SYSTEM - Billing Plugin is auto-discovered via @CapacitorPlugin annotation
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    // BILLING BRIDGE - BillingPlugin is automatically registered via @CapacitorPlugin annotation
    // No manual registration needed - BridgeActivity.onCreate() handles plugin initialization
}
