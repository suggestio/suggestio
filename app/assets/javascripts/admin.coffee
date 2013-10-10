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
		"_sw.async = true;" +
		'_sw.src = "https://suggest.io' + js_url + '";'+
		"var _sh = document.getElementsByTagName(\"head\")[0]; "+
		"_sh.appendChild(_sw);})();\n" +
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
	_d = $('#userSiteInput').val();

	_d = _d.replace('http://','');
	if( _d.charAt(_d.length - 1 ) == '/' ) _d =  _d.substring(0, _d.length-1 );

	if( !validateURL( _d ) )
	  highlight_bad_url()
		return false

  request_params

	$.post( _a, { "domain" : _d }, function(data) {
		if( data.status == 'no_such_domain' )
		{
			request_installation_code( _d );
		}
		else
		{
			if( data.status != 'error' )
			{
			add_domain_to_list( _d );
			}
			else
			{
			highlight_bad_url( _d );
			}
		}
		});

		return false;
		});

	$('#userSiteInput').bind('keypress', function()
		{
		$('.input-real').removeClass('bad-url');
		});

	var highlight_bad_url = function()
	{
	$('.input-real').addClass('bad-url');
	}

	var validateURL = function(url) {
	var regURLrf = /^(?:(?:https?|ftp|telnet):\/\/(?:[а-я0-9_-]{1,32}(?::[а-я0-9_-]{1,32})?@@)?)?(?:(?:[а-я0-9-]{1,128}\.)+(?:рф)|(?! 0)(?:(?! 0[^.]|255)[ 0-9]{1,3}\.){3}(?! 0|255)[ 0-9]{1,3})(?:\/[a-zа-я0-9.,_@@%&?+=\~\/-]*)?(?:#[^ \'\"&<>]*)?$/i;
	var regURL = /^(?:(?:https?|ftp|telnet):\/\/(?:[a-z0-9_-]{1,32}(?::[a-z0-9_-]{1,32})?@@)?)?(?:(?:[a-z0-9-]{1,128}\.)+(?:com|net|org|mil|edu|arpa|ru|gov|biz|info|aero|inc|name|[a-z]{2})|(?! 0)(?:(?! 0[^.]|255)[ 0-9]{1,3}\.){3}(?! 0|255)[ 0-9]{1,3})(?:\/[a-zа-я0-9.,_@@%&?+=\~\/-]*)?(?:#[^ \'\"&<>]*)?$/i;
	return regURLrf.test(url)||regURL.test(url);
	}

	var initSettingsForm = function()
	{
	// domain settings form
	$('#domainSettingsForm').bind('submit', function()
		{
		var _a = $(this).attr('action');

		$.post( _a, $('#domainSettingsForm').serialize()).success(

			function()
			{

			render_caption('domainSettingsStatus','@Messages("saved_successfully")');
			setTimeout(function()
			{
			$('#domainSettingsStatus').fadeOut(function()
				{
				$(this).html('').show();
				});
			}, 1000);
			}
			);
		return false;
		});
	}

	/**** inner functions ****/

	// add domain
	var add_domain_to_list = function( _d )
	{
	$('#domain_list').append( domains_list_template(_d) );

	domains_list_init();
	show_domainVerificationWindow(_d);
	}

	// возвращает html для списка доменов
	var domains_list_template = function( _d )
	{

	return '<li data-domain="' + _d + '" class="domain-li pressed-domain not-verified-domain">' +
		'<a class="domain-link" href="">' + _d + '</a>' +
		'<span class="delete-domain-cross" data-href="/admin/delete" data-domain="' + _d + '" title="Удалить сайт"></span>' +
		'</li>';
	}

	// показать окно с инфой о верификации домена
	var show_domainVerificationWindow = function( _d )
	{

	_hide_all_windows();

	$('#domainVerificationWindow').show();
	$('#domainVerificationWindow .domain-name').html( _d );
	$('#domainVerificationWindow .verification-file').attr('href', _domain_verification_link( _d ));

	$('#domainVerificationWindow .force-recheck').unbind('click').bind('click', function()
		{

		$.ajax('{% url action="revalidate" %}/' + _d, {
			success:
				function(data, code, xhr) {
				} });

		return false;
		});

	$('#domainVerificationWindow .verification-step-two .desc').hide();
	$('.verification-step-two .digit').removeClass('digit-active');

	};

	$('.verification-file').bind('click', function()
		{
		setTimeout(function()
		{
		$('#domainVerificationWindow .verification-step-two .desc').fadeIn(150);
		$('.verification-step-two .digit').addClass('digit-active');

		listen_for_events(_global_timestamp);

		},500);
		});

	var _hide_all_windows = function()
	{
	$('.sio-admin-window').hide();
	}

	// показать окно с настройками для домена
	var show_domainPreferencesWindow = function( _d )
	{
	_hide_all_windows();
	$.ajax('/admin/domain_settings/' + _d, {
		success: function( _tpl ) {
			$('#domainPreferencesWindow').html( _tpl );
		}
	});

	var d = $('#domainPreferencesWindow');

	if( d.is(':visible') )
	{
		dpw_slides( d, 'out', function()
			{
			dpw_slides( d, 'in' );
			});

	} else
	{
		d.show();
		dpw_slides( d, 'in' );
	}

	};

	var dpw_slides = function( o, _d, c )
	{
	var off_params = {'opacity':'0','margin-left':'-30px'};
	var on_params =  {'opacity':'1','margin-left':'0px'};

	if( _d == 'in' )  o.css(off_params).stop().animate(on_params,350);
	if( _d == 'out' ) o.stop().animate(off_params,350, function() { c(); });
	}

	// повесить нужные события на доменные имена в списке
	var domains_list_init = function()
	{

	$('.domain-link').unbind('click').bind('click', function()
		{
		var _d = $(this).html();

		if( $(this).parent().hasClass('not-verified-domain') )
		{
		show_domainVerificationWindow( _d );
		}
		else
		{
		show_domainPreferencesWindow( _d );
		}

		$('.domain-li').removeClass('pressed-domain');
		$(this).parent().addClass('pressed-domain');

		return false;
		});

	$('.delete-domain-cross').bind('click', function()
		{
		var _delete_href =  $(this).attr('data-href');
		var _d =  $(this).attr('data-domain');

		if( !confirm( "Are you sure, that you want to delete domain?" ) ) return false;

		$.post( _delete_href, { "domain" : _d }, function(data) {
			$('.domain-li').each(function()
			{
				if( $(this).attr('data-domain') == _d )
				{
				$(this).remove();
				_hide_all_windows();
				$('#introWindow').show();
				}
			});
			});

		});
	}

	// возвращает ссылку для верификации домена
	var _domain_verification_link = function( _d )
	{
	return '{% url action="get_validation_file" %}/' + _d
	}

	//
	var _set_domain_status = function( _d, status )
	{
	var _ds = $('.domain-li');
	_ds.each(function()
		{
			var _cd = $(this).attr('data-domain');
			if( _cd == _d )
			{
			if( status === true )
			{
				$(this).removeClass('not-verified-domain').addClass('verified-domain');

				if( loaded === true ) show_domainPreferencesWindow( _d );
			}else
			{
				$(this).removeClass('verified-domain').addClass('not-verified-domain');
			}
			}
		});
	};

	domains_list_init();

	// подписаться на события
	function listen_for_events( timestamp ) {
	$.ajax('{% url action="pull" %}/' + timestamp,
		{
		success:
			function(data, code, xhr) {
				for (var i=0; i < data.events.length; i++) {
					var _event = data.events[i];

					if( _event.type == 'domain_verification' )
					{
						var _d = 	_event.domain;
						var _iv = _event.is_verified;

						_set_domain_status( _d, _iv );

					}

				}
				_global_timestamp = data.timestamp;
				listen_for_events(data.timestamp);
			}
		});
	}

	var reindex_domain = function( domain )
	{
		$.post( '{% url action="reindex" %}/', { "domain" : domain }, function( data ) {
			$('#lastIndexedDate').html('');

			if( data.status == "ok" )
				render_caption('lastIndexedDate','@Messages("reindex_launched")');
			else
				render_caption('lastIndexedDate','@Messages("reindex_try_later")');
		});
	}

	var showImagesExplanation = function()
	{
		$('#imageExplanationLayout').fadeIn();
	}

	$('body').bind('click', function()
		{
		$('#imageExplanationLayout').fadeOut();
		});

	$('#imageExplanationLayout .el-text').bind('click', function( event )
		{
		event.stopPropagation();
		});


	var render_caption = function( elt_id, new_value )
	{

		if( typeof( window['render_caption' + elt_id] ) != 'undefined' ) return false;

		window['render_caption' + elt_id] = 1;

		var delay = 50;
		for( var i = 0; i < new_value.length+1; i++ )
		{
		setTimeout("$('#" + elt_id + "').html('" + new_value.substring(0,i) + "');", i*delay);
		}

		setTimeout(function()
			{
			delete( window['render_caption' + elt_id] );
			}, (new_value.length+1) * delay);

	}

	var loaded = false;

	//костылище
	setTimeout(function()
		{
		loaded = true
		},2000);

	$(document).ready(function() {
		listen_for_events(@timestampMs);
		@* _set_domain_status( "waveru.ru", true ); *@				@* ЧТО ЭТО??? *@
	});