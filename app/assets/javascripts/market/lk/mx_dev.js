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
      $dropDown = $this.find('.dropdown');

      $this.data({
        'open': false
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


  self.close = function($select) {
    $select.find('.dropdown').height(0);
    $select.data('open', false);
  }//self.close end


  self.open = function($select) {
    $select.find('.dropdown').height('');
    $select.data('open', true);
  }//self.open end


  self.setValue = function($select, title, value) {
    $select.find('.selected').html(title);
    $select.find('.result').val(value);
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