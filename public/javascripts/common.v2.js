config = 
{
	install_js_template : function( js_url )
	{
		return "<script type=\"text/javascript\">\n\t" +
						"(function() {\n\t" +
						"var _sw = document.createElement(\"script\");\n\t" +
						"_sw.type = \"text/javascript\";\n\t" +
						"_sw.async = true;" +
						'_sw.src = "https://suggest.io' + js_url + '";'+
						"var _sh = document.getElementsByTagName(\"head\")[0]; "+
						"_sh.appendChild(_sw);})();\n"+
						"</script>"
	}
}

crawled_queue = [

];

function highlight_bad_url( if_release )
{
	if( typeof( if_release ) != 'undefined' )
	{
		$('.input-real').removeClass('bad-url');
	}
	else
	{
		$('.input-real').addClass('bad-url');
		$('#userSiteInput').removeAttr('readonly').focus();
	}
}

function positionCrawlerMonitor()
{
	
	var window_width = $(window).width();
	
	var offset = crawler_active == false ? 300 : 620; 
	
	if( window_width > 900 )
	{
		tw = window_width - offset;
		tw = tw < 443 ? 443 : tw;
		$('.first-step .input').css({'width': tw + 'px'});
	}
}

(function init()
{
	
	$(window).bind('resize',function()
	{
		positionCrawlerMonitor();
	});
	$(window).bind('load',function()
	{
		
		$('.crawl-monitor').css({'opacity':'1'});
		
		if( typeof( locale ) != 'undefined' ) $('.lang-menu .' + locale).addClass('disabled').attr('href','').bind('click', function(){ return false; });
	});
	
	$(document).ready(function()
	{
		$('.start-button a').bind('click', function()
		{
			if( !$(this).hasClass('enabled') ) return false;
			submitSite();
			return false;
		});
		
		$('#userSiteInput').bind('keyup', function( event )
		{
			highlight_bad_url( 1 );
			
			if( $(this).val() !='' ) $('.start-button a').addClass('enabled');
			
			if( event.keyCode == 13 )
			{
				submitSite();
			}
		});
		
		function submitSite()
		{
				
				var _tw = $('#userSiteInput');
				_tw.val( _tw.val().replace('http://','') );
				if( _tw.val().charAt(_tw.val().length-1) == '/' ) _tw.val( _tw.val().substring(0, _tw.val().length-1 ));
				
				
				$('.start-button a').addClass('disabled');
				$('.start-button a').removeClass('enabled');
				var site_url = $('#userSiteInput').val();
				
				if( !validateURL( site_url ) )
				{
					highlight_bad_url();
				}
				else
				{
					
					$('#userSiteInput').attr('readonly','readonly')
					
					$.post("/crawl/site_add", { url: 'http://' + site_url }).success(function( data )
					{
						var js_url = data.js_url;
						showCrawler( js_url );
					});
				}
		}
		
		$('.feedback-form input,.feedback-form textarea').bind('keydown paste', function()
		{
			if( event.keyCode != 13 )
			{
			var id = $(this).attr('id');
			setTimeout(function()
			{
				toggleFieldLabel(id)
			},1);
			}
		});
		
/*
		$('.menu-link').bind('click', function()
		{
			var popupId = $(this).attr('data-popup');
			$('.popup-content-block').hide();
			$('body').addClass('about-layer');
			$('#' + popupId + 'Popup').show();
			return false;
		});
*/
		
		$('#popupCloseButton').bind('click', function()
		{
			$('.popup-content-block').hide();
			$('#feedbackPopup, #aboutPopup').hide();
			$('body').removeClass('about-layer');
			$('.popup-content-block input, .popup-content-block textarea').val('').removeClass('bad-value');
			$('.popup-content-block label').val('').removeClass('bad-label');
			return false;
		});
		
		$('#feedbackForm a').bind('click', function()
		{
			$('#feedbackForm').submit();
			return false;
		});
		
		$('#feedbackForm').bind('submit', function()
		{
			
			if( typeof( feedbackSent ) != 'undefined' ) return false;
			
			var emailField = $('#feedbackEmail');
			var emailLabel = $('#labelfeedbackEmail')
			
			var messageField = $('#feedbackMessage');
			var messageLabel = $('#labelfeedbackMessage')
			
			if( emailField.val() == '' || !validateEmail( emailField.val() ) ) emailField.addClass('bad-value'); emailLabel.addClass('bad-label');
			if( messageField.val() == '' ) messageField.addClass('bad-value');  messageLabel.addClass('bad-label');
			
			if( emailField.val() == '' || !validateEmail( emailField.val() ) || messageField.val() == '' ) return false;
			
			$.post("/feedback/submit", $('#feedbackForm').serialize(), function( data )
			{
				feedbackSent = true;
				$('#feedbackForm a').addClass('sent');
				$('#feedbackPopup').addClass('feedback-sent');
			});
			return false;
		});
		
		function toggleFieldLabel(id)
		{
			var label = $('#label' + id);
			$('#' + id).val() != '' ? label.hide() : label.show() ;
			$('#' + id).removeClass('bad-value');
			label.removeClass('bad-label');
		}
		
	});
	
})();


/* Internal functions */
replaceURLWithHTMLLinks = function(text) {
	return "<a style=\"color: #0380A6\" href='" + text + "'>" + text + "</a>"; 
}

validateEmail = function(email) { 
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
} 

function validateURL(url) {
	var regURLrf = /^(?:(?:https?|ftp|telnet):\/\/(?:[а-я0-9_-]{1,32}(?::[а-я0-9_-]{1,32})?@)?)?(?:(?:[а-я0-9-]{1,128}\.)+(?:рф)|(?! 0)(?:(?! 0[^.]|255)[ 0-9]{1,3}\.){3}(?! 0|255)[ 0-9]{1,3})(?:\/[a-zа-я0-9.,_@%&?+=\~\/-]*)?(?:#[^ \'\"&<>]*)?$/i;
	var regURL = /^(?:(?:https?|ftp|telnet):\/\/(?:[a-z0-9_-]{1,32}(?::[a-z0-9_-]{1,32})?@)?)?(?:(?:[a-z0-9-]{1,128}\.)+(?:com|net|org|mil|edu|arpa|ru|gov|biz|info|aero|inc|name|[a-z]{2})|(?! 0)(?:(?! 0[^.]|255)[ 0-9]{1,3}\.){3}(?! 0|255)[ 0-9]{1,3})(?:\/[a-zа-я0-9.,_@%&?+=\~\/-]*)?(?:#[^ \'\"&<>]*)?$/i;
	return regURLrf.test(url)||regURL.test(url);
}

function display_crawling_events( events )
{
	for( var i in events )
	{
		var event = events[i];
		
		switch( event.type ) {
			
			case 'debug':
				
				console.warn( "debug\nurl: " + event.url + "\ninfo: " + event.info );
				
				break;
			
			case 'crawler_activity':
				var crawler_activity = event.is_crawling == true ? crawler_events.indexation_in_progress : crawler_events.indexation_complete
				
				switch( event.value )
				{
					case "crawling":
						crawler_activity = crawler_events.indexation_in_progress;
						break;
					case "queue_empty":
						crawler_activity = crawler_events.indexation_complete;
						break;
					case "nxdomain":
						crawler_activity = crawler_events.nxdomain;
						highlight_bad_url();
						break;
					case "too_complex_content_or_overloaded_system":
						crawler_activity = crawler_events.too_complex_content_or_overloaded_system;
						break;
					case "unexpected_response_content":
						crawler_activity = crawler_events.unexpected_response_content;
						break;
					case "econnrefused":
						crawler_activity = crawler_events.econnrefused;
						break;
					case "econnreset":
						crawler_activity = crawler_events.econnreset;
						break;
					case "econnaborted":
						crawler_activity = crawler_events.econnaborted;
						break;					
					default:
						crawler_activity = event.value;
				}
				
				display_crawler_message(crawler_events.crawler_activity_label + ': ' + crawler_activity);
				break;
			case 'statistics':
				switch( event.interval )
				{
					case '10min' : 
						display_crawler_message( crawler_events.stat_10min + ": " + event.value );
						break;
					case 'hour' : 
						display_crawler_message( crawler_events.stat_hour	+ ": " + event.value );
						break;
					case 'day' : 
						display_crawler_message( crawler_events.stat_day + ": " + event.value );
						break;
					case 'week' : 
						display_crawler_message( crawler_events.stat_week + ": " + event.value );
						break;
					case 'month' : 
						display_crawler_message( crawler_events.stat_month + ": " + event.value );
						break;
					case 'year' : 
						display_crawler_message( crawler_events.stat_year + ": " + event.value );
						break;
				}
				
				break;
			case 'resp_time_avg_ms':
				display_crawler_message( crawler_events.average_response_time + ': ' + event.timing + 'ms');
				break;
			case 'url_indexed':
				var url = event.url;
				var shorten_url = url.length > config.displayed_url_length ? url.substring(0,config.displayed_url_length) + '...' : url; 
				display_crawler_message(crawler_events.page_crawled + ':<br/> <a target="_blanc" href="' + url + '">' + shorten_url + '</a>');
				break;
			
			case 'url_processed':
				var url = event.url;
				var shorten_url = url.length > config.displayed_url_length ? url.substring(0,config.displayed_url_length) + '...' : url; 
				display_crawler_message('url processed:<br/><a target="_blanc" href="' + url + '">' + shorten_url + '</a>');
				break;
			case 'url_error':
				var url = event.url;
				var shorten_url = url.length > config.displayed_url_length ? url.substring(0,config.displayed_url_length) + '...' : url; 
				var reason = event.reason;
				
				switch( reason )
				{
					case 'Page deduplicated.' : 
					display_crawler_message('<a class="bad-link" target="_blanc" href="' + url + '">' + shorten_url + '</a><br/>' + crawler_events.page_deduplicated);
					break;
					case 'HTTP request timeout.' : 
					display_crawler_message('<a class="bad-link" target="_blanc" href="' + url + '">' + shorten_url + '</a><br/>' + crawler_events.http_request_timeout);
					break;
					case 'HTTP 404.' : 
					display_crawler_message('<a class="bad-link" target="_blanc" href="' + url + '">' + shorten_url + '</a><br/>' + crawler_events.http_404);
					break;
					case 'Character encoding cannot be detected.' : 
					display_crawler_message('<a class="bad-link" target="_blanc" href="' + url + '">' + shorten_url + '</a><br/>' + crawler_events.unknown_character_encoding);
					break;
					default:
					display_crawler_message('<a class="bad-link" target="_blanc" href="' + url + '">' + shorten_url + '</a><br/>' + event.reason);
				}
				
				break;
			default:
				//console.log(crawler_events.undefined_event);
				break;
		}
	}
	
}

function display_crawler_message( message )
{
	$('#crawlEvents').append('<div class="crawler-message">' + message  + '</div>');
	$('#crawlEvents').append('<div class="crawler-delimiter"><span class="gray">></span><span class="brown">></span><span class="red">></span></div>')
}

function listen_for_events(timestamp, hostname) {
	
	$.ajax("/crawl/pull/" + hostname + "/" + timestamp,
	{
		success:
	    function(data, code, xhr) {
				
				if( $('#crawlerCaption').html() != site_indexation_caption_progress ) render_caption( 'crawlerCaption', site_indexation_caption_progress );
				
				display_crawling_events( data.events );
				
				$('.monitor-c').scrollTop('1000000');
				
				if( typeof( cometRequestTimer ) != 'undefined' ) clearTimeout(cometRequestTimer);
				cometRequestTimer = setTimeout(function()
				{
					latest_timestamp = data.timestamp;
					listen_for_events(data.timestamp, hostname);
				},config.cometRequestSpeed)
			
			},
		error :
		function()
		{
		 setTimeout(function()
		 {
		 	if( typeof( latest_timestamp ) != 'undefined' ) listen_for_events(latest_timestamp, hostname);
		 }, 5000);
		}
	});
}

function showCrawler( js_url )
{
	$('#jsCodeTextarea').val( config.install_js_template( js_url ) );
	$('.second-step .input, #installExplanation, #finalNote, #installLabel').fadeIn();
	$('.digit .unactive').animate({'margin-top':'-52px'});
}

var iOS = ( navigator.userAgent.match(/(iPad|iPhone|iPod)/i) ? true : false );

if( iOS == true )
{
	var iosCss = document.createElement('link');
	iosCss.setAttribute('rel','stylesheet');
	iosCss.setAttribute('type','text/css')
	iosCss.setAttribute('href','static/css/ios.css');
	document.getElementsByTagName('head')[0].appendChild( iosCss );
	
	$(document).ready(function()
	{
		$('a').bind('touchstart', function()
		{
			$(this).addClass('active');
		});
		$('a').bind('touchend', function()
		{
			$(this).removeClass('active');
		});
		$('.crawl-monitor').css({'width':'0px'});
		$('.crawl-border-right').hide();
	});
	
}