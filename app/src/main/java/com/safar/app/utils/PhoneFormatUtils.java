package com.safar.app.utils;

import java.util.regex.Pattern;

/**
 * Utility methods for phone format handling used by WhatsApp flow.
 */
public final class PhoneFormatUtils {

    private static final Pattern WHATSAPP_LOCAL_PATTERN = Pattern.compile("^03\\d{2}-\\d{7}$");

    private PhoneFormatUtils() {
        throw new AssertionError("No instances.");
    }

    /**
     * Validates strict local format: 03xx-xxxxxxx
     */
    public static boolean isValidWhatsappLocalNumber(String phone) {
        return phone != null && WHATSAPP_LOCAL_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Normalizes user input into 03xx-xxxxxxx typing format.
     */
    public static String normalizeWhatsappLocalInput(String input) {
        if (input == null) return "";

        String digits = input.replaceAll("[^0-9]", "");
        if (digits.startsWith("0092")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("92")) {
            digits = "0" + digits.substring(2);
        }

        if (digits.length() > 11) {
            digits = digits.substring(0, 11);
        }
        if (digits.length() <= 4) {
            return digits;
        }
        return digits.substring(0, 4) + "-" + digits.substring(4);
    }

    /**
     * Converts 03xx-xxxxxxx to WhatsApp international numeric format: 92xxxxxxxxxx
     */
    public static String toWhatsappInternationalNumber(String formattedPhone) {
        if (!isValidWhatsappLocalNumber(formattedPhone)) return null;
        String digits = formattedPhone.replace("-", "");
        return "92" + digits.substring(1);
    }
}

