package com.fintech.demo.demo

class User {

    String username
    String password
    String mobileNumber
    String qrCode
    Boolean isActivated = Boolean.FALSE

    static constraints = {
        username unique: true
        qrCode nullable: true
    }
}
