/*{# Шаблон для скриптоты. Обычно выполняется без переменных и кешируется на клиентах. #}
{# если происходит добавление домена в базу suggest.io #}
{% if render_installer %}
{# доступны переменные:
    - dkey. Например "vasya.ru"
    - timestamp для GET /js/pull/vasya.ru/123123123123
#}
{% endif %}
*/
(function() {
	
	if( typeof( window.sio ) != 'undefined' ) return false;
	
	var ce = function ( tag, attributes, inhtml )
	{
		var ne = document.createElement( tag );
		for( var attr in attributes )
			ne.setAttribute(attr, attributes[attr]);
		
		if( typeof( inhtml ) != 'undefined' ) ne.innerHTML = inhtml;
		
		return ne;
	};
	
	var re = function( e )
	{
		e = typeof( e ) == 'string' ? ge(e) : e;
		
		if( !exists( e ) ) return false;
		var p = e.parentNode
		
		if( p != null ) p.removeChild(e);
	}
	
	var ge = function ()
	{
		var ea;
		for (var i = 0; i < arguments.length; i++) {
			var e = arguments[i];
			if (typeof e == 'string' || typeof e == 'number')
				e = document.getElementById(e);
			if (arguments.length == 1)
				return e;
			if (!ea)
				ea = new Array();
			ea.push(e);
		}
		return ea;
	}
	
	var ge_tag = function( tag )
	{
		return document.getElementsByTagName( tag );
	}
	
	var hasClass = function(element, value)
	{
		var _class_pattern = new RegExp(value, "gi");	
		return element.className.match( _class_pattern ) ? true : false;
	};
	
	var addClass = function(element, value)
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
	};
	
	var removeClass = function(element, value)
	{
		if( typeof( element ) == 'undefined' ) return false;
		if (!element.className)
		{
			element.className = '';
		}else
		{
			newClassName = element.className.replace(value,'').replace(/\s{2,}/g, ' ');
			element.className = newClassName;
		}
	};
	
	var create_css = function( content )
	{
		//var c = ce('style',{'id':'sio_custom_css','type':'text/css'}, content );
		
		var c = document.createElement('style');
		c.id = 'sio_custom_css';
		c.type = 'text/css';
		
		if( typeof( c.styleSheet ) == 'undefined' )
		{
			c.innerHTML = content;
		}
		else
		{
			c.styleSheet.cssText = content;
		}
		
		if( ge('sio_custom_css') ) re( ge('sio_custom_css') );
		
		ge_tag('head')[0].appendChild(c);
	}
	
	var findPos = function(o)
	{
		var cl = ct = 0;
		if (o.offsetParent)
		{
			ct = o.offsetTop
			cl = o.offsetLeft
			
			while (o = o.offsetParent)
			{
				cl += o.offsetLeft
				ct += o.offsetTop
			}
		}
		return [cl,ct]
	};
	
	var is_array = function(o)
	{
		return Object.prototype.toString.call( o ) == '[object Array]';
	};
	
	var bind = function (o, type, listener)
	{
		if( typeof( o ) == 'undefined' || o == null ) return false;
		
		if( is_array( o ) )
		{
			siomap( function( _obj )
			{
				if( is_array( type ) )
				{
					siomap( function(_t)
					{
						addListener(_obj,_t,listener);
					}, type);
				}else
				{
					addListener(_obj,type,listener);
				}
			},o);
			
		}
		else
		{
			if( is_array( type ) )
			{
				siomap( function(_t)
				{
					addListener(o,_t,listener);
				}, type);
			}else
			{
				addListener(o,type,listener);
			}
		}
		
	};
	
	var addListener = function(o,type,listener)
	{
		if(o.addEventListener)
			o.addEventListener(type, listener, false);
			else if(o.attachEvent)
			o.attachEvent('on' + type, function() { listener.apply(o); });
	};
	
	var exists = function(o)
	{ return typeof( o ) == 'undefined' ? false : true; }
	
	var sio_log = function(m)
	{
		if( typeof( console ) == 'undefined' ) return false;
		console.log( m ); 
	};
	
	var siomap = function( fun, list )
	{
		for( var i in list )
		{
			if( typeof( list[i] ) != 'function' ) fun( list[i],i );
		}
	}
	
	var sio_obj_length = function(o)
	{
    var s = 0, k;
    for (k in o) {
        if (o.hasOwnProperty(k)) s++;
    }
    return s;
	};
	
	var sio_hex_to_rgb = function( hex )
	{
		var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
		return result ? {
			r: parseInt(result[1], 16),
			g: parseInt(result[2], 16),
			b: parseInt(result[3], 16)
		} : null;
	}
	
	var sio_component_to_hex = function (c) {
		var hex = c.toString(16);
		return hex.length == 1 ? "0" + hex : hex;
	}
	
	var sio_rgb_to_hex = function(r, g, b) {
		rgb = [].slice.call(arguments).join(",").match(/\d+/g);
      var hex,l;l=( hex = ( (rgb[0] << 16 ) + ( rgb[1] << 8 ) + +rgb[2] ).toString(16) ).length;
         while( l++ < 6 )
             hex="0"+hex 
      
    return hex;
	}
	
	var sio_get_style = function(elem, name)
	{
		// J/S Pro Techniques p136
		if (elem.style[name]) {
			return elem.style[name];
		} else if (elem.currentStyle) {
			return elem.currentStyle[name];
		}
		else if (document.defaultView && document.defaultView.getComputedStyle) {
			name = name.replace(/([A-Z])/g, "-$1");
			name = name.toLowerCase();
			s = document.defaultView.getComputedStyle(elem, "");
			return s && s.getPropertyValue(name);
		} else {
			return null;
		}
	}
	
	var win2unicode = function(str) {
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
	};
	
	var get_page_colors = function()
	{
		var colors = [];
		
		siomap(function(e)
		{
			try
			{
				
				var params_to_try = ['color','background-color'];
				
				siomap(function(_p)
				{
					
					var color = sio_get_style(e, _p);
					
					color = color.replace('rgb','').replace('(','').replace(')','').replace(' ','').replace(' ','');
					
					color = color.split(',');
					
					color = sio_rgb_to_hex( color[0],color[1],color[2] );
					
					if( colors.indexOf(color) == -1 )
					{
						colors.push( color );
					}
					
				}, params_to_try);
				
			}catch(err)
			{
			}
		},ge_tag('*'));
		
		return colors;
	}
	
	var rand = function()
	{
		return Math.floor( Math.random() * 10000000 );
	}
	
	var config = 
	{
		c_locale : 'ru',
		preferences_trigger_hash : '#sio_preferences',
		host    :    'https://suggest.io/',
		sio_css :    'static/css/sio.v7.css?v=' + rand(),
		searchRequestDelay: 250,
		search_hide_delay : 50,
		moseover_hide_delay : 2000,
		search_field_test_depth : 5,
		default_lang : 'ru',
		start_typing_label : "\u043D\u0430\u0447\u043D\u0438\u0442\u0435 \u0432\u0432\u043E\u0434\u0438\u0442\u044C \u0437\u0430\u043F\u0440\u043E\u0441...",
		found_none_label : "\u043D\u0438\u0447\u0435\u0433\u043E \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E \u0434\u043B\u044F",
		custom_label : "\u0438\u0441\u043A\u0430\u0442\u044C"
	};
	
	var langs = 
	{
		ru :
		{
			start_typing_label : "\u043D\u0430\u0447\u043D\u0438\u0442\u0435 \u0432\u0432\u043E\u0434\u0438\u0442\u044C \u0437\u0430\u043F\u0440\u043E\u0441...",
			found_none_label : "\u043D\u0438\u0447\u0435\u0433\u043E \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u043E \u0434\u043B\u044F",
			custom_label : "\u0438\u0441\u043A\u0430\u0442\u044C"
		},
		en :
		{
			start_typing_label : "start typing search request",
			found_none_label : "nothing found",
			custom_label : "search"
		},
		ua :
		{
			start_typing_label : "\u043F\u043E\u0447\u043D\u0456\u0442\u044C \u0432\u0432\u043E\u0434\u0438\u0442\u0438 \u0437\u0430\u043F\u0438\u0442...",
			found_none_label : "\u043D\u0456\u0447\u043E\u0433\u043E \u043D\u0435 \u0437\u043D\u0430\u0439\u0434\u0435\u043D\u043E",
			custom_label : "\u043F\u043E\u0448\u0443\u043A"
		},
		lt :
		{
			start_typing_label : "prad\u0117kite ra\u0161yti paie\u0161kos u\u017eklaus\u0105",
			found_none_label : "Nieko nerasta",
			custom_label : "Paie\u0161ka"
		}
	}
	
	var get_lang = function()
	{
		var _dd = domain_data;
		return typeof( _dd['lang'] ) != 'undefined' ? _dd['lang'] : config.default_lang;
	}
	
	
	var sio = {}
	
	sio.win2unicode = win2unicode;
	
	var domain_data;
	
	var locales =
	{
		'ru' :
		{
			'sf_background_color' : '\u041f\u043e\u043b\u0435 \u0432\u0432\u043e\u0434\u0430 \u0437\u0430\u043f\u0440\u043e\u0441\u0430',
			'background_color' : '\u0424\u043e\u043d \u043e\u043a\u043d\u0430',
			'border_color' : '\u041e\u0431\u0432\u043e\u0434\u043a\u0430',
			'title_color' : '\u0417\u0430\u0433\u043e\u043b\u043e\u0432\u043e\u043a',
			'highlight_color' : '\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430',
			'link_color' : '\u0426\u0432\u0435\u0442 \u0441\u0441\u044b\u043b\u043a\u0438',
			'window_width' : '\u0420\u0430\u0437\u043c\u0435\u0440\u044b \u043e\u043a\u043d\u0430 \u0441 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u043c\u0438 \u043f\u043e\u0438\u0441\u043a\u0430',
			'left_offset' : '\u041e\u0442\u0441\u0442\u0443\u043f \u0441\u043b\u0435\u0432\u0430',
			'top_offset' : '\u041e\u0442\u0441\u0442\u0443\u043f \u0441\u0432\u0435\u0440\u0445\u0443',
			'inner_border_color' : '\u0412\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u044f\u044f \u043e\u0431\u0432\u043e\u0434\u043a\u0430',
			'corner_offset' : '\u041f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0443\u0433\u043e\u043b\u043a\u0430',
			'results_delimiter_color' : '\u0420\u0430\u0437\u0434\u0435\u043b\u0438\u0442\u0435\u043b\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432',
			'search_field_ttl' : '\u0418\u043a\u043e\u043d\u043a\u0430 \u043f\u043e\u0438\u0441\u043a\u0430',
			'search_window_ttl' : '\u0428\u0430\u0431\u043b\u043e\u043d \u043e\u043a\u043d\u0430 \u0441 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u043c\u0438',
			'fonts_ttl' : '\u0428\u0440\u0438\u0444\u0442\u044B',
			'colors_ttl' : '\u0426\u0432\u0435\u0442\u0430',
			'dimensions_ttl' : '\u0420\u0430\u0437\u043c\u0435\u0440\u044b \u0438 \u043e\u0442\u0441\u0442\u0443\u043f\u044b',
			'language_ttl' : '\u042F\u0437\u044B\u043A',
			'result_description_color' : '\u0422\u0435\u043a\u0441\u0442',
			'language_block_explanation' :'\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u044f\u0437\u044b\u043a \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u0432\u0438\u0434\u0436\u0435\u0442\u0430',
			'search_preferences_ttl' :'\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u043f\u043e\u0438\u0441\u043a\u0430',
			'search_preferences_block_explanation' :'\u0414\u043b\u044f \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0439 \u0432\u044b\u0434\u0430\u0447\u0438 \u0432\u0435\u0440\u043d\u0438\u0442\u0435\u0441\u044c \u0432 \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440\u0441\u043a\u0438\u0439 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441 <a href="https://suggest.io/admin">Suggest.io</a>',
			'fonts_block_explanation' : '\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0448\u0440\u0438\u0444\u0442 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u0432\u0438\u0434\u0436\u0435\u0442\u0430',
			'search_window_block_explanation' : '\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0448\u0430\u0431\u043b\u043e\u043d \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u043e\u043a\u043d\u0430',
			'search_field_block_explanation' : '\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0432\u0438\u0434 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u043f\u043e\u043b\u044f',
			'window_close_confirmation' : '\u0412\u044b \u0443\u0432\u0435\u0440\u0435\u043d\u044b \u0447\u0442\u043e \u0445\u043e\u0442\u0438\u0442\u0435 \u0437\u0430\u043a\u0440\u044b\u0442\u044c \u043e\u043a\u043d\u043e \u0431\u0435\u0437 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u0439?',
			'search_window_block_hint' : '\u0414\u043b\u044f \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0434\u0438\u0437\u0430\u0439\u043d\u0430 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0439 \u0432\u044b\u0434\u0430\u0447\u0438, \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u044b\u0439 \u0437\u0430\u043f\u0440\u043e\u0441',
			'use_field_on_page' : '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u044e\u0449\u0435\u0435 \u043f\u043e\u043b\u0435 \u043f\u043e\u0438\u0441\u043a\u0430 \u043d\u0430 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0435',
			'show_results_in' : '\u0420\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u044b \u043f\u043e\u0438\u0441\u043a\u0430',
			'blank_page' : '\u0412 \u043d\u043e\u0432\u043e\u043c \u043e\u043a\u043d\u0435',
			'self_page' : '\u0412 \u0442\u0435\u043a\u0443\u0449\u0435\u043c \u043e\u043a\u043d\u0435'
		},
		'en':
		{
			'sf_background_color' : 'Search field background',
			'background_color' : 'Background',
			'border_color' : 'Border',
			'title_color' : 'Title',
			'highlight_color' : 'Result highlight',
			'link_color' : 'Link',
			'window_width' : 'Window width',
			'left_offset' : 'Left offset',
			'top_offset' : 'Top offset',
			'inner_border_color' : 'Inner stroke color',
			'corner_offset' : 'corner offset',
			'results_delimiter_color' : 'Results delimiter',
			'search_field_ttl' : 'Search field',
			'search_window_ttl' : 'Search window',
			'fonts_ttl' : 'Fonts',
			'colors_ttl' : 'Colors',
			'dimensions_ttl' : 'Dimensions',
			'language_ttl' : 'Language',
			'search_preferences_ttl' :'Search preferences',
			'search_preferences_block_explanation' :'To manage search results, please go back to Suggest.io',
			'result_description_color' : 'Result text',
			'language_block_explanation' :'Choose language of search widget',
			'fonts_block_explanation' : 'Select font',
			'search_window_block_explanation' : 'Select search window template',
			'search_field_block_explanation' : 'Select search field type',
			'window_close_confirmation' : 'Do you really want to close window without saving changes?',
			'search_window_block_hint' : 'Please, mention, that you can type in search request to see results preferences',
			'use_field_on_page' : 'Use search field on page'
		}
	}
	
	var translate = function( what )
	{
		return locales[config.c_locale][what];
	};
	
	var retina_prefix = ' @media only screen and (-Webkit-min-device-pixel-ratio: 1.5),only screen and (-moz-min-device-pixel-ratio: 1.5),only screen and (-o-min-device-pixel-ratio: 3/2),only screen and (min-device-pixel-ratio: 1.5)';
	var sio_csf_prefix = '.sio-custom-sf { background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>.png?v=6\')!important; }';
	var templates = 
	{
		search_fields :
		{
			'crnr':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'crnr-1','ds':'34x34','ds_retina':'66x66'},
				css : sio_csf_prefix + ' .sio-csf-trigger { width: 34px; height: 34px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-crnr-1.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 33px; height: 33px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-crnr-1-retina.png?v=6\')!important; background-size: 33px 33px!important; }'
			},
			'crnr-2':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'crnr-2','ds':'37x37','ds_retina':'82x82'},
				css : sio_csf_prefix + ' .sio-csf-trigger { width: 37px; height: 37px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-crnr-2.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 41px; height: 41px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-crnr-2-retina.png?v=6\')!important; background-size: 41px 41px!important; }'
			},
			'crnr-3':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'crnr-3','ds':'37x37','ds_retina':'72x72'},
				css : sio_csf_prefix + ' .sio-csf-trigger { width: 36px; height: 36px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-crnr-3-retina.png?v=6\')!important; background-size: 36px 36px!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 36px; height: 36px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-crnr-3-retina.png?v=6\')!important; background-size: 36px 36px!important; }'
			},
			'flag-1':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-1','ds':'26x36','ds_retina':'52x72'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 26px; height: 36px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-1.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 26px; height: 36px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-1-retina.png?v=6\')!important; background-size: 26px 36px!important; }'
			},
			'flag-2':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-2','ds':'28x42','ds_retina':'54x82'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 28px; height: 42px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-2.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 27px; height: 42px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-2-retina.png?v=6\')!important; background-size: 27px 42px!important; }'
			},
			'flag-3':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-3','ds':'27x35','ds_retina':'52x70'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 27px; height: 35px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-3.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 26px; height: 35px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-3-retina.png?v=6\')!important; background-size: 26px 35px!important; }'
			},
			'flag-4':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-4','ds':'28x44','ds_retina':'54x92'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 28px; height: 44px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-4.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 27px; height: 46px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-4-retina.png?v=6\')!important; background-size: 27px 46px!important; }'
			},
			'flag-5':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-5','ds':'23x37','ds_retina':'48x74'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 23px; height: 37px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-5.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 24px; height: 37px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-5-retina.png?v=6\')!important; background-size: 24px 37px!important; }'
			},
			
			'flag-6':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-6','ds':'18x46','ds_retina':'34x88'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 18px; height: 45px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-6.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 18px; height: 44px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-6-retina.png?v=6\')!important; background-size: 18px 44px!important; }'
			},
			'flag-7':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-7','ds':'22x40','ds_retina':'44x80'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 22px; height: 40px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-7.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 22px; height: 40px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-7-retina.png?v=6\')!important; background-size: 22px 40px!important; }'
			},
			'flag-8':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-8','ds':'34x32','ds_retina':'68x64'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 34px; height: 32px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-8.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 34px; height: 32px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-8-retina.png?v=7\')!important; background-size: 34px 32px!important; }'
			},
			'flag-9':{
				params : [{p:'sf_background_color',n: translate('sf_background_color'), d:'868686'}],
				gen_params : {'prefix':'flag-9','ds':'24x28','ds_retina':'48x56'},
				css : sio_csf_prefix + ' .sio-csf-trigger { right: 10px!important; width: 24px; height: 28px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-9.png?v=6\')!important; }' +
							retina_prefix + '{ .sio-csf-trigger { width: 24px; height: 28px; background: url(\'' + config.host + 'static/images_generated/<<sf_background_color>>-flag-9-retina.png?v=6\')!important; background-size: 24px 28px!important; }'
			},
			'default':
			{
				params : [],
				css : ''
			}
		},
		search_windows :
		{
			// fatborder
			'default' :
			{
				tpl : function()
				{
					return	'<div class="sio-fb-cont">' +
										
										'<div class="sio-fb-corner" id="sioSwCorner">' +
											'<svg xmlns="http://www.w3.org/2000/svg" version="1.1">' +
  											'<polygon points="0,17 16,0 32,17" />' +
											'</svg>' +
										'</div>'+
										
										'<div class="sio-fb-inner">' +
												'<div class="sio-initial-message sio-fb-initial-message">' +
													langs[get_lang()].start_typing_label +
												'</div>' +
												'<div class="sio-fb-search-results sio-search-results" id="sio_searchResults"></div>' +
												'<div class="sio-fb-not-found sio-found-none">' + langs[get_lang()].found_none_label + '&nbsp;<span id="sioNotFoundQ"></span></div>' +
										'</div>' +
										
										'<div class="sio-fb-footer"><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-label-ads.png"/></a></div>' + 
										
									'</div>';
				},
				css : '.sio-cwd-default { width: <<window_width>>px!important; } .sio-fb-search-results { max-height: <<max_height>>px!important; } .sio-fb-cont .sio-result-desc { color: #<<desc_color>>!important; } .sio-fb-corner { left: <<corner_offset>>px!important; } .sio-fb-inner { background-color: #<<bg_color>>!important; border-color: #<<border_color>>!important; } .sio-fb-cont .sio-result { border-color: #<<border_color>>!important; } .sio-fb-corner polygon { fill: #<<bg_color>>!important; stroke: #<<border_color>>!important; } .sio-fb-initial-message, .sio-fb-not-found, .sio-fb-cont .sio-result-title { color: #<<title_color>>!important; } .sio-fb-cont em { background-color: #<<highlight_color>>!important; } .sio-fb-cont .sio-result-link { color: #<<link_color>>!important; }',
				params : {'colors' :
									[{p:'bg_color',n:	translate('background_color'),d:'ffffff'},
									 {p:'border_color',n:translate('border_color'),d:'707172'},
									 {p:'title_color',n:translate('title_color'),d:'0f0f0f'},
									 {p:'desc_color',n:translate('result_description_color'),d:'478BA2'},
									 {p:'highlight_color',n:translate('highlight_color'),d:'c9d6e2'},
									 {p:'link_color',n:translate('link_color'),d:'5bb6d5'}],
									'dimensions':[{p:'window_width',n:translate('window_width'),d:'400',min:200,max:1000},
																{p:'left_offset', n:translate('left_offset'), d: 0, min:-500, max : 500},
																{p:'corner_offset', n:translate('corner_offset'), d: 20, min:20, max : 1000},
																{p:'top_offset', n:translate('top_offset'), d: 30, min:0, max : 60},
																{p:'max_height', n:translate('max_height'), d: 300, min:200, max : 1000}]
									},
				thumbnail : 'sio-fatborder-preview.png',
				def_top_offset : function() { return 0; }
			},
			'fatborder' : 
			{
				tpl: function()
				{
					return '<div class="sio-default-cont">' +
								'<div class="sio-spinner"></div>' +
								'<div class="sio-corner sio-default-corner" id="sioCorner" style="">' +
									'<svg xmlns="http://www.w3.org/2000/svg" version="1.1">' +
  									'<polygon class="sio-default-outer-polygon" points="-4,17 14,1 15,1 33,17" />' +
  									'<polygon class="sio-default-inner-polygon" points="0,17 14,5 15,5 29,17" />' +
									'</svg>' +
								'</div>' +
								'<div class="sio-footer">' +
									'<div class="sio-footer-inner"><a href="' + config.host + '" target="_blank"><span></span></a></div>'+
								'</div>' +
								'<div class="found-none-message sio-found-none sio-def-found-none" id="sioFoundNone">' + langs[get_lang()].found_none_label  + '&nbsp;<span id="sioNotFoundQ"></span></div>' +
								'<div class="sio-initial-message sio-initial-message sio-def-initial-message" id="sioInitialMessage">' + langs[get_lang()].start_typing_label + '</div>' +
								'<div class="sio-results-container sio-search-results  sio-def-search-results">' +
									'<div class="sio-results" id="sio_searchResults">' +
									'</div>' +
								'</div>' +
								'<div class="sio-def-footer"><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-label-ads.png"/></a><div class="sio-clear"></div></div>'
							'</div>';
				},
				css : '.sio-default-cont { background: #<<second_stroke_color>>; width: <<window_width>>px; border-color: #<<stroke_color>>; } .sio-default-corner { left: <<corner_offset>>px!important; } .sio-default-cont em { background: #<<highlight_color>>!important; } .sio-def-initial-message,.sio-def-search-results,.sio-def-found-none,.sio-def-footer { background: #<<bg_inner>>; } .sio-default-outer-polygon { stroke: #<<stroke_color>>; fill: #<<second_stroke_color>>; } .sio-default-inner-polygon { fill: #<<bg_inner>>; } .sio-def-initial-message, .sio-def-search-results, .sio-def-found-none, .sio-default-cont .sio-result-title { color: #<<title_color>> } .sio-default-cont .sio-result-desc { color: #<<desc_color>> } .sio-default-cont .sio-result-link { color: #<<link_color>> }',
				params : {'colors' :[{p:'stroke_color',n:translate('border_color'),d:'1F2B2D'},
														 {p:'second_stroke_color',n:translate('inner_border_color'),d:'ffffff'},
														 {p:'bg_inner',n:translate('background_color'),d:'1F2B2D'},
														 {p:'title_color',n:translate('title_color'),d:'ffffff'},
														 {p:'desc_color',n:translate('result_description_color'),d:'cccccc'},
														 {p:'highlight_color',n:translate('highlight_color'),d:'c9d6e2'},
														 {p:'link_color',n:translate('link_color'),d:'5bb6d5'}],
									'dimensions':[{p:'window_width',n:translate('window_width'),d:'414',min:200,max:1000},
																{p:'left_offset', n:translate('left_offset'), d: 0, min:-500, max : 500},
																{p:'corner_offset', n:translate('corner_offset'), d: 20, min:0, max : 1000},
																{p:'top_offset', n:translate('top_offset'), d: 30, min:0, max : 60}]
								},
				thumbnail : 'sio-default-preview.png',
				def_top_offset : function() { return 15; }
			},
			
			'plaintext' :
			{
				tpl : function()
				{
					return '<div class="sio-pt-cont">' +
										  '<div class="sio-pt-inner">' +
										  	'<div class="sio-pt-inner-2">' + 
										  		'<div class="sio-initial-message sio-pt-initial-message">' +
										  			langs[get_lang()].start_typing_label +
										  		'</div>' +
										  		'<div class="sio-pt-search-results sio-search-results" id="sio_searchResults"></div>' +
										  		'<div class="sio-pt-not-found sio-found-none">' + langs[get_lang()].found_none_label + '&nbsp;<span id="sioNotFoundQ"></span></div>' +
										  	'</div>' +
										  	'<div class="sio-pt-ads"><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-label-ads.png"/></a></div>' +
										  '</div>' +
										'</div>';
				},
				css : '.sio-cwd-plaintext { width: <<window_width>>px!important; } .sio-pt-inner-2 { background: #<<bg_color>>!important; } .sio-pt-ads { background: #<<border_color>>!important; } .sio-pt-inner { border-color: #<<border_color>>!important; } .sio-cwd-plaintext .sio-result-title, .sio-pt-initial-message, .sio-pt-not-found { color: #<<title_color>>!important; } .sio-cwd-plaintext .sio-result { border-color: #<<results_delimiter_color>>!important; } .sio-cwd-plaintext .sio-result-desc { color: #<<desc_color>>!important; } .sio-cwd-plaintext .sio-result-link { color: #<<link_color>>!important; } .sio-cwd-plaintext em { background: #<<highlight_color>>!important; }',
				params : {'colors' : [{p:'bg_color',n:translate('background_color'),d:'FFFFFF'},
															{p:'border_color',n:translate('border_color'),d:'A4A4A4'},
															{p:'title_color',n:translate('title_color'),d:'213845'},
															{p:'highlight_color',n:translate('highlight_color'),d:'478BA2'},
									 						{p:'desc_color',n:translate('result_description_color'),d:'478BA2'},
									 						{p:'link_color',n:translate('link_color'),d:'478BA2'},
									 						{p:'results_delimiter_color',n:translate('results_delimiter_color'),d:'ecebeb'}],
									'dimensions':[{p:'window_width',n:translate('window_width'),d:'500',min:200,max:1000},
																{p:'left_offset', n:translate('left_offset'), d: 0, min:-500, max : 500},
																{p:'top_offset', n:translate('top_offset'), d: 30, min:0, max : 60}]
								 },
				thumbnail : 'sio-plaintext-preview.png',
				def_top_offset : function() { return 6; }
			},
			
			'roundcorner' :
			{
				tpl : function()
				{
					return	'<div class="sio-rc-cont">' +
										
										'<div class="sio-rc-corner" id="sioSwCorner">' +
											'<svg xmlns="http://www.w3.org/2000/svg" version="1.1">' +
  											'<polygon points="0,18 17,1 19,0 21,1 38,18" style="stroke-width:0" />' +
											'</svg>' +
										'</div>'+
										
										'<div class="sio-rc-inner">' +
												'<div class="sio-initial-message sio-rc-initial-message">' +
													langs[get_lang()].start_typing_label +
												'</div>' +
												'<div class="sio-rc-search-results sio-search-results" id="sio_searchResults"></div>' +
												'<div class="sio-rc-not-found sio-found-none">' + langs[get_lang()].found_none_label + '&nbsp;<span id="sioNotFoundQ"></span></div>' +
										'</div>' +
										
										'<div class="sio-rc-footer"><div class="sio-rc-footer-fl sio-rc-f-line"></div><div class="sio-rc-footer-sl sio-rc-f-line"></div><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-label-ads.png"/></a></div>' + 
										
									'</div>';
				},
				css : '.sio-cwd-roundcorner { width: <<window_width>>px; }  .sio-rc-search-results { max-height: <<max_height>>px!important; } .sio-rc-cont { background: #<<bg_color>>; } .sio-rc-inner, .sio-rc-f-line { border-color: #<<border_color>>; } .sio-cwd-roundcorner .sio-result-title,.sio-rc-initial-message, .sio-rc-not-found { color: #<<title_color>>; }.sio-rc-corner svg polygon { fill: #<<bg_color>>; } .sio-rc-corner { left: <<corner_offset>>px!important; } .sio-cwd-roundcorner .sio-result-link { color: #<<link_color>> } .sio-cwd-roundcorner .sio-result-desc { color: #<<desc_color>> } .sio-cwd-roundcorner .sio-result-title em, .sio-rc-not-found span { background: #<<highlight_color>>; }',
				params : {'colors' :
									[{p:'bg_color',n:translate('border_color'),d:'213845'},
									 {p:'title_color',n:translate('title_color'),d:'000000'},
									 {p:'desc_color',n:translate('result_description_color'),d:'868686'},
									 {p:'link_color',n:translate('link_color'),d:'5bb6d5'},
									 {p:'highlight_color',n:translate('highlight_color'),d:'cddae6'}
									],
									'dimensions':[{p:'window_width',n:translate('window_width'),d:'420',min:200,max:1000},
																{p:'left_offset', n:translate('left_offset'), d: 0, min:-500, max : 500},
																{p:'top_offset', n:translate('top_offset'), d: 30, min:0, max : 60},
																{p:'corner_offset', n:translate('corner_offset'), d: 20, min:0, max : 1000},
																{p:'max_height', n:translate('max_height'), d: 300, min:200, max : 1000}]
									},
				thumbnail : 'sio-roundcorner-preview.png',
				def_top_offset : function() { return 20; }
			},
			
			// strange
			'strange' :
			{
				tpl : function()
				{
					return	'<div class="sio-st-cont">' +
										
										'<div class="sio-st-inner">' +
												'<div class="sio-initial-message sio-st-initial-message">' +
													langs[get_lang()].start_typing_label +
												'</div>' +
												'<div class="sio-st-header"><div class="shadow"></div></div>' + 
												'<div class="sio-st-search-results sio-search-results" id="sio_searchResults"></div>' +
												'<div class="sio-st-footer"><div class="shadow"></div><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-ads-noborder.png"/></a></div>' + 
												'<div class="sio-st-not-found sio-found-none">' + langs[get_lang()].found_none_label + '&nbsp;<span id="sioNotFoundQ"></span></div>' +
										'</div>' +
										
									'</div>';
				},
				css : '.sio-cwd-strange { width: <<window_width>>px!important; } .sio-st-cont { background: #<<bg_color>>!important; border-color: #<<border_color>>!important; } .sio-st-footer, .sio-st-header { border-color: #<<border_color>>!important; } .sio-cwd-strange .sio-result-title, .sio-st-not-found, .sio-st-initial-message  { color: #<<title_color>>!important; } .sio-cwd-strange .sio-result-link  { color: #<<link_color>>!important; }  .sio-cwd-strange .sio-result-desc { color: #<<desc_color>>!important; } .sio-cwd-strange .sio-result em { background: #<<em_color>>!important; } ',
				params : {'colors' :
									[{p:'bg_color',n:translate('background_color'),d:'213845'},
									 {p:'border_color',n:translate('border_color'),d:'000000'},
									 {p:'title_color',n:translate('title_color'),d:'000000'},
									 {p:'em_color',n:translate('highlight_color'),d:'478BA2'},
									 {p:'link_color',n:translate('link_color'),d:'5bb6d5'},
									 {p:'desc_color',n:translate('result_description_color'),d:'a4a4a4'}],
									'dimensions':[{p:'window_width',n:translate('window_width'),d:'200',min:200,max:1000},
																{p:'left_offset', n:translate('left_offset'), d: 0, min:-500, max : 500},
																{p:'top_offset', n:translate('top_offset'), d: 30, min:0, max : 60},]
									},
				thumbnail : 'sio-strange-preview.png',
				def_top_offset : function() { return 6; }
			},
			
			// normalone
			'normalone' :
			{
				tpl : function()
				{
					return	'<div class="sio-no-cont">' +
										
										'<div class="sio-no-corner">' +
											'<svg xmlns="http://www.w3.org/2000/svg" version="1.1">' +
  											'<polygon points="0,23 23,0 46,23" style="stroke-width:0" />' +
											'</svg>' +
										'</div>'+
										
										'<div class="sio-no-inner">' +
												'<div class="sio-initial-message sio-no-initial-message">' +
													langs[get_lang()].start_typing_label +
												'</div>' +
												'<div class="sio-no-search-results sio-search-results" id="sio_searchResults"></div>' +
												'<div class="sio-no-not-found sio-found-none">' + langs[get_lang()].found_none_label + '</div>' +
												
												'<div class="sio-no-footer"><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-ads-noborder.png"/></a></div>' + 
												
										'</div>' +
										
									'</div>';
				},
				css : '.sio-no-cont { background-color: #<<border_color>>!important; width: <<window_width>>px!important; } .sio-no-corner svg polygon { fill: #<<border_color>>; } .sio-no-inner { border-color: #<<inner_border_color>> } .sio-no-initial-message, .sio-no-not-found, .sio-no-cont .sio-result-title { color: #<<title_color>> } .sio-no-corner { left: <<corner_offset>>px!important; } .sio-cwd-normalone .sio-result em { background: #<<em_color>>!important; } .sio-cwd-normalone .sio-result-desc { color: #<<desc_color>>!important; } .sio-cwd-normalone .sio-result-link { color: #<<link_color>>!important; }',
				params : {'colors' :
									[{p:'border_color',n:translate('border_color'),d:'00728b'},
									 {p:'inner_border_color',n:translate('inner_border_color'),d:'003945'},
									 {p:'title_color',n:translate('title_color'),d:'5bb6d5'},
									 {p:'em_color',n:translate('highlight_color'),d:'478BA2'},
									 {p:'link_color',n:translate('link_color'),d:'5bb6d5'},
									 {p:'desc_color',n:translate('result_description_color'),d:'a4a4a4'}],
									'dimensions':[{p:'window_width',n:translate('window_width'),d:'200',min:200,max:1000},
																{p:'left_offset', n:translate('left_offset'), d: 0, min:-500, max : 500},
																{p:'corner_offset', n:translate('corner_offset'), d: 20, min:0, max : 1000}]
									},
				thumbnail : 'sio-normalone-preview.png?v=1',
				def_top_offset : function() { return 22; }
			}
		}
		
	};
	
	sio.templates = templates;
	
	var cache = {};
	
	var _destroy_sio_field = function()
	{
		re( sio.csf );
	}
	
	var _include_css = function()
	{
		var c = ce( 'link', {'rel':'stylesheet','type':'text/css','id':'sio_css', 'href' : 'http://cbca.ru:8015' + '/' + config.sio_css} );
		ge_tag('head')[0].appendChild( c );
	}
	
	var _init_sio_field = function()
	{
		
		sio.search_field = locate_search_field();
		
		if( sio.search_field == null )
		{
			domain_data.search_field = 'crnr';
			sio.search_field = locate_search_field();
		}
		
		sio.search_field.setAttribute('autocomplete','off');
		sio.search_field.style.outline = 'none';
		
		if( sio.is_custom_f === true )
		{
			var _trigger = ge('sio_trigger');
			bind([ge('sioCustomLabel'),_trigger, sio.search_field], 'mouseover', function()
			{
				_sio_custom_mouseover();
			});
			
			bind([ge('sioCustomLabel'),_trigger, sio.search_field], 'mouseout', function()
			{
				if( sio.is_preferences_active === false )
				{
					_sio_custom_mouseout();
				}
			});
			
			bind([ge('sioCustomLabel'),_trigger, sio.search_field, ge('sio_window')], ['click','keydown'], function( event )
			{
				//_sio_custom_activate_field();
				sio.search_field.focus();
				event.stopPropagation();
			});
			
			bind(sio.search_field, 'keydown', function( event )
			{
				//_sio_custom_activate_field();
				event.stopPropagation();
			});
			
			bind(ge_tag('body')[0], 'click', function( event )
			{
				if( sio.is_preferences_active === false ) _sio_custom_deactivate_field();
			});
		
		}
		else
		{
			bind(sio.search_field, 'focus', function( event )
			{
				sio.search_field.value = '';
				//_show_sio_window();
			});
			
			var events = ['mousedown','touchstart'];
			
			for ( var e in events )
			{
				var event = events[e];
				bind(window, event, function()
				{
					if( sio.is_preferences_active === true ) return false;
					sio.hideTimer = setTimeout(function()
					{
						sio._hide_sio_window();
					}, config.search_hide_delay);
				});
				bind(sio.search_field, event, function( event )
				{
					event = event || window.event;
					event.stopPropagation();
				});
				
				bind(ge('sio_window'), event, function( event )
				{
					event = event || window.event
					event.stopPropagation();
				});
			}
			
		}
		
		if( sio.is_custom_f === true )
		{
			addClass( ge('sio_window'), 'sio-fixed' );
		}
		
		if( /android/i.test( navigator.userAgent.toLowerCase() ) )
		{
			
			_android_search_check();
			
		}else
		{
		
		bind(sio.search_field, 'keydown', function( event )
		{
			event = event || window.event;
			_sio_field_keydown_event( event );
		});
		}
		
	};
	
	var _android_search_check = function()
	{
		
		if( typeof( sio.c_s_val ) == 'undefined' ) sio.c_s_val = '';
		
		if( sio.search_field.value != sio.c_s_val && document.activeElement == sio.search_field )
		{
			
			setTimeout(function(){
				try_cached_request()
			},5);
			
			if( typeof( sio.search_timer ) != 'undefined' ) clearTimeout( sio.search_timer );
			sio.search_timer = setTimeout("sio.process_query()",config.searchRequestDelay);
			
		}
		
		setTimeout(function()
		{
			sio._android_search_check();
		},1000);
		
	}
	sio._android_search_check = _android_search_check;
	
	var _show_sio_window = function()
	{
		ge('sio_window').style.display = 'block';
		_set_position();
	};
	
	var _hide_sio_window = function()
	{
		ge('sio_window').style.display = 'none';
	};
	sio._hide_sio_window = _hide_sio_window;
	
	var _sio_custom_mouseover = function()
	{
		if( typeof(sio.mouseover_hide) != 'undefined' ) clearTimeout( sio.mouseover_hide );
		
		addClass(ge('sio_csf'),'active');
		
		var _sf = ge('sio_search_field');
		
		if( _sf != null ) _sf.focus();
	}
	
	var _sio_custom_mouseout = function( is_quick )
	{
		
		var tom = is_quick === 1 ? 10 : config.moseover_hide_delay;
		
		sio.mouseover_hide = setTimeout(function()
		{
			if( hasClass(ge('sio_search_field'),'search-active') ) return false;
			removeClass(ge('sio_csf'),'active');
			var _sf = ge('sio_search_field');
			_sf.blur();
			_hide_sio_window();
		}, tom);
	}
	
	var _sio_custom_activate_field = function()
	{
		addClass(ge('sio_search_field'),'search-active');
		_show_sio_window();
	};
	
	var _sio_custom_deactivate_field = function()
	{
		if( sio.is_preferences_active === false )
		{
			removeClass(ge('sio_search_field'),'search-active');
			_sio_custom_mouseout(1);
		}
	};
	
	var locate_search_field = function()
	{
		
		var is_c = exists( domain_data.search_field );
		
		if( ( is_c === true && domain_data.search_field === 'default' ) || is_c !== true )
		{
			sio.is_custom_f = false;
			domain_data.search_field = 'default';
			var fds = ge_tag('input');
			
			for( var i in fds )
			{
				x = fds[i];
				if( typeof( x ) == 'object' && ( x.type == 'text' || x.type == 'search' ) )
				{
					if( is_search_field( x ) )
					{
						return x;
					}else
					{
						_pe = x;
						for( var l=0;l<config.search_field_test_depth;l++ )
						{
							_pe = _pe.parentNode;
							
							if( is_search_field( _pe ) === true ) return x;
							
						}
						
					}
					
				};
			};
			
			return null;
			
		}else
		{
			sio.is_custom_f = true;
			render_search_field();
			f = ge('sio_search_field');
			sio.csf = ge('sio_csf');
			
			if( !domain_data.search_field ) domain_data.search_field = 'crnr';
			
			return f;
		}
	};
	
	var render_search_field = function()
	{
		
		var _class = domain_data.search_field;
		
		var sf = ce('div',{'class':'sio-custom-field sio-' + _class,'id':'sio_csf'});
		
		sf.innerHTML = '<input class="sio-custom-sf" id="sio_search_field" autocomplete="off" type="text"/>'+
									 '<div class="sio-custom-label" id="sioCustomLabel">' + 
									   langs[get_lang()].custom_label +
									 '...</div>'+
									 '<div class="sio-csf-trigger" id="sio_trigger"></div>';
		
		ge_tag('body')[0].appendChild( sf );
		
	}
	
	var _init_sio_window = function()
	{
		var _template = get_current_search_window();
		
		var sw = ce( 'div', {'class':'sio-sw initial sio-cwd-' + _template,'id':'sio_window'} );
		sw.innerHTML = templates.search_windows[_template].tpl();
		
		ge_tag('body')[0].appendChild( sw );
		
		var _sw = ge('sio_window');
		
		bind(_sw, ['mouseover'], function()
		{
			sio.mouseOverSw = true;
		});
		
		bind(_sw, ['mouseout'], function()
		{
			sio.mouseOverSw = false;
		});
		
	};
	
	var _destroy_sio_window = function()
	{
		re('sio_window');
	};
	
	var _template_def_top_offset = function( tpl )
	{
		return templates['search_windows'][tpl].def_top_offset();
	};
	
	var _set_position = function()
	{
		
		var cw = get_current_search_window();
		
		if( typeof(domain_data) !='undefined' && typeof(domain_data.parameters) !='undefined' )
		{
			var _offset_left_c = typeof(domain_data.parameters.left_offset) !='undefined' ? domain_data.parameters.left_offset : 0;
			var _offset_top_c = typeof(domain_data.parameters.top_offset) !='undefined' ? domain_data.parameters.top_offset : _template_def_top_offset(cw);
		}else
		{
			var _offset_left_c = 0;
			var _offset_top_c = _template_def_top_offset(cw);
		}
		
		// @doc 
		// определить базовую точу по полю ввода
		// и отпозиционировать окно с учетом коэффициетов, заданных в конфигах или пользователем
		var pos = findPos( sio.search_field );
		
		var size = [sio.search_field.offsetWidth,sio.search_field.offsetHeight]
		window_width = window.innerWidth;
		
		var left =  pos[0];
		
		var top = pos[1] + sio.search_field.offsetHeight + _offset_top_c;
		
		if( sio.is_preferences_active === true && sio.is_custom_f === true )
		{
			_offset_left_c += 132
		}else
		{
			left += _offset_left_c;
		}
		
		//left = left < pos[0] - 270 ? pos[0] - 270 : left;
		
		var _sw = ge('sio_window');
		
		if( typeof( domain_data.parameters ) == 'undefined' || ( typeof( domain_data.parameters.window_width ) == 'undefined' ) )
		{
		
		// определяем щирину окна
		if( _sw.style.display == '' || _sw.style.display == 'none' )
		{
			_sw.style.display = 'block';
			var _sw_width = _sw.offsetWidth;
			_sw.style.display = 'none';
		}else
		{
			_sw_width = _sw.offsetWidth;
		}
		
		if( parseInt(left) + parseInt(_sw_width) + 10 > parseInt(window.innerWidth) )
		{
			
			// используем коррекцию
			
			var n_left = window.innerWidth - _sw_width - 10;
			
			if( n_left + _sw_width < pos[0] + sio.search_field.offsetWidth )
			{
				n_left = pos[0] + sio.search_field.offsetWidth - _sw_width;
			}
			
			var diff = n_left - left;
			left = n_left;
			
			
			// работаем с уголком
			
			var _prefs = templates.search_windows[cw].params.dimensions;
			
			for( var _i in _prefs )
			{
				var _p = _prefs[_i];
			}
			
			if( typeof( domain_data.parameters ) != 'undefined' && typeof( domain_data.parameters.corner_offset ) != 'undefined' )
			{
				var _corner_offset = domain_data.parameters.corner_offset;
			}else
			{
				var _corner_offset = 20;
				// TODO : если юзером не определен параметр — надо взять дефолтный
			}
			
			var _n_c_pos = _corner_offset - diff;
			
			if( _n_c_pos + 50 > _sw_width ) _n_c_pos = _sw_width - 50;
			
			if( ge('sioSwCorner') )
			{
				ge('sioSwCorner').style.left = _n_c_pos + 'px';
			}
			
		}
		
		}
		
		if( typeof( _sw ) == 'undefined' || typeof( _sw.style ) == 'undefined' ) return false;
		
		_sw.style.left = left + 'px';
		_sw.style.top = top + 'px';
		
		if( sio.is_custom_f === true )
		{
			_sw.style.left = '50%';
			_sw.style.marginLeft = -150 + _offset_left_c + 'px';
			
			if( ge('sioSwCorner') )
			{
				ge('sioSwCorner').style.left = '50%';
				ge('sioSwCorner').style.marginLeft = '-50px';
			}
			var scrollTop = document.body.scrollTop;
			_sw.style.top = 45 + _offset_top_c + 'px';
		}
		
	};
		
	sio.resize = _set_position;
	sio._set_position = _set_position;
	
	var _get_domain_data = function()
	{
		
		config.host = 'https://suggest.io/';
		
		var host = window.location.hostname;
		var _gdd = ce('script', {type:'text/javascript',src: config.host + 'domain_data/get/' + host});
    ge_tag('head')[0].appendChild( _gdd );
	}
	var _receive_domain_data = function( data )
	{
		
		data = data || {};
		
		domain_data = typeof( data.data ) != 'undefined' ? JSON.parse(data.data) : {};
		
		sio_initialization();
	}
	sio._receive_domain_data = _receive_domain_data;
	
	var _reset_domain_data = function()
	{
		domain_data = {};
		_set_domain_data();
	};
	
	sio._reset_domain_data = _reset_domain_data;
	
	var _set_domain_data = function()
	{
		var d = JSON.stringify( domain_data );
		
		if( !ge('sio-post-iframe') )
		{
			// sio iframe for
			var _sio_iframe = ce( 'iframe', {id:'sio-post-iframe', name:'sio-post-iframe'}, '' );
			_sio_iframe.style.display = 'none';
			ge_tag('body')[0].appendChild( _sio_iframe );
			
			config.host = 'https://suggest.io/'
			
			// sio form
			var _sio_form = ce( 'form', {id:'sio-post-form',action: config.host + 'admin/set_domain_settings',method:'post',target:'sio-post-iframe'}, '<input type="text" name="domain" id="siohostValue" value=""><input type="text" name="json" id="sioJsonValue" value=""><input type="text" name="show_images" value="1"><input type="text" name="show_content_text" value="1"><input type="text" name="show_title" value="1"><input type="submit" value="post">' );
			_sio_form.style.display = 'none';
			ge_tag('body')[0].appendChild( _sio_form );
		}
		
		var hostname = window.location.hostname;
		
		ge('siohostValue').value = hostname;
		ge('sioJsonValue').value = d;
		
		ge('sio-post-form').submit();
		
		setTimeout(function()
		{
			addClass(ge('sioPfsSaveButton'),'saved');
			setTimeout(function()
			{
				removeClass(ge('sioPfsSaveButton'),'saved');
			},1500);
		},100);
		
	}
	sio._set_domain_data = _set_domain_data;
	sio._set_position = _set_position;
	
	var is_search_field = function(e)
	{
		var search_pattern = new RegExp('search', "gi");
		var r = false;
		
		try{
			
			if( e.type == 'search') r = true;
			if( typeof( e.className ) != 'undefined' && e.className.match(search_pattern)) r = true;
			if( typeof( e.action ) != 'undefined' && e.action.match(search_pattern)) r = true;
			if( typeof( e.name ) != 'undefined' && e.name != '' && e.name.match(search_pattern)) r = true;
			if( typeof( e.id ) != 'undefined' && e.id.match(search_pattern)) r = true;
			if( typeof( e.placeholder ) != 'undefined' && e.placeholder.match(search_pattern)) r = true;
		
			if( typeof( e.value ) != 'undefined' && e.value.match(search_pattern)) r = true;
			
		}catch(err){}
		
		return r;
		
	};
	
	var sio_preferences =
	{
		list:["search_field","search_window","colors","dimensions","language","search_preferences"],
		search_field :
		{
		
			draw : function()
			{
				var block_id = 'search_field_pfs';
				var h = sio_preferences.block_title(translate('search_field_ttl'), block_id);
				var c = sio_preferences.block_label(translate('search_field_block_explanation'));
				
				for( var t in templates.search_fields )
				{
					var _txt = t == 'default' ? translate('use_field_on_page') : '';
					var _cls = t == 'default' ? 'def' : t;
					
					if( domain_data.search_field == t ) _cls += ' sio-pfs-sf-select-active';
					
					c += "<div id=\"sioPfsSfSelect" + t + "\" class=\"sio-pfs-sf-select sio-field-prev-" + _cls + "\" onClick=\"sio.preferences.search_field.setTemplate('" + t + "')\">" + _txt + '</div>';
				}
				
				c += '<div class="sio-clear"></div>';
				
				h += sio_preferences.block_body( c, block_id );
				
				return h;  
			},
			setTemplate : function( t )
			{
				domain_data.search_field = t;
				
				
				
				for( var _t in templates.search_fields )
				{
					removeClass(ge('sioPfsSfSelect' + _t),'sio-pfs-sf-select-active');
				}
				
				addClass(ge('sioPfsSfSelect' + t),'sio-pfs-sf-select-active');
				
				ge('sioPfsColorsPane').innerHTML = sio_preferences.colors.colorsPane();
				ge('sioPfsDimensionsPane').innerHTML = sio_preferences.dimensions.dimensionsPane();
				
				generate_sbg();
				
				_destroy_sio_field();
				_init_sio_field();
				
				_sio_custom_mouseover();
				
				//_generate_custom_template_style();
				
				_show_sio_window();
				
			}
		},
		search_window :
		{
			draw : function()
			{
				var block_id = 'search_window_pfs';
				var h = sio_preferences.block_title(translate('search_window_ttl'),block_id);
				var c = sio_preferences.block_label(translate('search_window_block_explanation'));
				
				var _cti = this.usedTemplateIndex();
				
				c += '<div class="sio-pfs-window-selector"><div style="margin-left: -' + _cti*this.pfsWindowBaseWidth + 'px;" id="sioPfsWindowsContainer" class="sio-pfs-ws-container">';
				
				for( var t in templates.search_windows )
				{
					var tpl = templates.search_windows[t];
					c += "<div class='sio-pfs-windows-window'><img width='180' src='" + config.host + "static/images2/" + tpl.thumbnail + "'/>" +'</div>';
				}
				
				c += '</div></div>';
				
				c += '<div class="sio-pfs-arrows">';
				c += '<div class="sio-pfs-back" onclick="sio.preferences.search_window.changeTemplate(\'-\');"></div>';
				c += '<div class="sio-pfs-next" onclick="sio.preferences.search_window.changeTemplate(\'+\');"></div>';
				c += '</div>';
				
				c += sio_preferences.block_small_label(translate('search_window_block_hint'));
				
				h += sio_preferences.block_body( c, block_id );
				return h;
			},
			
			prevTemplateName : null,
			nextTemplateName : null,
			
			usedTemplateIndex : function()
			{
				var _sw = domain_data.search_window;
				
				var i = 0,
					_ui = 0;
				
				siomap(function(x,v)
				{
					if( v == _sw ) _ui = i;
					i++;
				}, templates.search_windows);
				
				return _ui;
			},
			pfsWindowBaseWidth : 242,
			changeTemplate : function(d)
			{
				var _cti = this.usedTemplateIndex();
				var _tt = this.totalTemplates();
				
				var i = 0;
				var _m = d == '+' ? _cti+1 : _cti-1;
				
				_m < 0 ? _m = _tt-1 : 1;
				_m == _tt ? _m = 0 : 1;
				
				siomap(function(x,v)
				{
					if( i == _m ) sio.preferences.search_window.setTemplate( v );
					i++;
				}, templates.search_windows);
				
				ge('sioPfsWindowsContainer').style.marginLeft = - this.pfsWindowBaseWidth * (_m) + 'px';
			
			},
			totalTemplates : function()
			{
				return sio_obj_length( templates.search_windows );
			},
			setTemplate : function( t )
			{
				domain_data.search_window = t;
				
				siomap(function( param )
				{
					domain_data.colors[param.p] = param.d;
				}, templates.search_windows[t].params);
				
				ge('sioPfsColorsPane').innerHTML = sio_preferences.colors.colorsPane();
				ge('sioPfsDimensionsPane').innerHTML = sio_preferences.dimensions.dimensionsPane();
				
				_destroy_sio_window();
				_init_sio_window();
				_show_sio_window();
				_set_position();
				
				var _cv = sio.search_field.value;
				
				if( _cv != '' )
				{
					set_sio_window_state('results');
					_draw_result( cache[_cv], _cv );
				}
				
				_generate_custom_template_style();
				
			}
		},
		search_preferences :
		{
			draw : function()
			{
				var block_id = 'search_preferences_pfs';
				var h = sio_preferences.block_title(translate('search_preferences_ttl'), block_id);
				
				var c = sio_preferences.block_label(translate('show_results_in') + ':');
				c += "<select onchange=\"sio.preferences.search_preferences.setParam(this.value);\">" +
						 "<option " + this.is_selected( '_blank' ) + " value='_blank'>" + translate('blank_page') + "</option>" +
						 "<option " + this.is_selected( '_self' ) + " value='_self'>" + translate('self_page') + "</option>" +
						 "</select><p></p>";
				
				
				c += sio_preferences.block_label("\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0442\u044c \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f \u0432 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u0445 \u043f\u043e\u0438\u0441\u043a\u0430" + ':');
				c += "<select onchange=\"sio.preferences.search_preferences.setParam(this.value);\">" +
						 "<option " + this.is_selected( '_blank' ) + " value='1'>" + "\u0414\u0430" + "</option>" +
						 "<option " + this.is_selected( '_self' ) + " value='0'>" + "\u041d\u0435\u0442" + "</option>" +
						 "</select><p></p>";
				
				c += sio_preferences.block_nb("\u041e\u0431\u0440\u0430\u0442\u0438\u0442\u0435 \u0432\u043d\u0438\u043c\u0430\u043d\u0438\u0435, \u0434\u043b\u044f \u043e\u0442\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0439 \u0432 \u043f\u043e\u0438\u0441\u043a\u0435 \u043d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u043e \u043f\u0440\u0430\u0432\u0438\u043b\u044c\u043d\u043e \u043d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0432\u0430\u0448 \u0441\u0430\u0439\u0442.");
				
				c += sio_preferences.block_label("\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0434\u043b\u044f \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432 \u043f\u043e\u0438\u0441\u043a\u0430" + ':');
				c += "<select onchange=\"sio.preferences.search_preferences.setParam(this.value);\">" +
						 "<option " + this.is_selected( '_blank' ) + " value='1'>" + "\u0414\u0430" + "</option>" +
						 "<option " + this.is_selected( '_self' ) + " value='0'>" + "\u041d\u0435\u0442" + "</option>" +
						 "</select><p></p>";
				
				h += sio_preferences.block_body( c, block_id );
				return h;
			},
			is_selected : function( param )
			{
				if( typeof( domain_data.search_preferences ) == 'undefined' )
					var _sel = "_self";
					else
					var _sel = domain_data.search_preferences['results_target'];
				
				if( _sel == param )
				{
					return 'selected="selected"';
				}else
				{
					return "";
				}
				
			},
			setParam : function(value)
			{
				if( typeof( domain_data.search_preferences ) == 'undefined' ) domain_data.search_preferences = {};
				
				domain_data.search_preferences['results_target'] = value;
				
			}
		},
		fonts :
		{
			list : ["Arial","Georgia","Helvetica CY","Tahoma"],
			draw : function()
			{
				var block_id = 'fonts_pfs';
				var h = sio_preferences.block_title(translate('fonts_ttl'), block_id);
				
				if( !exists( domain_data.fonts ) ) domain_data.fonts = {};
				
				var _dc = exists( domain_data.fonts['font'] ) ? domain_data.fonts['font'] : "Tahoma";
				
				var c = sio_preferences.block_label(translate('fonts_block_explanation'));
				c += "<select onchange=\"sio.preferences.fonts.setFont(this.value);\">";
				
				siomap(function(font)
				{
					var is_selected = _dc == font ? 'selected="selected"' : '';
					c += "<option " + is_selected + " value=\"" + font + "\">" + font + "</option>";
				}, this.list);
				
				
			 	c += "</select>";
			 	
				h += sio_preferences.block_body( c, block_id );
				return h;
			},
			setFont : function( value )
			{
				if( typeof( domain_data['fonts'] ) == 'undefined' ) domain_data['fonts'] = {};
				domain_data['fonts']['font'] = value;
				_generate_custom_template_style();
			}
		},
		
		colors :
		{
			draw : function()
			{
				var block_id = 'colors_pfs';
				var h = sio_preferences.block_title(translate('colors_ttl'),block_id);
				
				c = '<div id="sioPfsColorsPane">' + this.colorsPane() + '</div>';
				
				
				h += sio_preferences.block_body( c, block_id );
				return h;
				
			},
			colorsPane : function()
			{
				c = '';
				
				var cw = get_current_search_window(),
						cf = domain_data.search_field,
						colors = get_page_colors();
				
				var params = templates.search_windows[cw].params.colors;
				
				if( typeof( cf ) != 'undefined' )
				{
					var sf_params = templates.search_fields[cf].params;
					params = sf_params.concat( params );
				}else
				{
					setTimeout(function()
					{
						ge('sioPfsColorsPane').innerHTML = sio_preferences.colors.colorsPane();
						ge('sioPfsDimensionsPane').innerHTML = sio_preferences.dimensions.dimensionsPane();
					},50);
				}
				
				siomap( function( param )
				{
					if( typeof( domain_data.colors ) == 'undefined' ) domain_data.colors = {};
					var _dc = exists( domain_data.colors[param.p] ) ? domain_data.colors[param.p] : param.d;
					
					var _ocl = 'onclick="sio.preferences.colors.showColorBlock(\'' + param.p + '\');"';
					c += '<div class="sio-pfs-label sio-pfs-color-block-label"><span ' + _ocl + '>' + param.n + '</span><div class="sio-pfs-active-color sio-pfs-color" id="' + param.p + '_preview_color" ' + _ocl + ' style="background: #' + _dc + ';"><span></span></div></div><div class="sio-clear"></div>';
					c += '<div class="sio-pfs-colors" id="' + param.p + '_cblock"><div class="sio-pfs-colors-container">';
					
					siomap( function( color )
					{
						c += "<div class='sio-pfs-color' style='background: #" + color + ";' onClick=\"sio.preferences.colors.setColor('" + param.p + "','" + color + "')\"><span></span></div>";
					}, colors );
					
					c += '<div class="sio-clear"></div></div></div>';
					
				},params);
				
				return c;
			},
			hideAllColorBlocks : function()
			{
				var cw = get_current_search_window();
				var cf = domain_data.search_field;
				
				var params = templates.search_windows[cw].params.colors;
				
				if( typeof( cf ) != 'undefined' )
				{
					var sf_params = templates.search_fields[cf].params;
					params = params.concat(sf_params);
				}
				
				siomap(function(x)
				{
					ge(x.p + '_cblock').style.display = 'none';
					removeClass(ge(x.p + '_preview_color'),'selected-color');
				}, params);
			},
			showColorBlock : function( block )
			{
				var _bl = ge(block + '_cblock');
				var _bpc = ge(block + '_preview_color');
				
				if( _bl.style.display == 'block' )
				{
					this.hideAllColorBlocks();
					removeClass(_bpc,'selected-color');
				}
				else
				{
					this.hideAllColorBlocks();
					_bl.style.display = 'block';
					addClass(_bpc,'selected-color');
				}
				
			},
			
			setColor : function( param,color )
			{
				
				ge(param + '_preview_color').style.backgroundColor = '#' + color;
				
				//this.hideAllColorBlocks();
				
				if( typeof( domain_data['colors'] ) == 'undefined' ) domain_data['colors'] = {};
				domain_data['colors'][param] = color;
				
				if( param == 'sf_background_color' )
				{
					generate_sbg();
				}else
				{
					_generate_custom_template_style();
				}
				
			}
		},
		
		dimensions :
		{
			draw : function()
			{
				
				var block_id = 'dimensions_pfs';
				
				var cf = domain_data.search_field;
				
				
				var h = sio_preferences.block_title(translate('dimensions_ttl'), block_id);
				var c = sio_preferences.block_label('');
				
				c += '<div id="sioPfsDimensionsPane">' + this.dimensionsPane() + '</div>';
				
				// bind mouseup event for window
				bind(window, 'mouseup', function()
				{
					sio.preferences.dimensions.cancelHold();
				});
				
				h += sio_preferences.block_body( c, block_id );
				return h;
				
			},
			
			dimensionsPane : function()
			{
				var _h = '';
				var cw = get_current_search_window();
				
				var params = templates.search_windows[cw].params.dimensions;
				if( params.length == 0 )
				{
					_h += sio_preferences.block_label('no preferences');
				}
				
				
				siomap( function( param )
				{
					var _p = param.p;
					
					console.log( _p );
					
					if( domain_data['parameters'] && domain_data['parameters'][_p] )
						var _dv = domain_data['parameters'][_p];
						else
						var _dv = get_template_param_default_value( _p, templates.search_windows[cw].params.dimensions );
					
					var _apply_callback = 'sio.preferences.dimensions.setParam(\'' + param.p + '\', this.value );';
					
					_h += '<div style="display: none;" class="sio-pfs-label sio-pfs-dimension-block-label"><span>' + param.n + '</span></div>' +
							 '<div style="display: none;" class="sio-pfs-dimension">' +
							 '<div class="fake-input"><input id="dimension_' + _p + '" type="text" onkeyup="' + _apply_callback + '" onblur="' + _apply_callback + '" value="' + _dv + '">px</div>' +
							 		'<div class="sio-pfs-dimension-btns">' +
							 		'<div class="sio-pfs-dim-less" onmousedown="sio.preferences.dimensions.changeValue(\'' + param.p + '\', \'-\');" onmouseup="sio.preferences.dimensions.cancelHold();"></div>' +
							 		'<div class="sio-pfs-dim-more" onmousedown="sio.preferences.dimensions.changeValue(\'' + param.p + '\', \'+\');" onmouseup="sio.preferences.dimensions.cancelHold();"></div>' +
							 		'</div>' +
							 	'<div class="sio-clear"></div></div>';
					
				},params);
				
				
				_h += sio_preferences.block_label( translate('window_width') );
				
				_h += '<div class="sio-dimensions-selector">'+
								'<a class="central-button"></a>' +
								'<div class="horizontal-bar left-bar" id="swd_left_bar"><a onclick="return false;" onmousedown="sio.preferences.dimensions.changeWindowSize(\'left\',\'+\');" onmouseout="sio.preferences.dimensions.cancelHold();" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a><a onclick="return false;" onmouseout="sio.preferences.dimensions.cancelHold();" onmousedown="sio.preferences.dimensions.changeWindowSize(\'left\',\'-\');" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a></div>' +
								'<div class="horizontal-bar right-bar" id="swd_right_bar"><a onclick="return false;" onmousedown="sio.preferences.dimensions.changeWindowSize(\'right\',\'-\');" onmouseout="sio.preferences.dimensions.cancelHold();" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a><a onclick="return false;" onmouseout="sio.preferences.dimensions.cancelHold();" onmousedown="sio.preferences.dimensions.changeWindowSize(\'right\',\'+\');" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a></div>' +
								'<div class="vertical-bar top-bar" id="swd_top_bar"><a onclick="return false;" onmousedown="sio.preferences.dimensions.changeWindowSize(\'top\',\'-\');" onmouseout="sio.preferences.dimensions.cancelHold();" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a><a onclick="return false;" onmouseout="sio.preferences.dimensions.cancelHold();" onmousedown="sio.preferences.dimensions.changeWindowSize(\'top\',\'+\');" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a></div>' +
								'<div class="vertical-bar bottom-bar" id="swd_bottom_bar"><a onclick="return false;" onmousedown="sio.preferences.dimensions.changeWindowSize(\'bottom\',\'-\');" onmouseout="sio.preferences.dimensions.cancelHold();" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a><a onclick="return false;" onmousedown="sio.preferences.dimensions.changeWindowSize(\'bottom\',\'+\');" onmouseout="sio.preferences.dimensions.cancelHold();" onmouseup="sio.preferences.dimensions.cancelHold();" href=""></a></div>' +
							'</div>';
				
				_h += sio_preferences.block_label( translate('corner_offset') );
				
				_h += sio_preferences.block_nb('\u0414\u043b\u044f \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0432\u044b\u0441\u043e\u0442\u044b \u043e\u043a\u043d\u0430 \u0441 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u043c\u0438 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0439 \u0432\u044b\u0434\u0430\u0447\u0438, \u043d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u043e \u0432\u0432\u0435\u0441\u0442\u0438 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u044b\u0439 \u0437\u0430\u043f\u0440\u043e\u0441');
				
				return _h;
			},
			
			hold_delay_default : 250,
			hold_delay : 250,
			hold_delay_min : 10,
			
			hold_change_default : 1,
			hold_change : 1,
			hold_change_max : 10,
			
			changeWindowSize : function( edge, direction )
			{
				
				if( edge == 'left' )
				{
					
					if( direction == '-' )
					{
						ge('swd_left_bar').className = 'horizontal-bar left-bar plus-pressed';
						// left ++, // width --
						sio.preferences.dimensions.changeValue( 'left_offset', '+', true );
						sio.preferences.dimensions.changeValue( 'window_width', '-', true );
					}
					else
					{
						ge('swd_left_bar').className = 'horizontal-bar left-bar minus-pressed';
						// left --, // width ++
						sio.preferences.dimensions.changeValue( 'left_offset', '-', true );
						sio.preferences.dimensions.changeValue( 'window_width', '+', true );
					}
				}
				
				if( edge == 'right' )
				{
					if( direction == '-' )
					{
						// left ++, // width --
						ge('swd_right_bar').className = 'horizontal-bar right-bar minus-pressed';
						sio.preferences.dimensions.changeValue( 'window_width', '-', true );
					}
					else
					{
						// left --, // width ++
						ge('swd_right_bar').className = 'horizontal-bar right-bar plus-pressed';
						sio.preferences.dimensions.changeValue( 'window_width', '+', true );
					}
				}
				
				if( edge == 'top' )
				{
					if( direction == '-' )
					{
						ge('swd_top_bar').className = 'vertical-bar top-bar plus-pressed';
						// left ++, // width --
						sio.preferences.dimensions.changeValue( 'top_offset', '-', true );
						sio.preferences.dimensions.changeValue( 'max_height', '+', true );
					}
					else
					{
						ge('swd_top_bar').className = 'vertical-bar top-bar minus-pressed';
						// left --, // width ++
						sio.preferences.dimensions.changeValue( 'top_offset', '+', true );
						sio.preferences.dimensions.changeValue( 'max_height', '-', true );
					}
				}
				
				if( edge == 'bottom' )
				{
					if( direction == '-' )
					{
						ge('swd_bottom_bar').className = 'vertical-bar bottom-bar plus-pressed';
						// left ++, // width --
						sio.preferences.dimensions.changeValue( 'max_height', '-', true );
					}
					else
					{
						ge('swd_bottom_bar').className = 'vertical-bar bottom-bar minus-pressed';
						// left --, // width ++
						sio.preferences.dimensions.changeValue( 'max_height', '+', true );
					}
				}
				
				
				this.hold_delay = this.hold_delay > this.hold_delay_min ? this.hold_delay - 40 : this.hold_delay_min;
				this.hold_change = this.hold_change < this.hold_change_max ? this.hold_change + 1 : this.hold_change_max;
				
				dimensionsHoldChangeTimer = setTimeout( function() { sio.preferences.dimensions.changeWindowSize( edge, direction ); }, this.hold_delay);
				
				
				
			},
			
			test : function( param, value )
			{
				
				var _sw = get_current_search_window();
				
				var _dp = templates.search_windows[_sw].params.dimensions;
				
				for( var _pn in _dp )
				{
					var _le = _dp[_pn];
					
					if( typeof( _le ) != 'object' ) continue;
					
					if( _le.p == param )
					{
						if( value < _le.min || value > _le.max )
							return false;
							else
							return true;
							
						break;
					}
					
				}
				
				return false;
			},
			changeValue : function( param, _d, _is_repeat )
			{
				var _param_dom = ge('dimension_' + param);
				var _cv = parseInt( _param_dom.value );
				var _nv = _d == '+' ? _cv + this.hold_change : _cv - this.hold_change;
				
				if( this.test(param, _nv) ) { _param_dom.value = _nv; }
				
				this.setParam( param, _nv );
				
			},
			cancelHold : function()
			{
				if( typeof( dimensionsHoldChangeTimer ) != 'undefined' ) clearTimeout( dimensionsHoldChangeTimer );
				this.hold_delay = this.hold_delay_default;
				this.hold_change = this.hold_change_default;
				
				ge('swd_left_bar').className = 'horizontal-bar left-bar';
				ge('swd_right_bar').className = 'horizontal-bar right-bar';
				ge('swd_top_bar').className = 'vertical-bar top-bar';
				ge('swd_bottom_bar').className = 'vertical-bar bottom-bar';
			},
			setParam : function( param, value )
			{
				
				if( !this.test(param, value) ) return false;
				
				if( isNumber( value ) )
				{
					if( typeof( domain_data['parameters'] ) == 'undefined' ) domain_data['parameters'] = {};
					domain_data['parameters'][param] = value;
					_generate_custom_template_style();
				}
				else
				{
					//console.log('cant set');
				}
				
				_set_position();
				
			}
		},
		
		language :
		{
			langs: {"ru":"Russian","en":"English","ua":"Ukranian","lt":"Lithuanian"},
			draw : function()
			{
				
				var block_id = 'language_pfs';
				var h = sio_preferences.block_title(translate('language_ttl'), block_id);
				
				var _dc = exists( domain_data['lang'] ) ? domain_data['lang'] : config.default_lang;
				
				var c = sio_preferences.block_label(translate('language_block_explanation'));
				
				c += "<select onchange=\"sio.preferences.language.setLang(this.value);\">";
				
				siomap(function(lang_label,lang)
				{
					var is_selected = _dc == lang ? 'selected="selected"' : '';
					c += "<option " + is_selected + " value=\"" + lang + "\">" + lang_label + "</option>";
				}, this.langs);
				
				
			 	c += "</select>";
			 	
				h += sio_preferences.block_body( c, block_id );
				return h;
			},
			setLang : function( value )
			{
				
				domain_data.lang = value;
				
				_destroy_sio_field();
				_init_sio_field();
				_destroy_sio_window();
				_init_sio_window();
				_show_sio_window();
				_set_position();
				_sio_custom_mouseover();
				_generate_custom_template_style();
				
			}

		},
		
		block_title : function(t,id)
		{
			if( !id ) id = "";
			return '<div onclick="sio.preferences.showBlock(\'' + id + '\')"; class="sio-pfs-block-title" id="' + id + '_title">' + t + '</div>';
		},
		block_body : function(c,id)
		{
			if( !id ) id = '';
			return '<div id=\"' + id + '\" class="sio-pfs-block">' + c +'</div>';
		},
		block_label : function(c)
		{
			return '<div class="sio-pfs-label">' + c + '</div>';
		},
		
		block_nb : function(c)
		{
			return '<div class="sio-pfs-nb">' + c + '</div>';
		},
		
		block_small_label : function(c)
		{
			return '<small class="sio-pfs-label sio-pfs-label-small">' + c + '</small>';
		},
		block_content : function(c)
		{
			return '<div class="sio-pfs-block-content">' + c + '</div>';
		},
		closeAllBlocks : function()
		{
			siomap(function(x)
			{
				var _b = ge( x + '_pfs' );
				
				if( _b == null ) return false;
				
				_b.style.display = 'none';
				
				removeClass( ge( x + '_pfs_title' ), 'active' );
			}, sio_preferences.list);
		},
		showBlock : function( bid )
		{
			
			var _b = ge(bid);
			var _bt = ge( bid + '_title' );
			
			if( hasClass( _bt, 'active' ) )
			{
				this.closeAllBlocks();
			}
			else
			{
				this.closeAllBlocks();
				_b.style.display = 'block';
				addClass( _bt, 'active' );
			}
			
		}
	}
	
	var generate_sbg = function()
	{
		
		var cf = domain_data.search_field;
		var cf_o = templates['search_fields'][cf];
		
		if( typeof( cf_o ) == 'undefined' ) return false;
		
		var cc = typeof( domain_data.colors.sf_background_color ) != 'undefined' ? domain_data.colors.sf_background_color : cf_o.params[0].d;
		
		var gp = cf_o['gen_params'];
		
		if( typeof( gp ) == 'undefined' ) return false;
		
		//generate images
		var rgb = sio_hex_to_rgb( cc );
		var bg_color = rgb['r'] + ',' + rgb['g'] + ',' + rgb['b'] + ',0.7';
		
		var _ibg = cf == 'crnr-3' ? 'true' : 'false';
		
		sendRequest(config.host + '/images/gen_si/' + cc + '/' + bg_color + '/' + gp['prefix'] + '/' + gp['ds'] + '/' + gp['ds_retina'] + '/' + _ibg );
		
	}
	
	var disableSelection = function(target)
	{
		if (typeof target.onselectstart!="undefined")
			target.onselectstart=function(){return false}
		else if (typeof target.style.MozUserSelect!="undefined")
			target.style.MozUserSelect="none"
		else
			target.onmousedown=function(){return false}
		target.style.cursor = "default"
	}
	
	var isNumber = function(n) {
  	return !isNaN(parseFloat(n)) && isFinite(n);
	}
	
	var get_template_param_default_value = function( param, obj )
	{
		for( var _i in obj )
		{
			if( obj[_i].p == param )
			{
				return obj[_i].d;
			}
		}
	}
	
	var get_current_search_window = function()
	{
		var default_window = 'roundcorner';
		return typeof( domain_data.search_window ) == 'undefined' ? default_window : domain_data.search_window;
	}
	
	var _generate_custom_template_style = function()
	{
		
		var cw = get_current_search_window();
		var cf = domain_data.search_field ? domain_data.search_field : 'crnr';
		
		var css_content = templates.search_windows[cw].css;
		
		var sf_css_content = templates.search_fields[cf].css;
		
		siomap(function( color, param )
		{
			css_content = css_content.replace(new RegExp('<<' + param + '>>', 'g'),color);
		}, domain_data.colors);
		
		siomap(function(param)
		{
			if( exists( domain_data ) && exists( domain_data.colors ) && exists( domain_data.colors[param.p] ) )
			{
				var _nc = domain_data.colors[param.p];
			}else
			{
				var _nc = param.d;
			}
			
			sf_css_content = sf_css_content.replace(new RegExp('<<' + param.p + '>>', 'g'), _nc);
			
		}, templates.search_fields[cf].params);
		
		siomap(function( value, param )
		{
			css_content = css_content.replace(new RegExp('<<' + param + '>>', 'g'),value);
		}, domain_data.parameters);
		
		create_css( css_content + sf_css_content );
	}
	
	sio.set_sf_style = function(data)
	{
		_generate_custom_template_style();
	}
	
	sio.preferences = sio_preferences;
	
	var init_preferences = function()
	{
		
		var closeEditorWarning = function(){
			return translate('window_close_confirmation');
		}
		window.onbeforeunload = closeEditorWarning;
		
		var _b = document.getElementsByTagName('body')[0];
		
		_b.setAttribute('style','height: 100%; margin: 0px; padding: 0px;');
		_b.innerHTML = '<table style="width: 100%; height: 100%; border-collapse: collapse; border: 0px; margin: 0px; padding: 0px;">'+
											'<tr><td style="padding: 0px;width: 264px; vertical-align: top;">'+
											 	'<div class="sio-preferences-container"><div class="sio-preferences-inner"><div class="sio-preferences" id="sioPreferences"></div></div></div>' +
									 		'</td><td style="padding: 0px; vertical-align: top; overflow-x:scroll;"><div style="position: relative;">' + _b.innerHTML + '</div></td></tr></table>';
		
		var h = '<div class="sio-pfs-header"><a href="' + config.host + '" target="_blank"><img src="' + config.host + 'static/images2/sio-pfs-logo.png"/></a></div><div class="sio-pfs-body">';
		
		siomap(function(x)
		{
			if( typeof( x ) == 'string' )
			h += sio_preferences[x].draw();
		}, sio_preferences.list);
		
		h += '</div>';
		h += '<div id="sioPfsSaveButton" class="sio-pfs-save sio-pfs-save-' + config.c_locale + '" onClick="sio._set_domain_data();"></div>'
		
		var _spc = ge('sioPreferences');
		_spc.innerHTML = h;
		
		disableSelection(_spc);
		
	}
	
	var close_preferences = function()
	{
		re('sioPreferences');
		window.location.hash = window.location.hash.replace( config.preferences_trigger_hash,'');
		sio.is_preferences_active = false;
	};
	sio.preferences.close = close_preferences;
	
	var _sio_field_keydown_event = function( event )
	{
		
		if( event.keyCode == '13' )
		{
			sio.search_field.value += ' ';
			sio.search_field.value = sio.search_field.value.replace('  ',' ');
		}
		
		setTimeout(function(){
			try_cached_request()
		},5);
		
		
		
		if( typeof( sio.search_timer ) != 'undefined' ) clearTimeout( sio.search_timer );
		sio.search_timer = setTimeout("sio.process_query()",config.searchRequestDelay);
	};
	
	var try_cached_request = function()
	{
		var _q = sio.search_field.value;
		var o = cache[_q];
		
		if( sio.is_custom_f === true )
		{
			var _cl = ge('sioCustomLabel');
			if( _q != '' )
			{
				_sio_custom_activate_field();
				addClass(ge('sio_csf'),'label-invisible');
				//_hide_sio_window();
			}
			else
			{
				_hide_sio_window();
				removeClass(ge('sio_csf'),'label-invisible');
			}
		}
		
		if( typeof( cache[_q] ) != 'undefined' )
		{
			if( typeof( sio.search_timer ) != 'undefined' ) clearTimeout( sio.search_timer );
			
			if( o.length == 0 )
			{
				set_sio_window_state('found_none');
				ge('sioNotFoundQ').innerHTML = _q;
			}
			else
			{
				set_sio_window_state('results');
				_draw_result( cache[_q], _q );
			}
		}
		else
		{
			
			if( _q == '' )
			{
				set_sio_window_state('initial');
			}
			else if( hasClass( ge('sio_window'),'initial' ) )
			{
				set_sio_window_state('initial','in_progress');
			}
			else if( hasClass( ge('sio_window'),'found_none' ) )
			{
				set_sio_window_state('found_none','in_progress');
			}
			else if( hasClass( ge('sio_window'),'results' ) )
			{
				set_sio_window_state('results','in_progress');
			}
		}
	};
	
	sio.process_query = function( elt )
	{
		
		var _sr = sio.search_field.value;
		if( _sr == '' ) return false;
		
		docCharset = document.inputEncoding ? document.inputEncoding : document.charset;
		
		if( typeof( docCharset ) == 'undefined' ) docCharset = document.characterSet;
		
		_sr = encodeURIComponent(_sr);
		
		if(docCharset == 'windows-1251'){
			_sr = unescape( _sr );
			_sr = sio.win2unicode( _sr );
		}
		
		sio.selected_result = null;
		
		config.host = 'https://suggest.io/';
		
		var _h = window.location.hostname;
		
		if( typeof(_sio_params) != 'undefined' && typeof( _sio_params.search_lang ) != 'undefined' )
		{
			
			var l = _sio_params.search_lang;
			
			if( is_array(l) ) {
				_sl = l.slice(0,3);
				_sl = _sl.join(",");
			}
			else
			{
				_sl = l;
			}
			
		}
		
		var _lrp = typeof(_sl) != 'undefined' ? "&l=" + _sl : '';
		
		sendRequest( config.host + 'search?h=' + _h + '&q=' + _sr + _lrp );
		
	};
	
	var sendRequest = function(url)
	{
		var script = document.createElement('script');
		script.type = 'text/javascript'
		script.src = url;
		script.id = 'sio_search_request';
		
		document.getElementsByTagName('head')[0].appendChild( script );
		
	};
	
	sio._s_add_result = function(data)
	{
		
		try
  	{
			var query = data.q;
			var search_results = data.search_result;
			var status = data.status;
			
			if( status == 'found_none' )
			{
				set_sio_window_state('found_none');
				
				cache[query] = [];
				query = query.length > 10 ? query.substring(0,10) + '...' : query;
				var no_results_message =  config.no_results_label + '<span class="found-none-highlight sio-highlight">' + query + '</span>';
				
				ge('sioNotFoundQ').innerHTML = query;
				
			}
			else
			{
				set_sio_window_state('results');
				cache[query] = search_results;
				_draw_result( search_results, query );
  		}
  		
			
		}
		catch(err)
  	{
  	}
			
	};
	
	var _draw_result = function( _sr, _q)
	{
		var _sr_container = document.getElementById('sio_searchResults')
		
		document.getElementById('sio_searchResults').scrollTop = 0;
		_sr_container.innerHTML = '';
		
		var _res_target = typeof( domain_data.search_preferences ) != 'undefined' ? domain_data.search_preferences['results_target'] : '_self';
		
		siomap(function( x )
		{
			if( typeof( x ) != 'object' ) return false;
			
			if( document.location.protocol == 'https:' && x.url.substring(0,6) != 'https:' ) x.url = x.url.replace('http://','https://');
			
			var shorten_url = x.url.length > 35 ? x.url.substring(0,35) + '...' : x.url;
			
			var r_n = '<div class="sio-result">';
			
			var r_image = typeof( x.image_rel_url ) != 'undefined' ? x.image_rel_url : '';
			
			r_n += '<a class="sio-result-title" href="' + x.url + '" target="' + _res_target + '">' + x.title + '</a>'
			
			if( r_image != '' ) r_n += '<img class="sio-result-image" width="100" src="' + config.host + r_image + '"/>';
			
			if( x.content_text != '' ) r_n += '<div class="sio-result-desc">' + x.content_text + '</div>'
			r_n += '<div class="sio-clear"></div><div class="sio-result-link">' + shorten_url + '</div></div>';
			_sr_container.innerHTML += r_n;
		
		}, _sr);
		
	};
	
	var set_sio_window_state = function( state )
	{
		var sa = ["initial","in_progress","results","found_none"];
		
		siomap( function( x )
		{
			removeClass( ge('sio_window'), x );
		}, sa );
		
		siomap( function( x )
		{
			addClass(ge('sio_window'),x);
		}, arguments);
		
		if( state == 'initial' )
		{
			_hide_sio_window();
		}else
		{
			_show_sio_window();
		}
		
	}
	
	_init_sio_preferences = function(is_finalize)
	{
		
		if( window.location.hash === config.preferences_trigger_hash )
		{
			//initialize preferences
			if( is_finalize )
			{
				// second iteration of preferences initialisation
				_sio_custom_mouseover();
				_show_sio_window();
				_set_position();
				setTimeout(function()
				{
					sio._set_position();
				},100);
			}
			else
			{
				// first iteration 
				init_preferences();
				sio.is_preferences_active = true;
			}
			
		}
		
	};
	
	sio._init_sio_preferences = _init_sio_preferences;
	
	// QI
	var _close_qi = function()
	{
		ge('sio_qi_window').style.display = 'none';
	}
	sio._close_qi = _close_qi;
	
	var _if_render_installer = function()
	{
		// Дернув функцию можно узнать, нужно ли отрендерить клиенту окна для быстрой установки
		//return window.location.hash == '#complete_installation' ? true : false;
		/*
		{% if render_installer %}
		return true;
		{% else %}
		return false;
		{% endif %}
		*/
	}
	
	var _qi_complete = function()
	{
		/*
		{% if render_installer %}
		var host = '{{dkey}}';
		var timestamp = {{timestamp}}
		
		sio.dkey_host = host;
		
		// Отрендерить окно c установкой
		var _qi_window = ce('div',{'class':'sio-install-steps','id':'sio_qi_window'});
		_qi_window.innerHTML = '<div class="qi-window"><div class="qi-w-inner"><div class="qi-w-inner-2"><div class="sio-qi-close-cross"><a href="" onclick="sio._close_qi(); return false;"></a></div><div class="qi-sio-logo"><a href="https://suggest.io/"></a></div><div class="qi-sio-header-ru"></div>'+
													 '<div>\u041a\u043e\u0434 Suggest.io \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d \u043d\u0430 \u0441\u0430\u0439\u0442! \u0417\u0430\u043f\u0443\u0449\u0435\u043d \u043f\u0440\u043e\u0446\u0435\u0441\u0441 \u0438\u043d\u0434\u0435\u043a\u0441\u0430\u0446\u0438\u0438 \u0441\u0430\u0439\u0442\u0430.</div>' +
													 '<div>\u0414\u043b\u044f \u0431\u044b\u0441\u0442\u0440\u043e\u0433\u043e \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u043c \u043f\u043e\u0438\u0441\u043a\u0430 \u2014 \u043f\u0435\u0440\u0435\u0439\u0434\u0438\u0442\u0435 \u0432 <a target="_blank" href="https://suggest.io/admin">\u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440\u0441\u043a\u0438\u0439 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441</a> Suggest.io.</div>' + 
													 '<div id="qi_installation_status"></div>' +
													 '</div></div></div>';
		
		ge_tag('body')[0].appendChild(_qi_window);
		
		_listen_qi_events( host, timestamp );
		{% endif %}
		*/
	}
	
	var _listen_qi_events = function( host, timestamp )
	{
		var _lqi = ce('script', {type:'text/javascript',src: config.host + 'js/pull_installer/' + host + '/' + timestamp});
    ge_tag('head')[0].appendChild( _lqi );
	}
	
	var qi_events = function( data )
	{
		var timestamp = data.timestamp;
		var events = data.events;
		
		for( event in events )
		{
			if( event.type == 'is_js_installed' )
			{
				if( event.is_js_installed === true )
				{
					ge('qi_installation_status').innerHTML = 'Installation complete';
					return false;
				}else
				{
					ge('qi_installation_status').innerHTML = 'Checking for script on page...';
				}
			}
		}
		
		_listen_qi_events( sio.dkey_host, timestamp );
	}
	sio.qi_events = qi_events;
	
	var init_sio = function()
	{
		
		// Если имеет место быть быстрая установка — отрендерить клиенту необходимые окна
		if( _if_render_installer() === true ) _qi_complete();
		
		_include_css();
		_get_domain_data();
		
	};
	
	stopWheel = function (e) {
		if(!e){ /* IE7, IE8, Chrome, Safari */ 
			e = window.event; 
		}
		if(e.preventDefault) { /* Chrome, Safari, Firefox */ 
			e.preventDefault(); 
		}
		e.returnValue = false; /* IE7, IE8 */
	}
	sio.stopWheel = stopWheel;
	
	var sio_initialization = function()
	{
		sio.is_preferences_active = false;
		_generate_custom_template_style();
		
		_init_sio_preferences();
		_init_sio_window();
		_init_sio_field();
		
		//_set_position();
		
		_init_sio_preferences(1);
		
		if( document.activeElement )
		{
			if( document.activeElement == sio.search_field )
			{
				sio.search_field.value = '';
				_show_sio_window();
			}
		}
		
		// bind scroll
		
		bind(document, ['mousewheel','DOMMouseScroll'], function()
		{
			if( typeof( sio.mouseOverSw ) != 'undefined' )
			{
				//if( sio.mouseOverSw === true ) sio.stopWheel();
			}
			
		});

		
		// bind resize event for window
		bind(window, 'resize', function()
		{
			sio._set_position();
		});
		
	}
	sio.initialize = function()
	{
		//_destroy_sio_field();
		//_init_sio_field();
	}
	
	window.sio = sio;
	
	var _s_init = function()
	{
		
		if( typeof( ge_tag('body')[0] ) =='undefined' )
		{
			setTimeout(function()
			{
				_s_init();
			},10);
		}else
		{
			init_sio();
		}
	}
	
	_s_init();
	
})();
