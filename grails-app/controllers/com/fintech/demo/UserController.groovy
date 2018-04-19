package com.fintech.demo

import com.fintech.demo.demo.User
import com.fintech.util.TimeBasedOneTimePasswordUtil
import com.warrenstrange.googleauth.GoogleAuthenticator
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig
import com.warrenstrange.googleauth.GoogleAuthenticatorKey
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator
import grails.converters.JSON

class UserController {

    def registerUser() {
        Map result = [:]
        String username = params.username
        String password = params.password
        User user = new User()
        user.username = username
        user.password = password;
        user.mobileNumber = params.mobileNumber
        if (user.save(flush: true)) {
            session['username'] = username
            redirect(action: 'dashboard', params: [username: username])
        } else {
            user.errors.each {
                println it
            }
            redirect(uri: "/")
        }
    }

    def doLogin() {
        Map result = [:]
        String username = params.username
        String password = params.password
        User user = User.findByUsernameAndPassword(username, password)
        if (user) {
            result.message = "Logged In"
            result.success = true
            session['username'] = username
            if (user?.qrCode) {
                session['isAuthorized'] = false
                redirect(action: "openTOTPValidate", params: [username: username])
            } else {
                redirect(action: "dashboard", params: [username: username])
            }
        } else {
            result.message = "Could not log you In"
            result.success = false
            render uri: "/"
        }
    }

    def generateQrCode() {
        Map result = [:]
        String username = params.username
        User user = User.findByUsername(username)
        if (user) {
            GoogleAuthenticatorQRGenerator.getOtpAuthURL()
            String base32Secret = "NY4A5CPJZ46LXZCP";
            String qrCode = TimeBasedOneTimePasswordUtil.qrImageUrl(username, base32Secret)
            user.qrCode = qrCode
            user.save(flush: true)
            result.qrCode = qrCode
        }
        render result as JSON
    }

    def generateGoogleAuthenticatorQRCode() {
        Map result = [:]
        String username = params.username
        GoogleAuthenticatorKey credentials
        User user = User.findByUsername(username)
        if (user) {
//            GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
//
//            final GoogleAuthenticatorKey key =
//                    googleAuthenticator.createCredentials(username);
//            final String secret = key.getKey();
//            final List<Integer> scratchCodes = key.getScratchCodes();
//
//            String otpAuthURL = GoogleAuthenticatorQRGenerator.getOtpAuthURL("Fintech Labs", username, key);

            String secretKey = TimeBasedOneTimePasswordUtil.generateBase32Secret()
            GoogleAuthenticatorConfig config =
                    new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                            .build();

            credentials =
                    new GoogleAuthenticatorKey.Builder(secretKey)
                            .setConfig(config)
                            .setVerificationCode(0)
                            .setScratchCodes(new ArrayList<Integer>())
                            .build();
            String code = GoogleAuthenticatorQRGenerator.getOtpAuthURL("Fintechlabs In", params.username, credentials);
            result.qrCode = code
            user.qrCode = secretKey
            user.save(flush: true)
        } else {
            result.error = true
            result.qrCode = ""
        }
        render result as JSON
    }

    def validateLoginTwoStep() {
        if (params.username && params.totpValue) {
            String totp = params.totpValue
            String username = params.username
            User user = User.findByUsername(username)
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            boolean isCodeValid = gAuth.authorize(user?.qrCode, totp as Integer);
            if (isCodeValid)
                redirect(action: 'dashboard')
            else
                redirect(action: 'openTOTPValidate')
        } else {
            redirect(uri: "/")
        }

    }

    def openTOTPValidate() {
        if (params.username) {
            render view: "/demo/recieveOTP", model: [username: session.getAttribute('username')]
        } else {
            redirect(uri: "/")
        }
    }

    def dashboard() {
        if (session.getAttribute('username')) {
            render view: "/demo/dashboard", model: [username: session.getAttribute('username')]
        } else {
            redirect(uri: "/")
        }
    }

    def logout() {
        session.invalidate()
        redirect(uri: "/")
    }
}
