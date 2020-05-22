// dynamically re-position nuance divs before footer for accessibility

$(window).on("load", function() {

    var waitForEl = function(selector, callback, count) {
        if (jQuery(selector).length) {
        callback();
        } else {
        setTimeout(function() {
          if(!count) {
            count=0;
          }
          count++;
          if(count<4) {
            waitForEl(selector,callback,count);
          } else {return;}
        }, 1000);
      }
    }

    waitForEl("#inqChatStage", function() {
        $("#Nuance-chat-anchored").appendTo("#wrapper #content #skipto article");
        $("#inqChatStage").appendTo("#wrapper #content #skipto article");
    });

});
