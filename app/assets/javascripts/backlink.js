// prevent resubmit warning
// replace history with corrected url


var docReferrer = document.referrer;

if (
    window.history &&
    window.history.replaceState &&
    typeof window.history.replaceState === 'function'
) {

    var urlSegment =    window.location.href.substring(window.location.href.lastIndexOf('/') + 1);

    switch(urlSegment) {
        case 'decision':
            window.history.replaceState(null, null, docReferrer);
            break;

        case 'how-to-update-income':
            window.history.replaceState(null, null, docReferrer);
            break;

        default:
            window.history.replaceState(null, null, window.location.href);
    }

}


