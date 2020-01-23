/*global cordova, module*/

/**
 * cordova.exec(function(winParam) {},
             function(error) {},
             "service",
             "action",
             ["firstArgument", "secondArgument", 42, false]);

             Here is how each parameter works:

function(winParam) {}: A success callback function. Assuming your exec call completes successfully, this function executes along with any parameters you pass to it.

function(error) {}: An error callback function. If the operation does not complete successfully, this function executes with an optional error parameter.

"service": The service name to call on the native side. This corresponds to a native class, for which more information is available in the native guides listed below.

"action": The action name to call on the native side. This generally corresponds to the native class method.

[ arguments ]: An array of arguments to pass into the native environment.
*/

/**
 * exec(<successFunction>, <failFunction>, <service>, <action>, [<args>]);
 */
const pluginName = "TmkUsb"

module.exports = {
    greet: function (name, successCallback, errorCallback) {
        cordova.exec(successCallback,
            errorCallback,
            pluginName,
            "greet",
            [name])
    },
    notSupportedAction: function (successCallback, errorCallback) {
        cordova.exec(successCallback,
            errorCallback,
            pluginName,
            "notSupportedAction",
            [])
    },
    info: function (successCallback, errorCallback) {
        cordova.exec(successCallback,
            errorCallback,
            pluginName,
            "info",
            [])
    },
    listUsbDevices: function (successCallback, errorCallback) {
        cordova.exec(successCallback,
            errorCallback,
            pluginName,
            "listUsbDevices",
            [])
    },
    reset: function (successCallback, errorCallback) {
        cordova.exec(successCallback,
            errorCallback,
            pluginName,
            "reset",
            [])
    },
    connect: function (successCallback, errorCallback) {
        cordova.exec(successCallback,
            errorCallback,
            pluginName,
            "connect",
            [])
    }
};
