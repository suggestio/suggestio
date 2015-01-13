(function() {

  define([], function() {
    return {
      /**
        @callback отправляет на сервер сообщение об успешной инициализации
        @param (Object) экземпляр webSocket для отправки результата на сервер
        @param (Json) новое состояние системы
      */

      onSuccess: function(connection, context) {
        var message;
        message = JSON.stringify(context);
        return connection.send(message);
      },
      /**
        @callback отправляет на сервер сообщение об ошибке инициализации
        @param (Object) экземпляр webSocket для отправки результата на сервер
        @param (String) описание ошибки
      */

      onError: function(connection, reason) {
        return connection.send(reason);
      },
      /**
        Получить модуль конкретной социальной сети
        @param (String) название модуля социальной сети
      */

      getServiceByName: function(name) {
        return require([name], function(service) {
          return console.log(service.init());
        });
      },
      /**
        Инициализация social api
        @param (Json) контекст, в котором вызывается api
      */

      init: function() {
        var context;
        context = {
          connectionUrl: "ws://example.org:12345/myapp",
          initializationStatus: false
        };
        return console.log("main module init");
      }
    };
  });

}).call(this);
