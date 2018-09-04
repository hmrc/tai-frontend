var taxCodeChange = (function () {

    function mirrorTitleHeight() {
        var sectionTitles = document.getElementsByClassName("section-title");
        resetHeights(sectionTitles[0], sectionTitles[1])
        mirrorElementHeight(sectionTitles[0], sectionTitles[1])
    }

    function mirrorHeights() {
        mirrorTitleHeight();

        var currentTaxCodesColumn = document.getElementsByClassName("tax-code-change__current")[0];
        var previousTaxCodesColumn = document.getElementsByClassName("tax-code-change__previous")[0];

        var currentTaxCodes = currentTaxCodesColumn.getElementsByClassName("tax-code-change__pod");
        var previousTaxCodes = previousTaxCodesColumn.getElementsByClassName("tax-code-change__pod");

        for (var i = 0; i < currentTaxCodes.length; i++) {
            if (currentTaxCodes[i] && previousTaxCodes[i]) {
                mirrorEmploymentHeadingHeight(currentTaxCodes[i], previousTaxCodes[i]);
                mirrorPayrollHeight(currentTaxCodes[i], previousTaxCodes[i]);
                mirrorDateHeight(currentTaxCodes[i], previousTaxCodes[i]);
            }
        }
    }

    function mirrorEmploymentHeadingHeight(current, previous) {
        mirrorPodElementHeight(current, previous, "tax-code-change__employment-heading");
    }

    function mirrorPayrollHeight(current, previous) {
        mirrorPodElementHeight(current, previous, "tax-code-change__payroll");
    }

    function mirrorDateHeight(current, previous) {
        mirrorPodElementHeight(current, previous, "tax-code-change__date");
    }

    function mirrorPodElementHeight(current, previous, className) {
        var currentThing = current.getElementsByClassName(className)[0];
        var previousThing = previous.getElementsByClassName(className)[0];

        if (isTaxCodePair(currentThing, previousThing)) {
            resetHeights(currentThing, previousThing)
            mirrorElementHeight(currentThing, previousThing);
        }
    }

    function setHeight(element, height) {
        $(element).height(height);
    }

    function getHeight(element) {
      return $(element).height()
    }

    function isTaxCodePair(current, previous) {
        return previous && current;
    }

    function mirrorElementHeight(current, previous) {
        if (getHeight(current) > getHeight(previous)) {
            setHeight(previous, getHeight(current))
        } else {
            setHeight(current, getHeight(previous))
        }
    }

    function resetHeights(current, previous) {
        setHeight(current, "");
        setHeight(previous, "");
    }

    if (document.getElementsByClassName("tax-code-change").length > 0) {
        mirrorHeights();
        window.addEventListener('resize', mirrorHeights);
    }
})()