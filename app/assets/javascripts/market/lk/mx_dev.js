;$(function() {

  var cbcaSelect = new CbcaSelect();
  cbcaSelect.init();

});


function CbcaSelect() {
  var self = this,
  animationTime = 200;


  self.init = function() {

    $('.cbca-select').each(function() {
      var $this = $(this),
      $dropDown = $this.find('.dropdown')
      initHeight = $dropDown.height();

      $this.data({
        'open': false,
        'initHeight': initHeight
      });
      $dropDown.css('height', 0);
    });


    $(document).on('click', '.cbca-select .selectbox',
    function(e) {
      e.stopPropagation();
      var $this = $(this),
      $thisWrap = $this.closest('.cbca-select');

      if($thisWrap.data('open')) {
        self.close($thisWrap);
      }
      else {
        self.closeAll();
        self.open($thisWrap);
      }
    });


    $(document).on('click', '.cbca-select .option',
    function(e) {
      var $this = $(this),
      title = $this.html(),
      value = $this.attr('data-value'),
      $thisWrap = $this.closest('.cbca-select');

      self.setValue($thisWrap, title, value);
    });


    $(document).on('click', 'html',
    function() {
      self.closeAll();
    });

  }//self.init end


  self.close = function($wrap) {

    $wrap.find('.dropdown').animate({
      'height': 0
    }, animationTime);
    $wrap.data('open', false);

  }//self.close end


  self.open = function($wrap) {

    $wrap.find('.dropdown').animate({
      'height': $wrap.data('initHeight')
    }, animationTime);
    $wrap.data('open', true);

  }//self.open end


  self.setValue = function($wrap, title, value) {

    $wrap.find('.selected').html(title);
    $wrap.find('.result').val(value);

  }//self.setValue end


  self.closeAll = function() {

    $('.cbca-select').each(
    function() {
      var $this = $(this);

      if($this.data('open')) {
        self.close($this);
      }
    });

  }//self.closeAll end

}