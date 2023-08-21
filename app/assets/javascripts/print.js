document.getElementById("printLink").addEventListener("click", function(e) {
    e.preventDefault();
    window.print();
});

document.getElementById("backLink").addEventListener("click", function(e) {
    e.preventDefault();
    window.history.back();
});
