(function() {

  define([], function() {
    /**
      @constant {Number} id приложения для социальной сети ВКонтакте
    */

    var API_ID;
    API_ID = 4705589;
    return {
      /**
        Вызывает метод api
        @param {String} имя метода
        @param {Object} параметры вызова
        @param {Function} callback функция
      */

      call: function(name, params, callback) {
        return VK.Api.call(name, params, callback);
      },
      /**
        @callback отправляет данные об успешной иницализации модуля
      */

      onSuccess: function() {
        return console.log("all good");
      },
      /**
        @callback отправляет данные об ошибке инициализации модуля
        @param (Object) экземпляр webSocket для отправки результата на сервер
        @param (String) описание ошибки
      */

      onError: function(connection, reason) {
        return connection.send(reason);
      },
      /**
        Инициализация модуля для социальной сети ВКонтакте
      */

      init: function() {
        return console.log("inti vk module");
      }
    };
  });

}).call(this);
