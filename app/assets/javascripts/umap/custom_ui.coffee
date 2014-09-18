$doc = $ document

$doc.ready ->
  $ '.js-select-id'
  .hide()

# выбор слоя
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

# выбор объекта
$doc.on 'change', '.js-select-id', ->
  $this = $ this
  $selected = $this.find 'option:selected'
  value = $selected.val()
  $storageProps = $ '#storage-feature-properties'
  $input = $storageProps.find 'textarea'

  if parseInt(value) != 0
    $input.val value

# следим за сессией
connectionErr = false
getSessionStatus = ()->
  SESSION_END    = 'Кажется, ваша сессия истекла'
  CONNECTION_ERR = 'Нет свзяи с сервером!'

  jsRoutes.controllers.Application.keepAliveSession().ajax
    type: 'post'
    success: (data, textStatus, xhr)->
      connectionErr = false
      if xhr.status == 204
        setTimeout getSessionStatus, 120000
      else
        confirm SESSION_END
    error: (error, textstatus)->
      if connectionErr
        alert CONNECTION_ERR
        return false
      if textStatus == 'timeout'
        connectionErr = true
        setTimeout getSessionStatus, 10000

getSessionStatus()