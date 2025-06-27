import SwiftUI
import AVFoundation
import UserNotifications
import FirebaseMessaging

class AppDelegate: NSObject, UIApplicationDelegate {
    // Called when the user grants (or revokes) notification permissions
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // Pass token to FCM
        if FirebaseManager.shared.isConfigured {
            Messaging.messaging().apnsToken = deviceToken
        }
        
        // Put the token in PHP's memory too so the developer can fetch it manually if they want
        let tokenString = deviceToken.map { String(format: "%02x", $0) }.joined()
        NativePHPSetPushTokenC(tokenString)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("Failed to register for remote notifications:", error.localizedDescription)
    }
    
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken = fcmToken else { return }

        LaravelBridge.shared.send?(
            "Native\\Mobile\\Events\\PushNotification\\TokenGenerated",
            ["token": fcmToken]
        )
    }
}
