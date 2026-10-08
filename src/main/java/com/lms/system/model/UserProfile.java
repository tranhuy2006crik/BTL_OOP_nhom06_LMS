package com.lms.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Arrays;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id", columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_visibility", nullable = false, length = 30)
    private String emailVisibility = "COURSE_PARTICIPANTS";

    @Column(name = "city_town", length = 100)
    private String cityTown;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "timezone", length = 80)
    private String timezone;

    @Column(name = "preferred_theme", nullable = false, length = 20)
    private String preferredTheme = "DEFAULT";

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "picture_description", length = 255)
    private String pictureDescription;

    @Column(name = "first_name_phonetic", length = 100)
    private String firstNamePhonetic;

    @Column(name = "last_name_phonetic", length = 100)
    private String lastNamePhonetic;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "alternate_name", length = 100)
    private String alternateName;

    @Column(name = "interests", columnDefinition = "text")
    private String interests;

    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Column(name = "institution", length = 150)
    private String institution;

    @Column(name = "department", length = 150)
    private String department;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "mobile_phone", length = 40)
    private String mobilePhone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "skype_id", length = 100)
    private String skypeId;

    @Column(name = "website", length = 255)
    private String website;

    @Lob
    @Column(name = "profile_picture", columnDefinition = "mediumblob")
    private byte[] profilePicture;

    @Column(name = "profile_picture_content_type", length = 30)
    private String profilePictureContentType;

    protected UserProfile() {
    }

    UserProfile(User user) {
        this.user = user;
    }

    void update(UserProfileDetails details) {
        emailVisibility = details.emailVisibility();
        cityTown = details.cityTown();
        country = details.country();
        timezone = details.timezone();
        preferredTheme = details.preferredTheme();
        description = details.description();
        pictureDescription = details.pictureDescription();
        firstNamePhonetic = details.firstNamePhonetic();
        lastNamePhonetic = details.lastNamePhonetic();
        middleName = details.middleName();
        alternateName = details.alternateName();
        interests = details.interests();
        idNumber = details.idNumber();
        institution = details.institution();
        department = details.department();
        phone = details.phone();
        mobilePhone = details.mobilePhone();
        address = details.address();
        skypeId = details.skypeId();
        website = details.website();
    }

    void updatePicture(byte[] picture, String contentType) {
        if (picture == null) {
            profilePicture = null;
            profilePictureContentType = null;
        } else {
            profilePicture = Arrays.copyOf(picture, picture.length);
            profilePictureContentType = contentType;
        }
    }

    UserProfileDetails details() {
        return new UserProfileDetails(
                emailVisibility,
                cityTown,
                country,
                timezone,
                preferredTheme,
                description,
                pictureDescription,
                firstNamePhonetic,
                lastNamePhonetic,
                middleName,
                alternateName,
                interests,
                idNumber,
                institution,
                department,
                phone,
                mobilePhone,
                address,
                skypeId,
                website
        );
    }

    byte[] picture() {
        if (profilePicture == null) {
            return null;
        } else {
            return Arrays.copyOf(profilePicture, profilePicture.length);
        }
    }

    String pictureContentType() {
        return profilePictureContentType;
    }
}
