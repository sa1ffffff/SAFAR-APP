# SAFAR Email.js Templates

Copy-paste these templates into your Email.js dashboard at https://www.emailjs.com/

---

## Template 1: Welcome Email
**Template ID:** `template_welcome`

### Template Variables:
- `{{user_name}}` - User's full name
- `{{user_email}}` - User's email address
- SAFAR - Your Trusted Ride Companion

### Subject Line:
```
Welcome to SAFAR, {{user_name}}! 🚗
```

### HTML Content:
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Welcome to SAFAR</title>
</head>
<body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0a0a0a;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #111111;">
        <!-- Header -->
        <tr>
            <td style="padding: 40px 30px; text-align: center; background: linear-gradient(135deg, #FFD600 0%, #FFA000 100%);">
                <h1 style="margin: 0; font-size: 36px; font-weight: 800; color: #000000; letter-spacing: 4px;">SAFAR</h1>
                <p style="margin: 8px 0 0 0; font-size: 14px; color: #333333;">Your Trusted Ride Companion</p>
            </td>
        </tr>
        
        <!-- Main Content -->
        <tr>
            <td style="padding: 40px 30px;">
                <h2 style="margin: 0 0 20px 0; font-size: 24px; font-weight: 600; color: #ffffff;">Welcome aboard, {{user_name}}! 👋</h2>
                
                <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #b0b0b0;">
                    Thank you for joining SAFAR — Pakistan's premier carpool marketplace. We're thrilled to have you as part of our growing community of smart commuters.
                </p>
                
                <p style="margin: 0 0 30px 0; font-size: 16px; line-height: 1.6; color: #b0b0b0;">
                    With SAFAR, you can:
                </p>
                
                <!-- Features List -->
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom: 30px;">
                    <tr>
                        <td style="padding: 15px 20px; background-color: #1a1a1a; border-radius: 8px; margin-bottom: 10px;">
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td width="40" style="vertical-align: top;">
                                        <span style="font-size: 24px;">🚗</span>
                                    </td>
                                    <td style="padding-left: 15px;">
                                        <p style="margin: 0; font-size: 15px; font-weight: 600; color: #ffffff;">Post & Find Rides</p>
                                        <p style="margin: 4px 0 0 0; font-size: 13px; color: #888888;">Share your journey or find the perfect ride</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr><td style="height: 10px;"></td></tr>
                    <tr>
                        <td style="padding: 15px 20px; background-color: #1a1a1a; border-radius: 8px;">
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td width="40" style="vertical-align: top;">
                                        <span style="font-size: 24px;">💰</span>
                                    </td>
                                    <td style="padding-left: 15px;">
                                        <p style="margin: 0; font-size: 15px; font-weight: 600; color: #ffffff;">Save Money</p>
                                        <p style="margin: 4px 0 0 0; font-size: 13px; color: #888888;">Split costs and reduce travel expenses</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr><td style="height: 10px;"></td></tr>
                    <tr>
                        <td style="padding: 15px 20px; background-color: #1a1a1a; border-radius: 8px;">
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td width="40" style="vertical-align: top;">
                                        <span style="font-size: 24px;">🌱</span>
                                    </td>
                                    <td style="padding-left: 15px;">
                                        <p style="margin: 0; font-size: 15px; font-weight: 600; color: #ffffff;">Go Green</p>
                                        <p style="margin: 4px 0 0 0; font-size: 13px; color: #888888;">Reduce your carbon footprint together</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                
                <!-- CTA Button -->
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                    <tr>
                        <td style="text-align: center; padding: 10px 0 30px 0;">
                            <a href="https://play.google.com/store/apps/details?id=com.safar.app" 
                               style="display: inline-block; padding: 16px 40px; background-color: #FFD600; color: #000000; text-decoration: none; font-size: 16px; font-weight: 700; border-radius: 8px; letter-spacing: 0.5px;">
                                START YOUR JOURNEY
                            </a>
                        </td>
                    </tr>
                </table>
                
                <p style="margin: 0; font-size: 14px; line-height: 1.6; color: #888888;">
                    If you have any questions, simply reply to this email — our team is here to help.
                </p>
            </td>
        </tr>
        
        <!-- Footer -->
        <tr>
            <td style="padding: 30px; background-color: #0a0a0a; border-top: 1px solid #222222;">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                    <tr>
                        <td style="text-align: center;">
                            <p style="margin: 0 0 10px 0; font-size: 18px; font-weight: 700; color: #FFD600; letter-spacing: 2px;">SAFAR</p>
                            <p style="margin: 0 0 15px 0; font-size: 12px; color: #666666;">Your Trusted Ride Companion</p>
                            <p style="margin: 0; font-size: 11px; color: #444444;">
                                © 2024 SAFAR. All rights reserved.<br>
                                Karachi, Pakistan
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
```

---

## Template 2: Password Reset Email
**Template ID:** `template_password_reset`

### Template Variables:
- `{{user_name}}` - User's full name
- `{{user_email}}` - User's email address
- `{{reset_code}}` - 6-digit reset code

### Subject Line:
```
Your SAFAR Password Reset Code: {{reset_code}}
```

### HTML Content:
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Password Reset - SAFAR</title>
</head>
<body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0a0a0a;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #111111;">
        <!-- Header -->
        <tr>
            <td style="padding: 40px 30px; text-align: center; background-color: #181818; border-bottom: 3px solid #FFD600;">
                <h1 style="margin: 0; font-size: 32px; font-weight: 800; color: #FFD600; letter-spacing: 4px;">SAFAR</h1>
            </td>
        </tr>
        
        <!-- Security Icon -->
        <tr>
            <td style="padding: 40px 30px 20px 30px; text-align: center;">
                <div style="display: inline-block; width: 80px; height: 80px; background-color: #1a1a1a; border-radius: 50%; line-height: 80px;">
                    <span style="font-size: 40px;">🔐</span>
                </div>
            </td>
        </tr>
        
        <!-- Main Content -->
        <tr>
            <td style="padding: 0 30px 40px 30px;">
                <h2 style="margin: 0 0 20px 0; font-size: 24px; font-weight: 600; color: #ffffff; text-align: center;">Password Reset Request</h2>
                
                <p style="margin: 0 0 25px 0; font-size: 16px; line-height: 1.6; color: #b0b0b0; text-align: center;">
                    Hi {{user_name}},<br><br>
                    We received a request to reset your SAFAR account password. Use the verification code below to complete the process:
                </p>
                
                <!-- Reset Code Box -->
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom: 30px;">
                    <tr>
                        <td style="text-align: center;">
                            <div style="display: inline-block; padding: 25px 50px; background: linear-gradient(135deg, #1a1a1a 0%, #222222 100%); border: 2px solid #FFD600; border-radius: 12px;">
                                <p style="margin: 0 0 8px 0; font-size: 12px; color: #888888; text-transform: uppercase; letter-spacing: 2px;">Your Verification Code</p>
                                <p style="margin: 0; font-size: 40px; font-weight: 800; color: #FFD600; letter-spacing: 8px; font-family: 'Courier New', monospace;">{{reset_code}}</p>
                            </div>
                        </td>
                    </tr>
                </table>
                
                <!-- Timer Warning -->
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom: 30px;">
                    <tr>
                        <td style="padding: 15px 20px; background-color: #2a1a00; border-radius: 8px; border-left: 4px solid #FFD600;">
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td width="30" style="vertical-align: top;">
                                        <span style="font-size: 18px;">⏱️</span>
                                    </td>
                                    <td style="padding-left: 10px;">
                                        <p style="margin: 0; font-size: 14px; color: #FFD600; font-weight: 600;">This code expires in 15 minutes</p>
                                        <p style="margin: 4px 0 0 0; font-size: 13px; color: #b0b0b0;">For security reasons, this code can only be used once.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                
                <!-- Security Notice -->
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                    <tr>
                        <td style="padding: 15px 20px; background-color: #1a1a1a; border-radius: 8px;">
                            <p style="margin: 0 0 10px 0; font-size: 14px; color: #ffffff; font-weight: 600;">🛡️ Security Notice</p>
                            <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #888888;">
                                If you didn't request this password reset, please ignore this email. Your account remains secure, and no changes have been made.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        
        <!-- Footer -->
        <tr>
            <td style="padding: 30px; background-color: #0a0a0a; border-top: 1px solid #222222;">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                    <tr>
                        <td style="text-align: center;">
                            <p style="margin: 0 0 10px 0; font-size: 18px; font-weight: 700; color: #FFD600; letter-spacing: 2px;">SAFAR</p>
                            <p style="margin: 0 0 15px 0; font-size: 12px; color: #666666;">Your Trusted Ride Companion</p>
                            <p style="margin: 0 0 10px 0; font-size: 11px; color: #444444;">
                                This is an automated security email. Please do not reply.
                            </p>
                            <p style="margin: 0; font-size: 11px; color: #444444;">
                                © 2024 SAFAR. All rights reserved.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
```

---

## Email.js Setup Instructions

### Step 1: Create Email.js Account
1. Go to https://www.emailjs.com/
2. Sign up for a free account (200 emails/month free)

### Step 2: Create Email Service
1. Go to **Email Services** → **Add New Service**
2. Choose your email provider (Gmail recommended)
3. Connect your email account
4. Note down your **Service ID** (e.g., `service_abc123`)

### Step 3: Create Templates
1. Go to **Email Templates** → **Create New Template**
2. For each template above:
   - Set the **Template ID** as specified
   - Paste the **Subject Line** in the Subject field
   - Paste the **HTML Content** in the Content field
   - Save the template

### Step 4: Get API Keys
1. Go to **Account** → **API Keys**
2. Copy your **Public Key**

### Step 5: Update SAFAR Code
Open `app/src/main/java/com/safar/app/utils/EmailService.java` and replace:
```java
private static final String PUBLIC_KEY = "YOUR_EMAILJS_PUBLIC_KEY";  // Paste your Public Key
private static final String SERVICE_ID = "YOUR_EMAILJS_SERVICE_ID";  // Paste your Service ID
```

---

## Testing

### Test Welcome Email
After signing up a new user, check if welcome email arrives with:
- Correct user name
- All features listed
- Working CTA button

### Test Password Reset
1. Click "Forgot Password" in the app
2. Enter email
3. Check email for reset code
4. Verify code displays correctly
5. Verify 15-minute expiry warning

---

## Pro Tips

1. **Test Mode**: Email.js has a test mode - use it before going live
2. **Spam Filters**: Add your Email.js sending address to whitelist
3. **Mobile Preview**: Test emails on mobile devices
4. **Fallback**: Plain text version auto-generated by Email.js
5. **Analytics**: Track open rates in Email.js dashboard

---

**Templates Created**: 2  
**Ready for Production**: ✅  
**Estimated Setup Time**: 15 minutes
