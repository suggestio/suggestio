config = 
{
	host    :    'https://suggest.io/',
	sio_css :    'static/css/sio.v5.css?v=4',
	searchRequestDelay: 250,
	start_typing_label : "\u043D\u0430\u0447\u043D\u0438\u0442\u0435 \u0432\u0432\u043E\u0434\u0438\u0442\u044C \u0437\u0430\u043F\u0440\u043E\u0441...",
	no_results_label : "\u043D\u0438\u0447\u0435\u0433\u043E \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E \u0434\u043B\u044F"
}

isIE = navigator.appVersion.lastIndexOf("MSIE") > 0;
isFF = navigator.userAgent.toLowerCase().indexOf("firefox") > 0;

sio = {
	is_custom : false,
	is_mobile : false,
	createElement : function ( tag, attributes, innerHTML )
	{
		new_elt = document.createElement( tag );
		for( var attr in attributes )
			new_elt.setAttribute(attr, attributes[attr]);
		
		if( innerHTML ) new_elt.innerHTML = innerHTML;
		return new_elt;
	},
	cache : {},
	findPos : function(obj) {
		var cl = ct = 0
		if (obj.offsetParent)
		{
			ct = obj.offsetTop
			cl = obj.offsetLeft
			
			while (obj = obj.offsetParent)
			{
				cl += obj.offsetLeft
				ct += obj.offsetTop
			}
		}
		return [cl,ct]
	},
	win2unicode		: function(str) {
		var charmap   = unescape(  
			"%u0402%u0403%u201A%u0453%u201E%u2026%u2020%u2021%u20AC%u2030%u0409%u2039%u040A%u040C%u040B%u040F"+  
			"%u0452%u2018%u2019%u201C%u201D%u2022%u2013%u2014%u0000%u2122%u0459%u203A%u045A%u045C%u045B%u045F"+  
			"%u00A0%u040E%u045E%u0408%u00A4%u0490%u00A6%u00A7%u0401%u00A9%u0404%u00AB%u00AC%u00AD%u00AE%u0407"+  
			"%u00B0%u00B1%u0406%u0456%u0491%u00B5%u00B6%u00B7%u0451%u2116%u0454%u00BB%u0458%u0405%u0455%u0457")  
		var code2char = function(code) {  
			if(code >= 0xC0 && code <= 0xFF) return String.fromCharCode(code - 0xC0 + 0x0410)  
			if(code >= 0x80 && code <= 0xBF) return charmap.charAt(code - 0x80)  
			return String.fromCharCode(code)  
		}  
		var res = ""  
		for(var i = 0; i < str.length; i++) res = res + code2char(str.charCodeAt(i))  
		return res  
	},
	bind	: function (obj, type, listener)
	{
		
		if( typeof( obj ) == 'undefined' || obj == null ) return false;
		
		if(obj.addEventListener)
			obj.addEventListener(type, listener, false);
			else if(obj.attachEvent)
			obj.attachEvent('on' + type, function() {listener.apply(obj);});
	},
	resize : function()
	{
		var pos = sio.findPos( sio.search_field );
		var size = [sio.search_field.offsetWidth,sio.search_field.offsetHeight]
		
		window_width = window.innerWidth;
		
		var left =  (pos[0] + 385) > window_width ? window_width - 385 : pos[0];
		
		left = left < pos[0] - 270 ? pos[0] - 270 : left;
		
		sioWidget.style.left = left + 70 - 70 + 'px';
		
		if( sio.is_custom === true )
		{
			sioWidget.style.left = '50%';
			sioWidget.style.marginLeft = '-150px';
			
			var scrollTop = document.body.scrollTop;
			
			sioWidget.style.top = scrollTop + 65 + 'px';
		}
		
		var diff = pos[0] - left - 320;
		
		if( sio.is_custom === true ) diff+=150;
		
		document.getElementById('sioCorner').style.backgroundPosition = diff + 'px top';
		
		if( sio.is_custom !== true ) sioWidget.style.top = pos[1] + size[1] + 20 + 'px';
	},
	hostname 			:	window.location.hostname,
	inputs 				: null,
	search_field	: null,
	selected_result : null,
	get_parent_node_with_tag : function(elt,tag)
	{
		var parent_node = elt.parentNode;
		
		if( typeof( parent_node ) == 'undefined' ) return false;
		
		if( !parent_node ) return false;
		
		if( parent_node.tagName == tag )
		{
			return parent_node;
		}else{
			return sio.get_parent_node_with_tag(parent_node,tag);  
		}
	},
	validate_search_dom_element : function(elt)
	{
		search_pattern = new RegExp('search', "gi");
		
		var is_search_field = false;
		
		try{
		if( elt.type == 'search') is_search_field = true;
		if( typeof( elt.className ) != 'undefined' && elt.className.match(search_pattern)) is_search_field = true;
		if( typeof( elt.action ) != 'undefined' && elt.action.match(search_pattern)) is_search_field = true;
		if( typeof( elt.name ) != 'undefined' && elt.name != '' && elt.name.match(search_pattern)) is_search_field = true;
		if( typeof( elt.id ) != 'undefined' && elt.id.match(search_pattern)) is_search_field = true;
		if( typeof( elt.placeholder ) != 'undefined' && elt.placeholder.match(search_pattern)) is_search_field = true;
		
		if( typeof( elt.value ) != 'undefined' && elt.value.match(search_pattern)) is_search_field = true;
		
		}catch(err){}
		
		return is_search_field;
		
	},
	log: function(message)
	{
		if( typeof( console ) != 'undefined' ) console.log( message );
	},
	init	: function()
	{
		
		sio.inputs = document.getElementsByTagName('input');
		
		for ( var i in sio.inputs )
		{
			var elt = sio.inputs[i];
			
			if( elt.type != 'text' && elt.type != 'search' ) continue;
			
			if( sio.validate_search_dom_element(elt) === true )
			{
				if( sio.search_field == null ) sio.search_field = elt;
				break;
				sio.log('search field found on first iteration');
			}else
			{
				var parent_form = sio.get_parent_node_with_tag(elt,'FORM');
				
				if( sio.validate_search_dom_element(parent_form) === true )
				{
					if( sio.search_field == null ) sio.search_field = elt;
					sio.log('search field found on second iteration');
					break;
				}
				else
				{
					sio.log('no search fields found');
				}
				
			}
			
		}
		
		if( sio.search_field == null )
		{
			sio.render_sio_field();
			sio.is_custom = true;
			return false;
		}
		
		sio.search_field.setAttribute('autocomplete', 'off');
		sio.search_field.style.outline = 'none';
		sio.bind(sio.search_field, 'mousedown', function()
		{
			setTimeout(function()
			{
				if( typeof( sio.searchHideTimer ) != 'undefined' ) clearTimeout( sio.searchHideTimer );
			},10);
		});
		sio.bind(sio.search_field, 'focus', function()
		{
			if( typeof( sio.searchHideTimer ) != 'undefined' ) clearTimeout( sio.searchHideTimer );
			
			sio.search_field.value = '';
			sioWidget.style.display = 'block';
			
			sio.removeClass(sioWidget,'found-none');
			sio.addClass(sioWidget,'initial');
			
			sio.resize();
		});
		
		if( typeof( sio.custom_field ) != 'undefined' )
		{
		sio.bind(sio.custom_field, 'click', function()
		{
			sio.search_field.focus();
			sio.addClass(sio.custom_field,'searchact');
		});
		sio.bind(sio.custom_field, 'mouseover', function()
		{
			if( typeof( hideCustomSearch ) != 'undefined' ) clearTimeout(hideCustomSearch);
			sio.addClass(sio.custom_field,'active');
			sio.search_field.focus();
		});
		
		sio.bind(sio.custom_field, 'mouseout', function()
		{
			if( typeof( hideCustomSearch ) != 'undefined' ) clearTimeout(hideCustomSearch);
			hideCustomSearch = setTimeout(function()
			{
				if( sio.hasClass(sio.custom_field, 'searchact') === false ) sio.search_field.blur();
				sio.removeClass(sio.custom_field,'active');
			},1500);
			
		});
		}
		sio.bind(sio.search_field, 'blur', function()
		{
			return false;
			setTimeout(function()
			{
				sioWidget.style.display = 'none';
			},150);
		});
		
		var parent_form = sio.search_field.parentNode;
		parent_form = parent_form.parentNode;
		
		parent_form.onsubmit = function()
		{
			return false;
		}
		
		sio.bind(sio.search_field, 'keydown', function( event )
		{
			sio.addClass(sio.custom_field,'searchact');
				
			event = event || window.event;
			
			if( event.keyCode == '40' || event.keyCode == '38' )
			{
				var _q = sio.search_field.value;
				if( typeof( sio.cache[_q] ) != 'undefined' )
				{
					var _r = sio.cache[_q];
					if( sio.selected_result == null )
					{
						sio.selected_result = 0;
					}
					else
					{
						event.keyCode == '40' ? sio.selected_result += 1 : sio.selected_result -= 1;
						sio.selected_result = sio.selected_result < 0 ? 0 : sio.selected_result;
						sio.selected_result = sio.selected_result >= _r.length ? _r.length-1 : sio.selected_result;
						
						var _r_elt = document.getElementById('sio_result' + sio.selected_result );
						
						var offset = _r_elt.offsetTop - 50;
						
						document.getElementById('sio_searchResults').scrollTop = offset;
						
						sio.addClass(_r_elt,'sio-selected-result')
					}
				}
				return false;
				
			}
			
			if( event.keyCode == '13' ) {
				sio.search_field.value += ' ';
				sio.search_field.value = sio.search_field.value.replace('  ',' ');
			}
			
			setTimeout(function(){
				sio.try_cached_request()
			},5);
			
			if( typeof( sio.search_timer ) != 'undefined' ) clearTimeout( sio.search_timer );
			sio.search_timer = setTimeout("sio.process_query()",config.searchRequestDelay);
		});
	},
	
	render_sio_field:function()
	{
		
		var sioSearchField = sio.createElement( 'div', {'class':'sio-search-field','id':'sio_search_field'} );
		sioSearchField.innerHTML = '<input id="sioSearchField" autocomplete="off" type="text"/><div class="sio-label" id="sioLabel">' + "\u0438\u0441\u043A\u0430\u0442\u044C" +'...</div><div class="sio-custom-corner"></div>';
		document.getElementsByTagName('body')[0].appendChild( sioSearchField );
		
		sio.sioLabel = document.getElementById('sioLabel');
		sio.custom_field = sioSearchField;
		sio.search_field = document.getElementById('sioSearchField');
		
		var all_dom_elts = document.getElementsByTagName("*");
		var default_margin_left = '-50';
		var possible_left_offset = 0;
		
		possible_left_offset = 0;
		sioSearchField.style.right = possible_left_offset + 'px';
		
		setTimeout(function(){ sio.addClass(sioWidget,'custom-widget'); },250);
		
		sio.init();
		
	},
	
	try_cached_request : function()
	{
		var _q = sio.search_field.value;
		
		if( _q == '' )
		{
			if( sio.sioLabel ) sio.sioLabel.style.display = 'block';
			sio.removeClass(sioWidget,'found-none');
			sio.addClass(sioWidget,'initial');
		}else
		{
			if( sio.sioLabel ) sio.sioLabel.style.display = 'none';
		}
		
		if( typeof( sio.cache[_q] ) != 'undefined' )
		{
			if( typeof( sio.search_timer ) != 'undefined' ) clearTimeout( sio.search_timer );
			sio.removeClass(sioWidget,'initial');
			sio._draw_result( sio.cache[_q], _q );
			
		}
	},
	process_query : function( elt )
	{
		
		var _sr = sio.search_field.value;
		if( _sr == '' ) return false;
		
		docCharset = document.inputEncoding ? document.inputEncoding : document.charset;
		
		if( typeof( docCharset ) == 'undefined' ) docCharset = document.characterSet;
		
		if(docCharset == 'windows-1251'){
			
			_sr = unescape( encodeURIComponent( _sr ) );
			_sr = sio.win2unicode( _sr );
		}
		
		sio.selected_result = null;
		
		sio.sendRequest( config.host + 'search?h=' + sio.hostname + '&q=' + _sr );
		
	},
	sendRequest : function(url)
	{
		var script = document.createElement('script');
		script.type = 'text/javascript'
		script.src = url;
		script.id = 'sio_search_request';
		
		document.getElementsByTagName('head')[0].appendChild( script );
		
		sio.addClass(sioWidget,'w-spinner');
		
	},
	_s_add_result : function(data)
	{
		
		if( sio.is_mobile === true )
		{
			siomobile.render_results( data );
			return false;
		}
		
		sio.removeClass(sioWidget,'w-spinner');
		
		try
  	{
			var query = data.q;
			var search_results = data.search_result;
			var status = data.status;
			
			sio.removeClass(sioWidget,'initial');
			
			if( status == 'found_none' )
			{
				sio.addClass(sioWidget,'found-none');
				
				query = query.length > 10 ? query.substring(0,10) + '...' : query;
				
				var no_results_message =  config.no_results_label + '<span class="found-none-highlight sio-highlight">' + query + '</span>';
				
				document.getElementById('sioFoundNone').innerHTML = no_results_message;
			}
			else
			{
				sio.cache[query] = search_results;
  			sio._draw_result( search_results, query );
  		}
			
		}
		catch(err)
  	{
  	}
			
	},
	_draw_result:function( _sr, _q)
	{
		var _sr_container = document.getElementById('sio_searchResults')
		
		document.getElementById('sio_searchResults').scrollTop = 0;
		
		sio.removeClass(sioWidget,'found-none');
		_sr_container.innerHTML = '';
		for( var i in _sr )
		{
			var r = _sr[i];
			
			if( typeof( r ) != 'object' ) return false;
			var shorten_url = r.url.length > 35 ? r.url.substring(0,35) + '...' : r.url;
			
			if( document.location.protocol == 'https:' && r.url.substring(0,6) != 'https:' ) r.url = r.url.replace('http://','https://');
			
			var r_n = '<div class="sio-result" id="sio_result' + i + '"><a class="sio-result-title" href="' + r.url + '" target="_blank">' + r.title + '</a>'
			if( r.content_text != '' ) r_n += '<div class="sio-result-desc">' + r.content_text + '</div>'
			r_n += '<div class="sio-result-link">' + shorten_url + '</div></div>';
			_sr_container.innerHTML += r_n;
		}
		
		if( _sr.length == 0 )
		{
		}
	},
	hasClass:function(element, value)
	{
		var _class_pattern = new RegExp(value, "gi");	
		return element.className.match( _class_pattern ) ? true : false;
	},
	addClass:function(element, value)
	{
		if(element==null)
			return 0;
		
		if (!element.className)
		{
			element.className = value;
		}else
		{
			var newClassName = element.className;
			
			var _class_pattern = new RegExp(value, "gi");	
			if( newClassName.match( _class_pattern ) ) return false;
			
			newClassName += " ";
			newClassName += value;
			element.className = newClassName;
		}
	},
	removeClass:function(element, value)
	{
		if( typeof( element ) == 'undefined' ) return false;
		if (!element.className)
		{
			element.className = '';
		}else
		{
			newClassName = element.className.replace(value,'');
			element.className = newClassName;
		}
	},
	hideSearch:function()
	{
		sio.searchHideTimer = setTimeout(function()
		{
			sioWidget.style.display = 'none';
			if( sio.sioLabel ) sio.sioLabel.style.display = 'block';
			sio.removeClass(sio.custom_field,'searchact');
			sio.search_field.value = '';
			sio.search_field.blur();
		},100);
	}
}

sio.init();

var sioCss = sio.createElement( 'link', {'rel':'stylesheet','type':'text/css','id':'suggestioCss', 'href' : config.host + config.sio_css} );
document.getElementsByTagName('head')[0].appendChild( sioCss );

var sioWidget = sio.createElement( 'div', {'class':'sio-widget','id':'sio_widget'} );
sioWidget.innerHTML = '<div class="sio-content-container"><div class="sio-spinner"></div><div class="sio-corner" id="sioCorner"></div><div class="sio-footer"><div class="sio-footer-inner"><a href="http://suggest.io/" target="_blank"><span></span></a></div></div><div class="found-none-message" id="sioFoundNone"></div><div class="sio-initial-message" id="sioInitialMessage">' + config.start_typing_label + '</div><div class="sio-results-container"><div class="sio-results" id="sio_searchResults"></div></div></div>';
sioWidget.style.display = 'none';
document.getElementsByTagName('body')[0].appendChild( sioWidget );

sio.bind(window, 'resize', sio.resize);
sio.bind(window, 'scroll', sio.resize);

var events = ['mousedown','touchstart'];

for ( var e in events )
{
	var event = events[e];
	sio.bind(window, event, sio.hideSearch);
	sio.bind(sioWidget, event, function( event )
	{
		event.stopPropagation();
	});
}