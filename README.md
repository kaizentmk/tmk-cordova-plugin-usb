# tmk-cordova-plugin-usb

## folders and files

* /src          - platform sources (android -> java)
* /www          - front end js
* plugin.xml    - plugin config and mapping

## info

[Cordova Plugin Development Guide](https://cordova.apache.org/docs/en/latest/guide/hybrid/plugins/index.html)

[Plugin Specification](https://cordova.apache.org/docs/en/latest/plugin_ref/spec.html)

[Cordova Hello World Plugin](https://github.com/don/cordova-plugin-hello)

[cordova-plugin-usb-event](https://www.npmjs.com/package/cordova-plugin-usb-event)

[How to Create a Cordova Plugin from Scratch](https://www.outsystems.com/blog/posts/how-to-create-a-cordova-plugin-from-scratch/)

[How to create an Android Cordova plugin for showing Toast popups](https://dev.to/nikola/how-to-create-an-android-cordova-plugin-for-showing-toast-popups--9fb)


## Usage

### SSH
~~~
cordova plugin add git@github.com:kaizentmk/tmk-cordova-plugin-usb.git
~~~

### HTTPS

~~~
cordova plugin add https://github.com/kaizentmk/tmk-cordova-plugin-usb.git
~~~

### Edit www/js/index.js and add the following code inside onDeviceReady

~~~ js
    var success = function(message) {
        alert(message);
    }

    var failure = function() {
        alert("Error calling Hello Plugin");
    }

    hello.greet("World", success, failure);
~~~