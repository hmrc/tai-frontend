// prevent resubmit warning
// replace history with corrected url


var docReferrer = document.referrer;

if (
    window.history &&
    window.history.replaceState &&
    typeof window.history.replaceState === 'function'
) {

    const filter = (value, x) => {
        let segments = value.split('/');
        return segments[ segments.length -x];
    }

    var url = window.location.href;
    var  urlSegment = filter(url,2) +'/' + filter(url,1)

console.warn('urlSegment', urlSegment)
    switch(urlSegment) {
        case 'update-remove-employment/decision':
            window.history.replaceState(null, null, docReferrer);
            break;

        case 'update-income/how-to-update-income':
            window.history.replaceState(null, null, docReferrer);
            break;

        case 'update-income-details/decision':
            console.warn('replace with:  ', docReferrer)
           window.history.replaceState(null, null, docReferrer);
            break;

        default:
            window.history.replaceState(null, null, window.location.href);
    }

}


