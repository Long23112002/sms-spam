package com.example.sms_app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import timber.log.Timber

object IntentUtils {
    
    /**
     * Mở Facebook profile
     * @param context Context
     * @param facebookUrl URL Facebook (https://www.facebook.com/giay.hien.90)
     */
    fun openFacebook(context: Context, facebookUrl: String = "https://www.facebook.com/giay.hien.90") {
        try {
            // Thử mở bằng Facebook app trước
            val facebookIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("fb://profile/giay.hien.90")
                setPackage("com.facebook.katana")
            }
            
            // Kiểm tra xem Facebook app có cài đặt không
            if (facebookIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(facebookIntent)
                Timber.d("✅ Opened Facebook with app")
            } else {
                // Nếu không có Facebook app, mở bằng browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl))
                context.startActivity(browserIntent)
                Timber.d("✅ Opened Facebook with browser")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error opening Facebook")
            Toast.makeText(context, "❌ Không thể mở Facebook", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Mở Zalo chat với số điện thoại
     * @param context Context
     * @param phoneNumber Số điện thoại (0383479698)
     */
    fun openZalo(context: Context, phoneNumber: String = "0383479698") {
        try {
            // Thử mở bằng Zalo app trước
            val zaloIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://zalo.me/$phoneNumber")
                setPackage("com.zing.zalo")
            }
            
            // Kiểm tra xem Zalo app có cài đặt không
            if (zaloIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(zaloIntent)
                Timber.d("✅ Opened Zalo with app")
            } else {
                // Nếu không có Zalo app, thử mở bằng browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/$phoneNumber"))
                context.startActivity(browserIntent)
                Timber.d("✅ Opened Zalo with browser")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error opening Zalo")
            // Fallback: Mở dialer với số điện thoại
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                context.startActivity(dialIntent)
                Toast.makeText(context, "📞 Mở dialer: $phoneNumber", Toast.LENGTH_SHORT).show()
                Timber.d("✅ Opened dialer as fallback")
            } catch (dialException: Exception) {
                Timber.e(dialException, "❌ Error opening dialer")
                Toast.makeText(context, "❌ Không thể mở Zalo hoặc dialer", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Mở email client
     * @param context Context
     * @param email Email address
     * @param subject Email subject
     */
    fun openEmail(context: Context, email: String, subject: String = "Hỗ trợ SMS App") {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            context.startActivity(Intent.createChooser(emailIntent, "Gửi email"))
            Timber.d("✅ Opened email client")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error opening email")
            Toast.makeText(context, "❌ Không thể mở email", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Mở browser với URL
     * @param context Context
     * @param url URL to open
     */
    fun openBrowser(context: Context, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
            Timber.d("✅ Opened browser with URL: $url")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error opening browser")
            Toast.makeText(context, "❌ Không thể mở trình duyệt", Toast.LENGTH_SHORT).show()
        }
    }
}
