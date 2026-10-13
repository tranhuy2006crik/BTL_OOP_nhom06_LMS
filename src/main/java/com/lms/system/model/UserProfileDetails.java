package com.lms.system.model;

public record UserProfileDetails(
        String emailVisibility,
        String cityTown,
        String country,
        String timezone,
        String preferredTheme,
        String description,
        String pictureDescription,
        String firstNamePhonetic,
        String lastNamePhonetic,
        String middleName,
        String alternateName,
        String interests,
        String idNumber,
        String institution,
        String department,
        String phone,
        String mobilePhone,
        String address,
        String skypeId,
        String website
) {
    public UserProfileDetails {
        emailVisibility = requireChoice(emailVisibility, "COURSE_PARTICIPANTS", "HIDDEN", "email visibility");
        cityTown = optionalText(cityTown, 100, "city/town");
        country = optionalText(country, 100, "country");
        timezone = optionalText(timezone, 80, "timezone");
        preferredTheme = requireChoice(preferredTheme, "DEFAULT", "LIGHT", "DARK", "preferred theme");
        description = optionalText(description, 10_000, "description");
        pictureDescription = optionalText(pictureDescription, 255, "picture description");
        firstNamePhonetic = optionalText(firstNamePhonetic, 100, "first name phonetic");
        lastNamePhonetic = optionalText(lastNamePhonetic, 100, "last name phonetic");
        middleName = optionalText(middleName, 100, "middle name");
        alternateName = optionalText(alternateName, 100, "alternate name");
        interests = optionalText(interests, 2_000, "interests");
        idNumber = optionalText(idNumber, 100, "ID number");
        institution = optionalText(institution, 150, "institution");
        department = optionalText(department, 150, "department");
        phone = optionalText(phone, 40, "phone");
        mobilePhone = optionalText(mobilePhone, 40, "mobile phone");
        address = optionalText(address, 255, "address");
        skypeId = optionalText(skypeId, 100, "Skype ID");
        website = optionalText(website, 255, "website");
    }

    private static String optionalText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private static String requireChoice(String value, String first, String second, String field) {
        if (!first.equals(value) && !second.equals(value)) {
            throw new IllegalArgumentException("Unsupported " + field);
        }
        return value;
    }

    private static String requireChoice(String value, String first, String second, String third, String field) {
        if (!first.equals(value) && !second.equals(value) && !third.equals(value)) {
            throw new IllegalArgumentException("Unsupported " + field);
        }
        return value;
    }
}
