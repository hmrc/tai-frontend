
var card = (function () {

  // Capture when a user clicks
  $('.card-body').on('click', function () {
    var url = $(this).find('a').attr('href')
    if (url !== undefined) {
      window.location.href = url
    }
  })

 // set max height for any collection of elements
  function setMaxheight (ele, maxHeight) {
    $(ele).height(maxHeight)
  }

  function checkSize() {
    var maxHeight = getMaxHeight('.card-body')
    setMaxHeight('.card-body', maxHeight)
  }

  // get max height for any collection of elements
  function getMaxHeight (ele) {
    var height = []
    $(ele).each(function () {
      height.push($(this).height())
    })
    var maxHeight = height.sort(function (a, b) { return b - a })[0]
    return maxHeight
  }

  // set max height for any collection of elements
  function setMaxHeight (ele, maxHeight) {
    $(ele).height(maxHeight)
  }

  // Check each card. If the card does not contain a .card-action
  // make .card-body full height
  function fullHeight() {
    var cardEle = $('.card').not(':has(.card-action)')
    cardEle.each(function () {
      var $cardBody = $(this).children('.card-body')
      var maxHeight = getMaxHeight('.card')
      var paddingTop = $cardBody.css('padding-top').replace('px', '')
      var paddingBottom = $cardBody.css('padding-bottom').replace('px', '')
      var totalHeight = maxHeight - paddingTop - paddingBottom
      $cardBody.css('border-bottom', '0')
      setMaxheight($cardBody, totalHeight)
    })
  }

  isNotMobile(checkSize)
  $(window).resize(isNotMobile(checkSize))

  // This should be enabled if a card with a card-action
  // and a card without a card-action can be on the same row
  //isNotMobile(fullHeight)
  //$(window).resize(isNotMobile(fullHeight))

  // Only run function if the screen size is not mobile.
  function isNotMobile (func) {
    if (navigator.appVersion.indexOf('MSIE 10') === -1) {
      if ($('.card').css('flex-basis') !== '100%') {
        return func()
      }
    }
  }
})()

var doc = document.documentElement
doc.setAttribute('data-useragent', navigator.userAgent)