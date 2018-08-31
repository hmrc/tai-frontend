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
                mirrorTaxCodePairColumnHeights(currentTaxCodes[i], previousTaxCodes[i])
            }
        }
    }

    function mirrorTaxCodePairColumnHeights(current, previous) {
        var currentEmploymentHeading = current.getElementsByClassName("tax-code-change__employment-heading")[0];
        var previousEmploymentHeading = previous.getElementsByClassName("tax-code-change__employment-heading")[0];

        if (isTaxCodePair(currentEmploymentHeading, previousEmploymentHeading)) {
            resetHeights(currentEmploymentHeading, previousEmploymentHeading)
            mirrorElementHeight(currentEmploymentHeading, previousEmploymentHeading);
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