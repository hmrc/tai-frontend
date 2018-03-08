$(document).ready(function() {
	// Details/summary polyfill from frontend toolkit
	GOVUK.details.init()
});

// re-enable any disabled buttons when navigating back
// Safari Mac/iOS
window.onpageshow = function(event) {
    $('[type="submit"][disabled]').removeAttr('disabled');
}

/* Back link configuration */
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}
var backLinkElem = document.getElementById("backLink");
if (backLinkElem !=  null){
    if (window.history && window.history.back && typeof window.history.back === 'function') {
        var backScript = (document.referrer.indexOf(window.location.host) !== -1) ? "javascript:window.history.back(); return false;" : "javascript:void(0);"
        backLinkElem.setAttribute("onclick",backScript);
        backLinkElem.setAttribute("href","javascript:void(0);");
    }
}

/* Temp - pending AF update to > 3.0.2 */
window.GOVUK.performance.sendGoogleAnalyticsEvent = function (category, event, label) {
  if (window.ga && typeof(window.ga) === 'function') {
    ga('send', 'event', category, event, label);
  } else {
    _gaq.push(['_trackEvent', category, event, label, undefined, true]);
  }
};
window.GOVUK.performance.stageprompt.setupForGoogleAnalytics();