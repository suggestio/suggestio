sio = 
  
  config :
    host: 'http://localhost:9000'
  
  index :
    
    js_code_tpl : ( js_url ) ->
      "<script type=\"text/javascript\">\n\t" +
        "(function() {\n\t" +
        "var _sw = document.createElement(\"script\");\n\t" +
        "_sw.type = \"text/javascript\";\n\t" +
        "_sw.async = true;" +
        '_sw.src = "' + sio.config.host + js_url + '";'+
        "var _sh = document.getElementsByTagName(\"head\")[0]; "+
        "_sh.appendChild(_sw);})();\n"+
      "</script>"
    
    init : () ->
      
      ## Забиндить действие добавления сайта на поле ввода
      $('#userSiteInput').bind "keydown", ( event ) ->
        
        ## Если нажат Enter — инициировать добавление сайта
        if event.keyCode == 13
          
          ## Берем url сайта
          ## необходимо добавить валидацию на стороне JS
          site_url = $(this).val()
          
          sio.index.add_site site_url

      ## И через нажание на кнопку
      $('.sioStartButton').bind "click", () ->
        site_url = $('#userSiteInput').val()
        sio.index.add_site site_url

        return false


    ## Фунция для добавления сайта
    ## Делает POST запрос на сервак
    add_site : ( site_url ) ->
      
      api_url = "/js/add_domain"
      
      params = 
        method : 'post'
        data :
          url : 'http://' + site_url
        success : ( data ) ->
          sio.index.render_install_code data
      
      $.ajax api_url, params
    
    ## Показать юзеру код для установки
    render_install_code : ( data ) ->
      $('#jsCodeTextarea').val sio.index.js_code_tpl( data.js_url )
      $('.sio-second-step').show()
      
window.sio = sio