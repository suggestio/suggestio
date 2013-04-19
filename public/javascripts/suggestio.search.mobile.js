siomobile = 
{
	viewport_tag : null,
	sio_mobile_input : null,
	sio_mobile_search_box : null,
	init : function()
	{
		// wait for sio
		
		if( typeof( sio ) == 'undefined' )
		{
			setTimeout(function(){ siomobile.init(); },10);
			return false;
		}
		
		// turn on mobile mode
		sio.is_mobile = true;
		
		// check if there is meta viewport on page
		
		var meta_tags = document.getElementsByTagName('meta');
		var viewport_tag = null;
		for( var i in meta_tags )
		{
			var tag = meta_tags[i];
			if( tag.name == 'viewport' )
			{
				viewport_tag = tag;
				break;
			}
		}
		
		// create viewport tag if needed
		
		if( viewport_tag == null )
		{
			viewport_tag = document.createElement('meta');
			viewport_tag.setAttribute('name','viewport');
			viewport_tag.id = "viewport";
			
			document.getElementsByTagName('head')[0].appendChild(viewport_tag);
			siomobile.viewport_tag = viewport_tag;
		}
		
		// load mobile css
		var sio_mobile_css = sio.createElement( 'link', {'rel':'stylesheet',
																										 'type':'text/css',
																										 'id':'suggestioCss',
																										 'href' : config.host + 'static/css/sio.search.mobile.css'} );
		document.getElementsByTagName('head')[0].appendChild( sio_mobile_css );
		
		// create mobile search box
		var sio_mobile_input = sio.createElement( 'input', {'type':'text',
																												'class':'sio-mobile-input',
																												'autocorrect':'off'});
		
		sio_mobile_input.id = "sioMobileInput";
		document.getElementsByTagName('body')[0].appendChild( sio_mobile_input );
		
		siomobile.sio_mobile_input = sio_mobile_input;
		
		// create mobile search results
		var sio_mobile_search_box = sio.createElement( 'div', {'class':'sio-mobile-search-box'});
		
		sio_mobile_search_box.id = "sioMobileSearchBox";
		sio_mobile_search_box.innerHTML = '<div id="sioMobileSearchContent" class="sio-mobile-search-content"></div>';
		document.getElementsByTagName('body')[0].appendChild( sio_mobile_search_box );
		siomobile.sio_mobile_search_box = sio_mobile_search_box;
		
		// change events
		sio.bind(sio.search_field, 'focus', function()
		{
			siomobile.sio_mobile_input.style.display = 'block';
			siomobile.sio_mobile_search_box.style.display = 'block';
			siomobile.sio_mobile_input.focus();
		});
		
		// bind mousedown events
		sio.bind(siomobile.sio_mobile_input, 'keydown', function()
		{
			if( typeof( siomobile.search_timer ) != 'undefined' ) clearTimeout( siomobile.search_timer );
			siomobile.search_timer = setTimeout("siomobile.process_query()", 250);
		});
		
	},
	process_query : function()
	{
		
		var _sr = siomobile.sio_mobile_input.value;
		if( _sr == '' ) return false;
		
		docCharset = document.inputEncoding ? document.inputEncoding : document.charset;
		
		if( typeof( docCharset ) == 'undefined' ) docCharset = document.characterSet;
		
		if(docCharset == 'windows-1251'){
			
			_sr = unescape( encodeURIComponent( _sr ) );
			_sr = sio.win2unicode( _sr );
		}
		
		sio.sendRequest( config.host + 'search?h=' + sio.hostname + '&q=' + _sr );
		
	},
	render_results : function( data )
	{
		var _q = data.q;
		
		if( data.status == 'found_none' )
		{
			var results_html = '<div class="sio-mobile-search-result">No results for &laquo;' + _q + '&raquo;</div>';
		}
		else
		{
			var _sr = data.search_result;
			
		var results_html = '';
		
		for( var i in _sr )
		{
			var result = _sr[i];
			results_html += '<div class="sio-mobile-search-result" onclick="window.location=\'' + result.url +  '\'">' + result.title + '<span class="sio-r-link">' + result.url + '</span></div>'
		}
		}
		var _s_container = document.getElementById('sioMobileSearchContent');
		siomobile.sio_mobile_search_box.scrollTop = 0;
		
		_s_container.innerHTML = results_html;
		
	}
}

siomobile.init();