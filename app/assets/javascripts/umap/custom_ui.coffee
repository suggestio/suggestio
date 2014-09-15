$doc = $ document

$doc.ready ->
  $ '.js-select-id'
  .hide()

$doc.on 'change', '.js-select-label', ->
  $this = $ this
  $selected = $this.find 'option:selected'
  value = $selected.val()

  $this.hide()
  $ ".js-select-id[data-label = '#{value}']"
  .show()

$doc.on 'change', '.js-select-id', ->
  $this = $ this
  $selected = $this.find 'option:selected'
  value = $selected.val()
  $storageProps = $ '#storage-feature-properties'
  $input = $storageProps.find 'input[name = "name"]'
  $input.val value