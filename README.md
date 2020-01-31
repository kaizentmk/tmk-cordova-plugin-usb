# tmk-cordova-plugin-usb

min SDK version: 27
Android >= 8.1 Oreo

## folders and files

* /android-usb  - android studio project - contains java files for the plugin; based on sample project USBEnumerator. Only some classes are taken from here(see: plugin.xml)
* /www          - cordova plugin js
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
        alert("Error calling TmkUsb Plugin");
    }

    window.cordova.plugins.tmkusb.greet("World", success, failure);
~~~

## Cordova dependency in android studio

 [maven cordova framework for gradlew](https://mvnrepository.com/artifact/org.apache.cordova/framework)
 build.gradlew (Module:app)
 ~~~ groovy
 dependencies {
    implementation group: 'org.apache.cordova', name: 'framework', version: '8.0.0'
 }
 ~~~

## Issues
### building
  /home/tmk/projs/tmk-cordova-plugin-usb/android-usb/app/src/main/java/com/example/androidthings/usbenum/UsbActivity.java:55: Error: Call requires API level 23 (current min is 21): android.content.Context#getSystemService [NewApi]
          mUsbManager = getSystemService(UsbManager.class);



 ## TODO

 1. Init broadcast receiver in initialize method. You can make it so the app will start automatically by adding an IntentFilter to the MainActivity. The IntentFilter triggers when any new device is attached. To explicitly specify the type of device by providing the vendor ID and/or product ID in an XML file.

 2. Query for all connected devices. All USB slave devices have a vendor and product ID.

 3. Check for matching vendor id. An Arduino’s vendor ID is always 0x2341 or 9025.

 4. Request user permission to access the device

 5. If permission granted open the device

 6. Create serial connection and set parameters

 7. Begin thread to continuously check for incomming data

 8. Display/write data. Take note that reading from the device is asynchronous, so it will continuously run in the background, receiving data as soon as possible. All data received is in the form of raw bytes, so it needs to be re-encoded into a readable format such as UTF-8.

 ## Development

 ~~~ bash
git add . && git commit -am"Tmkcordovapluginusb" && git push --force origin master
 ~~~