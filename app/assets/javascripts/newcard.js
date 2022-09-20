var card = (function () {
  // Capture when a user clicks
  const activeCards = document.querySelectorAll('.card-body' && '.active');
  if(activeCards.length){
    for (let i = 0; i < activeCards.length; i++) {
      activeCards[i].addEventListener('click', function (e) {
        let url = this.querySelector('.card-link').getAttribute('href');
        if (url !== undefined) {
          window.location.href = url;
        }
      });
    }
  }

})();

