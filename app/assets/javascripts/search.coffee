do_search = (search_request, domain, is_debug) ->

    request_params =
        url : 'livesearch?h=' + domain + '&q=' + search_request + '&debug=' + is_debug
        success : () ->
            console.log "success"
        error : (  ) ->
            console.log "error"

    $.ajax request_params

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