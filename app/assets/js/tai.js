$(document).ready(function() {
	var $breadCrumb = $("#global-breadcrumb nav ol li:first-child a");
	$breadCrumb.click(function(){
		event.preventDefault();
		$href = $(this).attr('href')
		$path = $href.substring($href.lastIndexOf("/")+1);
		ga('send', {
			hitType: 'event',
			eventCategory: 'check-income-tax',
			eventAction: 'Breadcrumb Link - Account home',
			eventLabel: "'"+$path+"'",
			hitCallback: function() {
				window.location.href = $href;
			}
		});
	});

	// Details/summary polyfill from frontend toolkit
	GOVUK.details.init()
});

// re-enable any disabled buttons when navigating back
// Safari Mac/iOS
window.onpageshow = function(event) {
    $('[type="submit"][disabled]').removeAttr('disabled');
}