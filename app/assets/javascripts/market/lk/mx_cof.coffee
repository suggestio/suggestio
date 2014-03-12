$(document).on 'click', '.select-iphone .iphone-block', ->
  $this = $(this)
  if(!$this.hasClass 'act')
    $('.iphone-block.act').removeClass 'act'
    $this.addClass 'act'

$(document).on 'click', '.select-ipad .ipad-block', ->
    $this = $(this)
    dataGroup = $this.attr 'data-group'

    if(!$this.hasClass 'act')
      $('.ipad-block.act[data-group = "'+dataGroup+'"]').removeClass 'act'
      $this.addClass 'act'

$(document).on 'click', '.block .tab', ->
  $this = $(this)
  $wrap = $this.closest '.block'
  index = $wrap.find('.tabs .tab').index(this)

  if(!$this.hasClass 'act')
    $wrap.find('.tabs .act').removeClass 'act'
    $this.addClass 'act'
    $wrap.find('.tab-content').hide()
    $wrap.find('.tab-content').eq(index).show()