package com.grispi.grispiimport.zendesk

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource

class PhoneNumberValidator {

    companion object {
        private val phoneNumberUtil = PhoneNumberUtil.getInstance()

        fun isValid(number: String): Boolean {
            try {
                val phoneNumber: PhoneNumber = phoneNumberUtil.parse(number, CountryCodeSource.UNSPECIFIED.name)
                phoneNumberUtil.format(phoneNumber, PhoneNumberFormat.E164)
                return true
            } catch (ex: NumberParseException) {
                return false;
            }
        }
    }

}