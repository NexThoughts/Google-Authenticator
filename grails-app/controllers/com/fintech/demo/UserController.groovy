package com.fintech.demo

import com.fintech.demo.demo.User
import com.fintech.util.TimeBasedOneTimePasswordUtil
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
        if(user.save(flush: true)){
            render(view:'profile.gsp')
        }else{
            user.errors.each {
                println it
            }
        }
    }

    def doLogin(){
        Map result = [:]
        String base32Secret = "NY4A5CPJZ46LXZCP";
        String username = params.username
        String password = params.password
        Integer secretKey = params.secretKey as Integer
        User user = User.findByUsernameAndPassword(username , password)
        if(user && secretKey){
            if(TimeBasedOneTimePasswordUtil.validateCurrentNumber(base32Secret , secretKey , 0  , System.currentTimeMillis() , 30)){
                result.message = "Logged In"
                result.success = true
            }else{
                result.message = "Could not log you In"
                result.success = false
            }
        }
        render result as JSON
    }
    def generateQrCode() {
        Map result = [:]
        String username = params.username
        User user = User.findByUsername(username)
        if (user) {
            String base32Secret = "NY4A5CPJZ46LXZCP";
            String qrCode = TimeBasedOneTimePasswordUtil.qrImageUrl(username, base32Secret)
            user.qrCode = qrCode
            user.save(flush: true)
            result.qrCode = qrCode
        }
        render result as JSON
    }
}
