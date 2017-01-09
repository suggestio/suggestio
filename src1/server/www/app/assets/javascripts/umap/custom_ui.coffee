# следим за сессией
connectionErr = false
getSessionStatus = ()->
  SESSION_END    = 'Кажется, ваша сессия истекла'
  CONNECTION_ERR = 'Нет свзяи с сервером!'

  jsRoutes.controllers.Static.keepAliveSession().ajax
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
