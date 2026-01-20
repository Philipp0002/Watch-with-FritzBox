import { keyEventInit } from "./keyevent-init.js";
import { hbbtvFn } from "./hbbtv.js";
import { VideoHandler } from "./hbb-video-handler.js";


window.HBBTV_POLYFILL_DEBUG = true;

function init() {

    window.HBBTV_POLYFILL_DEBUG && console.log("hbbtv-polyfill: load");

    window.signalopenhbbtvbrowser = function(command) {
        document.title = command;
    }

    // intercept XMLHttpRequest
    /*let cefOldXHROpen = window.XMLHttpRequest.prototype.open;
    window.XMLHttpRequest.prototype.open = function(method, url, async, user, password) {
        // do something with the method, url and etc.
        window.HBBTV_POLYFILL_DEBUG && console.log("XMLHttpRequest.method: " + method);
        window.HBBTV_POLYFILL_DEBUG && console.log("XMLHttpRequest.async: "  + async);
        window.HBBTV_POLYFILL_DEBUG && console.log("XMLHttpRequest.url: "    + url);

        url = window.cefXmlHttpRequestQuirk(url);

        window.HBBTV_POLYFILL_DEBUG && console.log("XMLHttpRequest.newurl: " + url);
        this.addEventListener('load', function() {
            // do something with the response text
            window.HBBTV_POLYFILL_DEBUG && console.log('XMLHttpRequest: url ' + url + ', load: ' + this.responseText);
        });

        return cefOldXHROpen.call(this, method, url, async, user, password);
    }*/

    // global helper namespace to simplify testing
    window.HBBTV_POLYFILL_NS = window.HBBTV_POLYFILL_NS || {
    };
    window.HBBTV_POLYFILL_NS = {
        ...window.HBBTV_POLYFILL_NS, ...{
            keysetSetValueCount: 0,
            streamEventListeners: [],
        }
    };
    window.HBBTV_POLYFILL_NS.currentChannel = window.HBBTV_POLYFILL_NS.currentChannel || {
                   'TYPE_TV': 12,
                   'channelType': 12,
                   'sid': 1,
                   'onid': 1,
                   'tsid': 1,
                   'name': 'test',
                   'ccid': 'ccid:dvbt.0',
                   'dsd': ''
               };
    window.HBBTV_POLYFILL_NS.preferredLanguage = window.HBBTV_POLYFILL_NS.preferredLanguage || 'DEU';

    // set body position
    document.body.style.position = "absolute";
    document.body.style.width = "1280px";
    document.body.style.height = "720px";

    keyEventInit();
    hbbtvFn();

    //new VideoHandler().initInterval();
    new VideoHandler().initialize();

    window.HBBTV_POLYFILL_DEBUG && console.log("hbbtv-polyfill: loaded");
}

if (!document.body) {
    console.log("hbbtv-polyfill: wait for DOMContentLoaded");
    document.addEventListener("DOMContentLoaded", init);
} else {
    console.log("hbbtv-polyfill: start init immediately");
    init();
}
