do_search = (search_request, domain, is_debug) ->

<<<<<<< local
    url : '/search/site?h=' + domain + '&q=' + search_request + '&debug=' + is_debug

    search_script = document.createElement "script"
    search_script.setAttribute "type", "text/javascript"
    search_script.setAttribute "src", url

    head = document.getElementsByTagName("head")[0]
    head.appendChild search_script
=======
    request_params =
<<<<<<< local
        url : '/search/site?h=' + domain + '&q=' + search_request + '&debug=' + is_debug
=======
        url : 'livesearch?h=' + domain + '&q=' + search_request + '&debug=' + is_debug
        success : () ->
            console.log "success"
        error : (  ) ->
            console.log "error"
>>>>>>> other
>>>>>>> other

    $.ajax request_params

sio =
  _s_add_result : ( data ) ->
    search_results = data['search_result']
    sr_dom = $('#searchResults')

    sr_dom.html ""

    for sr in search_results
      sr_dom.append('<div class="search-results-line"><h1><a href="' + sr.url + '" target="_blank">' + sr.title + '</a></h1><img src="' + sr.image_rel_url + '"><p>url: ' + sr.url + '</p><p>' + sr.contentText + '</p></div>')



window.sio = sio

# Биндим поиск на поднятие клавиш
$('#searchRequestField').bind "keyup", () ->

    if typeof window["searchTimer"] != 'undefined'
        clearTimeout window["searchTimer"]

    do_search_cb = () ->

        search_domain = $('#searchDomainField').val()
        search_request = $('#searchRequestField').val()

        is_debug = $('#isDebugField').is(':checked')


        do_search search_request, search_domain, is_debug


    window["searchTimer"] = setTimeout do_search_cb, 300
