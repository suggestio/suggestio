var cbca = {};

;$(function() {

  cbca.selectCat = new CbcaSelect('cat-select');
  ChooseCategory();

  $(document).on('click', '.block .toggle',
  function(e) {
    e.preventDefault();

    var $this = $(this),
    slideContentId = $this.attr('data-content');

    $this.toggleClass('open').text( $this.text()=="Свернуть"?"Развернуть":"Свернуть");
    $('#'+slideContentId).slideToggle();
  });


 $('.color.custom').each(
  function() {

    var $this = $(this),
    updatePreview = false,
    $wrap = $this.parent(),
    $checkbox = $wrap.find('.one-checkbox');

    $this.ColorPicker({
      color: '#'+$this.find('input').val(),
      onBeforeShow: function() {
        if($checkbox.size()) {
          $checkbox.trigger('click').get(0).checked = true;
        }
      },
      onShow: function (colpkr) {
        $(colpkr).fadeIn(500);
        return false;
      },
      onHide: function (colpkr) {
        $(colpkr).fadeOut(500);
        return false;
      },
      onChange: function (hsb, hex, rgb) {
        if($checkbox.size()) {
          $checkbox.attr('data-value', hex).get(0).checked = true;
        }
        $this.find('input').val(hex);
        clearTimeout(updatePreview);
        updatePreview = setTimeout(
        function() {
          $this.find('input').trigger('change');
        }, 500);

      }
    });

  });





});

function CbcaSelect(containerId) {
  var self = this,
  animationTime = 200,
  $container = $('#'+containerId),
  $input = $('#catId');

  self.currIndex = 0;


  self.initDropDown = function() {

    $('.cbca-select').each(
    function() {
      var $this = $(this),
      $dropDown = $this.find('.dropdown'),
      $wrap = $this.parent(),
      index = $wrap.find('.cbca-select').index(this),
      left = 0;

      if(!$this.data('init')) {
        $this.data({
          'open': false,
          'init': true
        });
        $dropDown.css('height', 0);

        $wrap.find('.cbca-select:lt('+index+')').each(
        function() {
          left += $(this).position().left + $(this).width();
        });
        $this.css('left', left);
        if(index > 0) {
          $this.prepend('<span class="pre-span">&nbsp;/&nbsp;</span>');
        }

        var $selected = $this.find('.option[data-selected]');
        if($selected.length) {
           $this.find('.selected').html($selected.html());
           $input.val($selected.attr('data-value'));
        }
      }
    });

  }//self.init end


  self.bindEvents = function() {
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
      value = $this.attr('data-value');

      self.setValue(value);
    });


    $(document).on('click', 'html',
    function() {
      self.closeAll();
    });

  }//self.bindEvents end


  self.close = function($select) {
    $select.find('.dropdown').height(0);
    $select.data('open', false);
  }//self.close end


  self.open = function($select) {
    $select.find('.dropdown').height('');
    $select.data('open', true);
  }//self.open end


  self.setValue = function(value) {
    var $option = $container.find('.option[data-value = "'+value+'"]'),
    $select = $option.closest('.cbca-select');

    self.currIndex = $container.find('.cbca-select').index($select);
    self.clearSelects();

    $select.find('.selected').html($option.html());
    $input.val(value).trigger('change');

  }//self.setValue end


  self.clearSelects = function() {
    $container.find('.cbca-select:gt('+self.currIndex+')').remove();
  }//self.clearSelects end


  self.closeAll = function() {
    $('.cbca-select').each(
    function() {
      var $this = $(this);

      if($this.data('open')) {
        self.close($this);
      }
    });
  }//self.closeAll end

  self.generateSubCat = function(options) {

    var defaults = {
      data: '',
      style: ''
    },
    options = $.extend(defaults, options),
    html = '<span class="cbca-select" style="'+options.style+'">'+
    '<div class="selectbox"><div class="value"><div class="selected">--------</div><div class="dropdown">';

    for(key in options.data) {
      var obj = options.data[key],
      optionClass;

      if(key%2 == 0) {
        optionClass = 'odd';
      }
      else {
        optionClass = 'even';
      }
      html += '<div class="option '+optionClass+'" data-value="'+obj.id+'">'+obj.name+'</div>';
    }

    html += '</div></div><div class="triger"></div></div></span>';

    return html;
  }

  self.initDropDown();
  self.bindEvents();

}

function ChooseCategory() {
    var self = this;

    self.init = function() {

      $(document).on('change', '#catId',
      function(e) {
        var $this = $(this),
        catId = $this.val();

        jsRoutes.controllers.MarketCategory.directSubcatsOf(catId).ajax({
          success: function(data) {
            $wrap = $('#cat-select').find('.cbca-select').eq(cbca.selectCat.currIndex),
            left = $wrap.position().left + $wrap.width();

            if(data.length) {
              var html = cbca.selectCat.generateSubCat({
                data:    data,
                style:   'top: 0; left: '+left+'px;'
              });
              $('#cat-select').append(html);
              cbca.selectCat.initDropDown();
            }
          },
          error: function(error) {
            console.log(error);
          }
        });

      });


    }//self.init end



    self.init();
}