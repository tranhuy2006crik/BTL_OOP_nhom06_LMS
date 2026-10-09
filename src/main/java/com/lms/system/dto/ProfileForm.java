package com.lms.system.dto;

import com.lms.system.model.User;
import com.lms.system.model.UserProfileDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {

    private String fullName;
    @NotBlank(message = "Tên không được để trống")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String firstName;
    @Size(max = 150, message = "Họ tối đa 150 ký tự")
    private String lastName;
    private String emailVisibility = "COURSE_PARTICIPANTS";
    @Size(max = 100)
    private String cityTown;
    @Size(max = 100)
    private String country;
    @Size(max = 80)
    private String timezone;
    private String preferredTheme = "DEFAULT";
    @Size(max = 10000)
    private String description;
    @Size(max = 255)
    private String pictureDescription;
    @Size(max = 100)
    private String firstNamePhonetic;
    @Size(max = 100)
    private String lastNamePhonetic;
    @Size(max = 100)
    private String middleName;
    @Size(max = 100)
    private String alternateName;
    @Size(max = 2000)
    private String interests;
    @Size(max = 100)
    private String idNumber;
    @Size(max = 150)
    private String institution;
    @Size(max = 150)
    private String department;
    @Size(max = 40)
    private String phone;
    @Size(max = 40)
    private String mobilePhone;
    @Size(max = 255)
    private String address;
    @Size(max = 100)
    private String skypeId;
    @Size(max = 255)
    private String website;

    public ProfileForm() {
    }

    public ProfileForm(String fullName) {
        this.fullName = fullName;
    }

    public ProfileForm(User user) {
        fullName = user.getFullName();
        String name = user.getFullName().trim();
        int nameBoundary = name.indexOf(' ');
        if (nameBoundary < 0) {
            firstName = name;
            lastName = "";
        } else {
            firstName = name.substring(0, nameBoundary);
            lastName = name.substring(nameBoundary + 1);
        }
        emailVisibility = user.getEmailVisibility();
        cityTown = user.getCityTown();
        country = user.getCountry();
        timezone = user.getTimezone();
        preferredTheme = user.getPreferredTheme();
        description = user.getDescription();
        pictureDescription = user.getPictureDescription();
        firstNamePhonetic = user.getFirstNamePhonetic();
        lastNamePhonetic = user.getLastNamePhonetic();
        middleName = user.getMiddleName();
        alternateName = user.getAlternateName();
        interests = user.getInterests();
        idNumber = user.getIdNumber();
        institution = user.getInstitution();
        department = user.getDepartment();
        phone = user.getPhone();
        mobilePhone = user.getMobilePhone();
        address = user.getAddress();
        skypeId = user.getSkypeId();
        website = user.getWebsite();
    }

    public UserProfileDetails toUserProfileDetails() {
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

    @NotBlank(message = "Họ tên không được trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return fullName;
        }
        String normalizedFirstName;
        if (firstName == null) {
            normalizedFirstName = "";
        } else {
            normalizedFirstName = firstName.trim();
        }
        String normalizedLastName;
        if (lastName == null) {
            normalizedLastName = "";
        } else {
            normalizedLastName = lastName.trim();
        }
        return (normalizedFirstName + " " + normalizedLastName).trim();
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        this.firstName = null;
        this.lastName = null;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmailVisibility() { return emailVisibility; }
    public void setEmailVisibility(String emailVisibility) { this.emailVisibility = emailVisibility; }
    public String getCityTown() { return cityTown; }
    public void setCityTown(String cityTown) { this.cityTown = cityTown; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPictureDescription() { return pictureDescription; }
    public void setPictureDescription(String pictureDescription) { this.pictureDescription = pictureDescription; }
    public String getFirstNamePhonetic() { return firstNamePhonetic; }
    public void setFirstNamePhonetic(String firstNamePhonetic) { this.firstNamePhonetic = firstNamePhonetic; }
    public String getLastNamePhonetic() { return lastNamePhonetic; }
    public void setLastNamePhonetic(String lastNamePhonetic) { this.lastNamePhonetic = lastNamePhonetic; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getAlternateName() { return alternateName; }
    public void setAlternateName(String alternateName) { this.alternateName = alternateName; }
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getSkypeId() { return skypeId; }
    public void setSkypeId(String skypeId) { this.skypeId = skypeId; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
