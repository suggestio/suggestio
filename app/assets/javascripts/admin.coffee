#domains =
#	{% for pd in person_domains %}
#		{% if forloop.counter > 1 %},{% endif %}
#		'{{pd.domain_id}}':{}
#		{% empty %}
#	{% endfor %}

# add domain link
$('#addDomainLink').bind "click", () ->
	add_domain_action()
	return false

add_domain_action = () ->

	$('.domain-li').removeClass "pressed-domain"
	_hide_all_windows()
	$('#addDomainWindow').show()


$('#addNewDomainButton').bind "click", () ->
	$('#addDomainForm').submit()
	return false

install_js_template = ( js_url ) ->
	return "<script type=\"text/javascript\">\n\t" +
		"(function() {\n\t" +
		"var _sw = document.createElement(\"script\");\n\t" +
		"_sw.type = \"text/javascript\";\n\t" +
		"_sw.async = true" +
		'_sw.src = "https://suggest.io' + js_url + '"'+
		"var _sh = document.getElementsByTagName(\"head\")[0] "+
		"_sh.appendChild(_sw)})()\n" +
		"<\/script>"

request_installation_code = ( url ) ->

  request_params =
    url: 'http://' + url
    method : post
    success : ( data ) ->
    	js_url = data.js_url
    	$('#qiCode').show()
    	$('#jsCodeTextarea').val install_js_template js_url

	$.ajax "/crawl/site_add"

$('#addDomainForm').bind "submit", () ->

	_a = $(this).attr "action"
	_d = $('#userSiteInput').val()

	_d = _d.replace('http://','')
	if _d.charAt _d.length - 1 == '/'
	  _d =  _d.substring 0, _d.length-1

	if( !validateURL( _d ) )
	  highlight_bad_url()
		return false

  request_params =
    url : _a
    data :
        "domain" : _d
    method : post
    success : (data) ->
      if( data.status == 'no_such_domain' )
        request_installation_code( _d )
      else
        if( data.status != 'error' )
          add_domain_to_list( _d )
        else
          highlight_bad_url( _d )

  $.ajax request_params

  return false

$('#userSiteInput').bind 'keypress', () ->
	$('.input-real').removeClass('bad-url')

highlight_bad_url = () ->
  $('.input-real').addClass 'bad-url'

validateURL = (url) ->
    regURLrf = /^(?:(?:https?|ftp|telnet):\/\/(?:[а-я0-9_-]{1,32}(?::[а-я0-9_-]{1,32})?@@)?)?(?:(?:[а-я0-9-]{1,128}\.)+(?:рф)|(?! 0)(?:(?! 0[^.]|255)[ 0-9]{1,3}\.){3}(?! 0|255)[ 0-9]{1,3})(?:\/[a-zа-я0-9.,_@@%&?+=\~\/-]*)?(?:#[^ \'\"&<>]*)?$/i
    regURL = /^(?:(?:https?|ftp|telnet):\/\/(?:[a-z0-9_-]{1,32}(?::[a-z0-9_-]{1,32})?@@)?)?(?:(?:[a-z0-9-]{1,128}\.)+(?:com|net|org|mil|edu|arpa|ru|gov|biz|info|aero|inc|name|[a-z]{2})|(?! 0)(?:(?! 0[^.]|255)[ 0-9]{1,3}\.){3}(?! 0|255)[ 0-9]{1,3})(?:\/[a-zа-я0-9.,_@@%&?+=\~\/-]*)?(?:#[^ \'\"&<>]*)?$/i
	  regURLrf.test(url)||regURL.test(url)

initSettingsForm = () ->
	$('#domainSettingsForm').bind "submit", () ->
		_a = $(this).attr('action')


add_domain_to_list = ( _d ) ->
	$('#domain_list').append( domains_list_template(_d) )

	domains_list_init()
	show_domainVerificationWindow(_d)

domains_list_template = ( _d ) ->

	'<li data-domain="' + _d + '" class="domain-li pressed-domain not-verified-domain">' +
		'<a class="domain-link" href="">' + _d + '</a>' +
		'<span class="delete-domain-cross" data-href="/admin/delete" data-domain="' + _d + '" title="Удалить сайт"></span>' +
		'</li>'

show_domainVerificationWindow = ( _d ) ->
  _hide_all_windows()

	$('#domainVerificationWindow').show()
	$('#domainVerificationWindow .domain-name').html( _d )
	$('#domainVerificationWindow .verification-file').attr('href', _domain_verification_link( _d ))


	$('#domainVerificationWindow .verification-step-two .desc').hide()
	$('.verification-step-two .digit').removeClass('digit-active')

$('.verification-file').bind 'click', () ->
		listen_for_events(_global_timestamp)

_hide_all_windows = () ->
	$('.sio-admin-window').hide()

show_domainPreferencesWindow = ( _d ) ->
	_hide_all_windows()


_domain_verification_link = ( _d ) ->
	'validation_url/' + _d
