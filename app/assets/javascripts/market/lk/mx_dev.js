var cbca = {};

;$(function() {

  cbca.select = new CbcaSelect();
  cbca.select.init('cat-input');
  ChooseCategory();

});

function CbcaSelect() {
  var self = this,
  animationTime = 200;


  self.init = function(inputId) {

    $('.cbca-select').each(function() {
      var $this = $(this),
      $dropDown = $this.find('.dropdown');

      if(!$this.data('init')) {
        $this.data({
          'open': false,
          'init': true,
          'inputId': inputId
        });
        $dropDown.css('height', 0);
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
      title = $this.html(),
      value = $this.attr('data-value'),
      $thisWrap = $this.closest('.cbca-select');

      self.setValue($thisWrap, title, value);
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


  self.setValue = function($select, title, value) {
    var catId = $select.attr('id');

    $select.find('.selected').html(title);
    if($select.data('inputId')) {
      $('#'+$select.data('inputId'))
      .val(value)
      .attr('data-curr-cat', catId)
      .trigger('change');
    }
    else {
      $select.find('.result')
      .val(value)
      .attr('data-curr-cat', catId)
      .trigger('change');
    }
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

  self.generateSubCat = function(options) {

    var defaults = {
      data: '',
      id: '',
      preText: '',
      style: '',
      inputId: false,
      counter: ''
    },
    options = $.extend(defaults, options),
    html = '<span class="cbca-select" id="'+options.id+'" data-counter="'+options.counter+'" style="'+options.style+'">'+options.preText;

    if(options.inputId && !$('#'+options.inputId).length) {
      html += '<input type="hidden" value="" id="'+options.inputId+'" class="result"  />';
    }

    html += '<div class="selectbox"><div class="value"><div class="selected">--------</div><div class="dropdown">';

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

  self.bindEvents();

}

function ChooseCategory() {
    var self = this;

    self.init = function() {

      $(document).on('change', '#cat-input',
      function(e) {
        var $this = $(this),
        catId = $this.val();


        jsRoutes.controllers.MarketCategory.directSubcatsOf(catId).ajax({
          success: function(data) {
            var wrapId = $this.attr('data-curr-cat'),
            $wrap = $('#'+wrapId),
            counter = 1 + parseInt($wrap.attr('data-counter'), 10),
            nextCatId =  'cat-' + counter,
            left = $wrap.position().left + $wrap.width();

            for (var i = counter; i <= 3; i++) {
              $('#cat-'+i).remove();
            }


            if(data.length) {
              var html = cbca.select.generateSubCat({
                data:    data,
                id:      nextCatId,
                preText: '<span class="pre-span">&nbsp;/&nbsp;</span>',
                style:   'top: 0; left: '+left+'px;',
                inputId:   'cat-input',
                counter: counter
              });
              $('.categories').append(html);
              cbca.select.init('cat-input');
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