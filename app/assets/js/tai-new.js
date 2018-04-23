$(document).ready(function() {
	// Details/summary polyfill from frontend toolkit
	GOVUK.details.init()

	// Use GOV.UK shim-links-with-button-role.js to trigger a link styled to look like a button,
	// with role="button" when the space key is pressed.
	GOVUK.shimLinksWithButtonRole.init()

	var showHideContent = new GOVUK.ShowHideContent()
	showHideContent.init()

	// Move focus to error summary
	$('.error-summary').focus();

    // Determine play language setting
    GOVUK.playLanguage = (function() {
        var playCookieName = encodeURIComponent("PLAY_LANG") + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) === ' ')
                c = c.substring(1, c.length);
            if (c.indexOf(playCookieName) === 0)
                return decodeURIComponent(c.substring(playCookieName.length, c.length));
        }
        return "en";
    }());

	// Character/Word Count
    var charCount = new GOVUK.CharCount()
    charCount.init({
      selector: 'js-char-count',
      highlight: true,
      language: GOVUK.playLanguage
    })

});

// re-enable any disabled buttons when navigating back
// Safari Mac/iOS
window.onpageshow = function(event) {
    $('[type="submit"][disabled]').removeAttr('disabled');
}