$doc = $ document

$doc.ready ->
  $ '.js-select-id'
  .hide()

$doc.on 'click', '.js-select-label button', ->
  $this = $ this

  if $this.hasClass 'js-act'
    return false

  $ '.js-select-label .js-act'
  .removeClass 'js-act'
  $this.addClass 'js-act'
  value = $this.attr 'data-label'

  $ ".js-select-id"
  .hide()
  .filter "[data-label = '#{value}']"
  .show()

$doc.on 'change', '.js-select-id', ->
  $this = $ this
  $selected = $this.find 'option:selected'
  value = $selected.val()
  $storageProps = $ '#storage-feature-properties'
  $input = $storageProps.find 'textarea'
  $input.text value