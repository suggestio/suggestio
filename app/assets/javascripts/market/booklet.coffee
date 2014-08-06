$ document
.scroll () ->

  $document = $ document
  $header = $ '#header'
  scrollTop = $document.scrollTop()

  if scrollTop > 500
    $header.addClass '__js-dark'
  else
    $header.removeClass '__js-dark'