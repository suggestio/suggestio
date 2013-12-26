{#/* sio renderer v2

Шаблон для скриптоты. Обычно выполняется без переменных и кешируется на клиентах.
если происходит добавление домена в базу suggest.io

Доступны переменные:
    - dkey. Например "vasya.ru"
    - timestamp для GET /js/pull/vasya.ru/123123123123

TODO:
    - отрабатывать ситуацию, когда юзер добавил вызов скрипта на страницу дважды
    - -//- когда юзер добавил вызов скрипта в <head>
    - не отображать настройки если юзер зашел с мобилы
*/
#}
(function() {
	
	var sio = {};
	
	/* Сгенерить рандомное целое число */
	var rand = function()
	{
		return Math.floor( Math.random() * 10000000 );
	}
	
	/* Настроечки */
	var config = 
	{
		//sio_host : 'https://suggest.io/',
		sio_host : 'https://suggest.io/',
		sio_css : 'static/css/sio.v8.css',// + rand(),
		host : window.location,
		searchRequestDelay : 100,
		lang : 'ru',
		search_field_test_depth : 3
	};
	
	if( window.location.hash == '#debug' )
	{
		config.sio_host = 'http://localhost:8003/';
		config.host = 'http://localhost:8003/';
	}
	
	/* Полезные функции */
	/* Создать DOM элемент */
	var ce = function ( tag, attributes, inhtml )
	{
		var ne = document.createElement( tag );
		for( var attr in attributes )
			ne.setAttribute(attr, attributes[attr]);
		
		if( typeof( inhtml ) != 'undefined' ) ne.innerHTML = inhtml;
		
		return ne;
	};
	
	/* Удалить DOM элемент */
	var re = function( e )
	{
		e = typeof( e ) == 'string' ? ge(e) : e;
		
		if( !exists( e ) || e == null ) return false;
		var p = e.parentNode
		
		if( p != null ) p.removeChild(e);
	};
	
	/* Проверить, есть ли объект */
	var exists = function(o)
	{ return typeof( o ) == 'undefined' ? false : true; }
	
	/* Получить элемент/элементы по id / массиву id */
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
	};
	
	/* Получить элементы по тэгу */
	var ge_tag = function( tag )
	{
		return document.getElementsByTagName( tag );
	};
	
	/* Есть ли у переданного элемента запрошенный класс */
	var hasClass = function(element, value)
	{
		var _class_pattern = new RegExp(value, "gi");	
		return element.className.match( _class_pattern ) ? true : false;
	};
	
	var is_retina = function()
	{
		return window.devicePixelRatio > 1 ? true : false;
	}
	
	/* Добавить указанный класс к переданному элементу */
	var addClass = function(element, value)
	{
		var element = typeof( element ) == 'string' ? ge(element) : element;
		
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
	
	/* Удалить указанный класс с элемента */
	var removeClass = function(element, value)
	{
		var element = ge(element);
		
		if(element==null)
			return 0;
		
		if (!element.className)
		{
			element.className = '';
		}else
		{
			newClassName = element.className.replace(value,'').replace(/\s{2,}/g, ' ');
			element.className = newClassName;
		}
	};
	
	/* Показать объект */
	var show = function( element )
	{
		if( ge(element) == null ) return false;
		ge(element).style.display = 'block';
	}
	sio.show = show;
	
	/* Скрыть объект */
	var hide = function( element )
	{
		if( ge(element) == null ) return false;
		ge(element).style.display = 'none';
	}
	sio.hide = hide;
	
	/* Повесить событие / группу событий */
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

	/* Повесить листнер на объект */
	var addListener = function(o,type,listener)
	{
		if(o.addEventListener)
			o.addEventListener(type, listener, false);
			else if(o.attachEvent)
			o.attachEvent('on' + type, function() { listener.apply(o); });
	};
	
	/* Создать style tag, содержащий переданный в content css */
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
	};
	
	/* Найти координаты объекта */
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
		
		return {'left':cl,'top':ct}
	};
	

	
	/* Отмапить список с  указанной функцией */
	var siomap = function( fun, list )
	{
		for( var i in list )
		{
			if( typeof( list[i] ) != 'function' ) fun( list[i],i );
		}
	}
	
	/* Запилить jsonp запрос */
	var _make_request = function( url )
	{
		var script_tag = ce('script', {type:'text/javascript', src: url});
		ge_tag('head')[0].appendChild( script_tag );
	};
	

	
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
	
	/* Сконвертить кодировку */
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
	sio.win2unicode = win2unicode;
	
	/* Набижать на страницу и нограбить цвета */
	var get_page_colors = function()
	{
		if( typeof( sio.gpcs ) != 'undefined' ) return sio.gpcs;
		
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
		
		sio.gpcs = colors;
		
		return colors;
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
	
	var sio_hex_to_rgb = function( hex )
	{
		var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
		return result ? {
			r: parseInt(result[1], 16),
			g: parseInt(result[2], 16),
			b: parseInt(result[3], 16)
		} : null;
	}
	sio.sio_hex_to_rgb = sio_hex_to_rgb;
	
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
	
	var select_random_word_on_page = function()
	{
		var divs = ge_tag( 'h1' );
		for( var i in divs )
		{
			var d = divs[i];
			var word = d.innerHTML;
			break;
		}
		
		var w = word ? word.toLowerCase() : ""
		w = w.replace(/(<([^>]+)>)/ig,"");
		w = w.replace("\t",'');
		
		return w;
		
	}
	
	sio.select_random_word_on_page = select_random_word_on_page;
	
	/**********/
	/* Функции для взаимодействия с серваком sio */
	/* Отправить на сервер sio запрос на получение json с настройками для домена */
	var _get_domain_data = function()
	{
		var domain = window.location.hostname;
		var url = config.sio_host + 'domain_data/get/' + domain;
		_make_request( url );
	}
	
	/* callback, дергается после вызова _get_domain_data */
	var _receive_domain_data = function( d )
	{
		
		// на случай если data будет undefined
		var d = d || {};
		
		sio.domain_data = typeof( d.data ) != 'undefined' ? JSON.parse(d.data) : {};
		
		if( !sio.domain_data.lang ) sio.domain_data.lang = config.lang;
		
		// все ок, настройки получены, можно запускать поиск, передав в качестве параметра 1
		search.init(1);
		
		// разместить кнопку с управлением настройками
		{% if host_admin or render_installer %}
		render_admin_button();
		{% endif %}
	}
	sio._receive_domain_data = _receive_domain_data;
	
	/* схоронить domain_data на сервак */
	var _set_domain_data = function()
	{
		var d = JSON.stringify( sio.domain_data );
		
		if( !ge('sio-post-iframe') )
		{
			// sio iframe for
			var _sio_iframe = ce( 'iframe', {id:'sio-post-iframe', name:'sio-post-iframe'}, '' );
			_sio_iframe.style.display = 'none';
			ge_tag('body')[0].appendChild( _sio_iframe );
			
			// sio form
			var form_data = '<input type="text" name="domain" 			id="siohostValue" value="">'+
											'<input type="text" name="json" 				id="sioJsonValue" value="">'+
											'<input type="text" name="show_images"	id="sioShowImagesValue"  value="1">'+
											'<input type="text" name="show_content_text" value="1">'+
											'<input type="text" name="show_title" value="1">'+
											'<input type="submit" value="post">';
											
			var _sio_form = ce( 'form', {id:'sio-post-form',action: config.sio_host + 'admin/set_domain_settings',method:'post',target:'sio-post-iframe'}, form_data );
			_sio_form.style.display = 'none';
			ge_tag('body')[0].appendChild( _sio_form );
		}
		
		var hostname = window.location.hostname;
		
		ge('siohostValue').value = hostname;
		ge('sioJsonValue').value = d;
		
		ge('sioShowImagesValue').value = ge('sio_cselect_active_value_is_show_images_selector').value;
		
		ge('sio-post-form').submit();
		
		sio.preferences.all_changes_saved();
		
		if( typeof( sio._qi_completed ) != 'undefined' && sio._qi_completed == true )
		{
			sio.search.hideSearch();
			sio.preferences.hide();
			
			ge('sio_qi_window').innerHTML = '<div class="qi-window"><div class="qi-w-inner"><div class="qi-w-inner-2"><div class="qi-sio-logo"><a href="https://suggest.io/"></a></div>'+
													 '<div><small>\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u044b!</small></div>' +
													 '<div><small>\u0414\u043b\u044f \u0431\u044b\u0441\u0442\u0440\u043e\u0433\u043e \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u043c \u0432 \u0431\u0443\u0434\u0443\u0449\u0435\u043c, \u043d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u043e \u043f\u0440\u043e\u0439\u0442\u0438 \u043f\u0440\u043e\u0446\u0435\u0441\u0441 <a href=\"http://suggest.io/login/complete_install\">\u0431\u044b\u0441\u0442\u0440\u043e\u0439 \u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u0438 \u043d\u0430 \u0441\u0430\u0439\u0442\u0435 Suggest.io<\/a>.</small></div>' +
													 '</div></div></div>';
			
			ge('sio_qi_window').style.display = 'block';
		}
		
	}
	sio._set_domain_data = _set_domain_data;
	
	/* Функция для рендера кнопки админа */
	var render_admin_button = function()
	{
		
		var _ab = ce('div',{'class':'sio-admin-button','id':'adio_admin_button'});
		_ab.innerHTML = '<div onclick="sio.preferences.init();"></div>';
		ge_tag('body')[0].appendChild( _ab );
		
		bind(ge('adio_admin_button'), 'click', function( event )
		{
			event.stopPropagation();
		});
		
	}
	
	var cache = {};
	
	/* Тут все что касается непосредственно процесса поиска */
	var search =
	{

		/* Инициализация поискового контейнера */
		init_layout : function()
		{
			
			if( ( typeof( sio.domain_data.search_layout ) == 'undefined' && ( typeof( sio.domain_data.search_field ) == 'undefined' || sio.domain_data.search_field == 'default') ) 
						|| ( typeof( sio.domain_data.search_layout ) != 'undefined'  && sio.domain_data.search_layout == 'default' && sio.domain_data.search_field != 'default')
						|| ( sio.domain_data.search_layout == 'default' && sio.domain_data.search_field == 'default' ) )
			{
				sio.domain_data.search_layout = 'default';
				this.drop_down_search_window.init();
			}else
			{
				sio.domain_data.search_layout = 't_style';
				this.t_style_search_window.init();
			}
			
			/* Если юзер уже вводил какой-то запрос — ввести его в поле и отрендерить результатен */
			/* Если нет — выбрать рандомное слово на странице и запилить из него поисковый запрос */
			/* И все это только если активны настройки ЙО */
			
			if( typeof( sio.preferences ) != 'undefined' && typeof( sio.is_preferences_active ) != 'undefined' && sio.is_preferences_active === true && typeof( sio.preferences.forbid_auto_complete ) != 'undefined' && sio.preferences.forbid_auto_complete != true )
			{
				if( typeof( sio.prev_user_search ) != 'undefined' && sio.prev_user_search != '' )
				{
					sio.search_field.value = sio.prev_user_search;
				}else
				{
					sio.prev_user_search = sio.search_field.value = sio.select_random_word_on_page();
				}
				
				sio.search.process_query();
			}
			
			if( typeof( sio.preferences ) != 'undefined' ) sio.preferences.forbid_auto_complete = false;
			
		},

			/* Отпозиционировать окошка */
			set_position : function()
			{
				if( typeof( sio.domain_data.dimensions ) == 'undefined' ) return false;
				
				var pos = findPos( sio.search_field );
				var _lt = pos.left;
				
				var _wo = _lt + sio.domain_data.dimensions.window_width + sio.domain_data.dimensions.window_margin + 10;
				
				if( _wo > window.innerWidth )
				{
					_lt = _lt - Math.abs( window.innerWidth - _wo - 10 );
				}
				
				ge('sio_search_window').style.left = _lt + 'px';
				ge('sio_search_window').style.top = pos.top + sio.search_field.offsetHeight + 'px';
				
			}
		},
		/* Нарисовать тэшечку */
		t_style_search_window : {
			init : function()
			{

				
				bind(sio.search_field, 'keydown', function()
				{
					sio.search.search_field_keydown_event();
				});
				
				bind(window, 'resize', function()
				{
					sio.search.t_style_search_window.adjust();
				});
				
				bind(ge('sio_search_window'), 'click', function()
				{
					sio.search.hideSearch();
				});
				
				bind(ge('sio_searchResults'), 'click', function( event )
				{
					event.stopPropagation();
				});
				
			},
			adjust : function()
			{
				if( ge('sio_csf') == null ) return false;
				//show( ge('sio_csf') );
				ge('sio_searchResults').style.paddingTop = ge('sio_csf').offsetHeight + 'px';
				
				if( sio.domain_data.base_element )
				{
					var _be = ge(sio.domain_data.base_element);
					
					if( _be == null ) return false;
					
					var left = findPos(_be);
					left = left.left;
					var width = _be.offsetWidth;
					
					sio.domain_data.dimensions.column_margin = 0;
					sio.domain_data.dimensions.column_width = width - 20;
					
					ge('sio_searchResults').style.left = left + 'px';
					
					sio.search._generate_custom_template_style();
					
				}
				
			}
		},
		/* Выпилить поиск */
		destroy : function()
		{
			if( typeof( sio.search_field ) != 'undefined' ) sio.prev_user_search = sio.search_field.value;
			delete(sio.search_field);
			re('sio_csi');
			re('sio_csf');
			re('sio_search_window');
		},
		
		// Показать поиск
		showSearch : function()
		{
			show("sio_csf");
			
			if( sio.search_field.value != '' ) show("sio_search_window");
			
			sio.search.t_style_search_window.adjust();
			
			sio.search_field.focus();
			
			var _b = document.getElementsByTagName('body')[0];
			
			if( !hasClass(_b, 'sio-fixed-body') && sio.domain_data.search_layout != 'default' ) _b.className = _b.className + ' sio-fixed-body';
			
		},
		hideSearch : function()
		{
			hide("sio_csf");
			hide("sio_search_window");
			
			var _b = document.getElementsByTagName('body')[0];
			_b.className = _b.className.replace(' sio-fixed-body', '');
			
		},


		locate_field_on_page : function()
		{
			
			var input_tags = ge_tag('input');
				
				for( var i in input_tags )
				{
					var x = input_tags[i];
					
					if ( x.id == 'sio_search_field' ) continue;
					
					if( typeof( x ) == 'object' && ( x.type == 'text' || x.type == 'search' ) )
					{
						if( this.is_search_field( x ) )
						{
							sio.domain_data.search_field = 'default';
							return x;
						}else
						{
							var _pe = x;
							for( var l=0;l<config.search_field_test_depth;l++ )
							{
								_pe = _pe.parentNode;
								if( this.is_search_field( _pe ) === true )
								{
									sio.domain_data.search_field = 'default';
									return x;
								}
							}
							
						}
					
				};
			};
			
			return null;
			
		},


		//
		process_query : function()
		{

			if( sio.search_field.value == '' )
			{
				if( ge('sio_csf_label') != null ) ge('sio_csf_label').style.display = 'block';
			}
			else
			{
				if( ge('sio_csf_label') != null ) ge('sio_csf_label').style.display = 'none';
			}

			// запилить значение поискового запроса
			var _sr = sio.search_field.value;
			if( _sr == '' )
			{
				hide("sio_search_window");
				return false;
			}

			// проерить есть ли чо в кеше
			if( typeof( cache[_sr] ) != 'undefined' )
			{
				sio.search._draw_result( cache[_sr], _sr );
				show("sio_search_window");
				return false;
			}

			// определить кодировку
			docCharset = document.inputEncoding ? document.inputEncoding : document.charset;
			if( typeof( docCharset ) == 'undefined' ) docCharset = document.characterSet;

			_sr = encodeURIComponent(_sr);
			var _h = window.location.hostname;

			// пофиксить кодировку
			if(docCharset == 'windows-1251'){
				_sr = unescape( _sr );
				_sr = sio.win2unicode( _sr );

				_h = encodeURIComponent( _h );
				_h = unescape( _h );
				_h = sio.win2unicode( _h );
			}

			sio.selected_result = null;

			// TODO: тут что-то с языком — надо вспомнить
			/*
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

			}*/

			var _lrp = typeof(_sl) != 'undefined' ? "&l=" + _sl : '';

			this.sendRequest( config.sio_host + 'search?h=' + _h + '&q=' + _sr + _lrp );

		},
		_draw_result : function( _sr, _q)
		{

			if( _q != '' ) cache[_q] = _sr;

			var _sr_container = document.getElementById('sio_searchResults')

			document.getElementById('sio_searchResults').scrollTop = 0;
			_sr_container.innerHTML = '';

			var _res_target = typeof( sio.domain_data.search_preferences ) != 'undefined' ? sio.domain_data.search_preferences['results_target'] : '_self';
			var _res_link = typeof( sio.domain_data.search_preferences ) != 'undefined' ? sio.domain_data.search_preferences['results_links'] : 'true';

			siomap(function( x )
			{
				if( typeof( x ) != 'object' ) return false;

				if( document.location.protocol == 'https:' && x.url.substring(0,6) != 'https:' ) x.url = x.url.replace('http://','https://');

				var shorten_url = x.url.length > 35 ? x.url.substring(0,35) + '...' : x.url;

				var r_n = '<div class="sio-result"><div class="sio-result-inner">';

				var r_image = typeof( x.image_rel_url ) != 'undefined' ? x.image_rel_url : '';

				r_n += '<a class="sio-result-title" href="' + x.url + '" target="' + _res_target + '">' + x.title + '</a>'

				r_n += '<div class="sio-result-desc">';

				if( r_image != '' ) r_n += '<img class="sio-result-image" width="100" src="' + config.sio_host + r_image + '"/>';

				if( x.content_text ) r_n += x.content_text;

				r_n += '</div>'

				if( _res_link == "true" )
					r_n += '<div class="sio-clear"></div><div class="sio-result-link">' + shorten_url + '</div></div>';
					else
					r_n += '<div class="sio-clear"></div></div></div>';

					_sr_container.innerHTML += r_n;

			}, _sr);
		},
		
		/* Сгеренить необходимые стили */
		_generate_custom_template_style : function()
		{
			
			if( typeof( sio.forbid_style_generation ) != 'undefined' && sio.forbid_style_generation == true )
			{
				sio.forbid_style_generation = false;
				return false;
			}
			
			if( sio.domain_data.search_layout == 'default' )
			{
				var tpl = sio.domain_data.drop_down_template ? sio.domain_data.drop_down_template : sio.domain_data.search_window;
				var tpl_data = sio.templates.drop_down_windows[tpl];
			}else
			{
				var tpl = sio.domain_data.t_style_template;
				var tpl_data = sio.templates.t_windows[tpl];
			}
			
			
			var template_style = tpl_data.css;
			var params = tpl_data.params;
			
			var css_content = template_style;
			
			if( typeof( sio.domain_data.colors ) == 'undefined' ) sio.domain_data.colors = {};
			if( typeof( sio.domain_data.dimensions ) == 'undefined' ) sio.domain_data.dimensions = {};
			
			siomap(function( color, param )
			{
				var p = color.p;
				var c = sio.domain_data.colors[p] ? sio.domain_data.colors[p] : color.d;
				
				css_content = css_content.replace(new RegExp('<<' + p + '>>', 'g'),c);
			}, params.colors);
			
			siomap(function( dim, param )
			{
				var prm = dim.p;
				
				if( typeof( sio.domain_data.dimensions[prm] ) != 'undefined' )
				{
					var c = sio.domain_data.dimensions[prm]
				}else
				{
					var c = dim.d;
					sio.domain_data.dimensions[prm] = dim.d;
				}
				
				css_content = css_content.replace(new RegExp('<<' + prm + '>>', 'g'),c);
			}, params.dimensions);
			
			
			// Запилить стиль для уголка
			if( sio.domain_data.search_layout != 'default' )
			{
				for( var i in params.colors )
				{
					if( params.colors[i].p == 'sf_bg' ) var _sf_dc = params.colors[i].d;
				}
				
				var _st = sio.domain_data.search_field;
				var _sf_c = sio.domain_data.colors.sf_bg ? sio.domain_data.colors.sf_bg : _sf_dc;
				css_content += ' ' + templates.generate_sf_css(_st,_sf_c);
			}
			/* Сделать непосредственно вызов генератора */
			
			create_css( css_content );
			
		},
		// Сгенерить картинки для уголков
		generate_sbg : function()
		{
			sio.forbid_style_generation = true;
			
			var cf = sio.domain_data.search_field;
			var cf_o = templates['search_fields'][cf];
			
			var t_tpl = sio.domain_data.t_style_template;
			
			var d_ps = templates.t_windows[t_tpl].params.colors;
			for( var i in d_ps )
			{
				if( d_ps[i].p == 'sf_bg' ) _def_c = d_ps[i].d;
			}
			
			if( typeof( cf_o ) == 'undefined' ) return false;
			
			var cc = typeof( sio.domain_data.colors.sf_bg ) != 'undefined' ? sio.domain_data.colors.sf_bg : _def_c;
			
			var gp = cf_o['gen_params'];
			
			if( typeof( gp ) == 'undefined' ) return false;
			
			//generate images
			var rgb = sio_hex_to_rgb( cc );
			var bg_color = rgb['r'] + ',' + rgb['g'] + ',' + rgb['b'] + ',0.7';
			
			var _ibg = cf == 'crnr-3' ? 'true' : 'false';
			
			sio.sendRequest(config.sio_host + '/images/gen_si/' + cc + '/' + bg_color + '/' + gp['prefix'] + '/' + gp['ds'] + '/' + gp['ds_retina'] + '/' + _ibg );
			
		}
		
	}
	
	sio.set_sf_style = function(data)
	{
		sio.forbid_style_generation = false;
		sio.search._generate_custom_template_style();
	}
	
	sio._s_add_result = search._s_add_result;
	sio.sendRequest = search.sendRequest;
	sio.search = search;
	
	/* Быстрая установка */
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
		
		{% if render_installer %}
		return true;
		{% else %}
		return false;
		{% endif %}
		
	}
	
	var _qi_complete = function()
	{
		sio._qi_completed = true;
		
		{% if render_installer %}
		var host = '{{dkey}}';
		var timestamp = {{timestamp}}
		
		sio.dkey_host = host;
		
		// запостить
		
		var _sio_iframe_1 = ce( 'iframe', {id:'sio-install-post-iframe', name:'sio-install-post-iframe'}, '' );
		_sio_iframe_1.style.display = 'none';
		ge_tag('body')[0].appendChild( _sio_iframe_1 );
		
		// sio form
		var _sio_install_form = ce( 'form', {id:'sio-install-post-form',action: config.sio_host + 'js/install_url/' + host + '/{{domain_qi_id}}',method:'post',target:'sio-install-post-iframe'}, '<input type="text" name="url" value="' + window.location + '"><input type="submit" value="post">' );
		_sio_install_form.style.display = 'none';
		ge_tag('body')[0].appendChild( _sio_install_form );
    
    _sio_install_form.submit();
		
		// Отрендерить окно c установкой
		var _qi_window = ce('div',{'class':'sio-install-steps','id':'sio_qi_window'});
		_qi_window.innerHTML = '<div class="qi-window"><div class="qi-w-inner"><div class="qi-w-inner-2"><!--<div class="sio-qi-close-cross"><a href="" onclick="sio._close_qi(); return false;"></a></div>--><div class="qi-sio-logo"><a href="https://suggest.io/"></a></div>'+
													 '<div><small>\u041a\u043e\u0434 Suggest.io \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d \u043d\u0430 \u0441\u0430\u0439\u0442! \u0427\u0435\u0440\u0435\u0437 \u043d\u0435\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0441\u0435\u043a\u0443\u043d\u0434 \u0431\u0443\u0434\u0435\u0442 \u0437\u0430\u043f\u0443\u0449\u0435\u043d \u043f\u0440\u043e\u0446\u0435\u0441\u0441 \u0438\u043d\u0434\u0435\u043a\u0441\u0430\u0446\u0438\u0438 \u0441\u0430\u0439\u0442\u0430.</small></div>' +
													 '<div><small><strong>\u0421\u0442\u0430\u0442\u0443\u0441:</strong> <span id="qi_status_message">\u0437\u0430\u043f\u0443\u0441\u043a \u0438\u043d\u0434\u0435\u043a\u0441\u0430\u0446\u0438\u0438</span></small></div>' +
													 '<div class="qi-prefs-button"><a href="" onclick="sio._close_qi(); sio.preferences.init(); return false;"></a><div>' +
													 '</div></div></div>';
		
		ge_tag('body')[0].appendChild(_qi_window);
		
		setTimeout(function()
		{
			_listen_qi_events( host, timestamp );
		}, 500);
		
		{% endif %}
		
	}
	
	var _listen_qi_events = function( host, timestamp )
	{
		var _lqi = ce('script', {type:'text/javascript',src: config.sio_host + '/js/pull_installer/' + host + '/' + timestamp});
    ge_tag('head')[0].appendChild( _lqi );
	}
	
	var qi_events = function( data )
	{
		var timestamp = data.timestamp;
		var events = data.events;
		
		for( event in events )
		{
			var e = events[event];
			if( e.type == 'is_js_installed' )
			{
				if( e.is_js_installed === true )
				{
					ge('qi_status_message').innerHTML = '\u0418\u043d\u0434\u0435\u043a\u0441\u0430\u0446\u0438\u044f \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u0430!';
					return false;
				}else
				{
					ge('qi_status_message').innerHTML = '\u041f\u0440\u043e\u0438\u0437\u043e\u0448\u043b\u0430 \u043e\u0448\u0438\u0431\u043a\u0430 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0438, \u043e\u0431\u0440\u0430\u0442\u0438\u0442\u0435\u0441\u044c \u0432 \u0441\u043b\u0443\u0436\u0431\u0443 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u043a\u0438';
				}
			}
		}
		
		_listen_qi_events( sio.dkey_host, timestamp );
	}
	sio.qi_events = qi_events;
	
	/* Тут все что касается настроек */
	/* Все настройки хранятся в объекте
		 domain_data =
		 {
		 	'search_field' 	: 'default' || ''
		 	'search_layout' : 'default' || 'suit'
		 } */
	
	{% if host_admin or render_installer %}
	var preferences =
	{
		// скрыть окно с настройкмаи
		hide : function()
		{
			sio.is_preferences_active = false;
			hide(ge('sio_spf'));
		},
		// массив с id окон для редактированиы
		prefs : ["search_field","search_layout","colors","dimensions","search_preferences"],
		
		/* Далее у нас идут объекты с настройками */
		/* Настройки поля ввода — либо используем дефолтовое поле ввода с выпадающим полем, либо сьют с кастомной иконкой */
		
		search_field :
		{
			before_active : function()
			{
				hide( ge('sio_reset_button') );
			},
			// отрисовать блок
			draw : function()
			{
				
				// тут мы проверяем, используется дефолтовое поле или кастомное
				// это какие-то мега костыли — надо придумать решение для проверки более изящное
				if( sio.domain_data.search_field == 'default' )
				{
					var is_def_sf = 1;
					var is_cust_sf = 0;
				}
				else
				{
					var is_def_sf = 0;
					var is_cust_sf = 1;
				}
				//
				var h = "";
				
				h += sio.preferences.checkbox(transl('use_default_field'), 'onclick="sio.preferences.search_field.set(\'default\');"',is_def_sf,"spf_sf_def_cb","sio-spf-sf-checkbox");
				
				h += sio.preferences.checkbox(transl('use_custom_field'), 'onclick="sio.preferences.search_field.set(\'custom\');"',is_cust_sf,"spf_sf_cust_cb", "sio-spf-sf-checkbox");
				h += '<div class="sio-clear"></div>';
				
				var _disp = is_def_sf == 1 ? 'display: none;':'';
				
				h += '<div id="sioPfsNoFieldExplanation" class="sio-pfs-text" style="display: none;"><p>К сожалению, Suggest.io не удалось найти поискового поля на странице. Если поле на странице все же есть, ознакомьтесь с нашими рекомендациями по насройке.</p><p><a onclick="sio.preferences.search_field.set(\'custom\'); return false;" href="">Ясно, понятно, вернуться к выбору иконок</a></p></div>';
				
				h += '<div class="sio-spf-sf-icons" id="sioSpfSfIcons" style="' + _disp + '">';
				for( var t in templates.search_fields )
				{
					var _cls = t == 'default' ? 'def' : t;
					var _is_active = t == sio.domain_data.search_field ? 'sio-pfs-sf-select-active' : '';
					h += "<div id=\"sioPfsSfSelect" + t + "\" class=\"" + _is_active + " sio-pfs-sf-select sio-field-prev-" + _cls + "\" onClick=\"sio.preferences.search_field.set('" + t + "')\">" + '</div>';
				}
				h += '</div>';
				
				return h;
			},
			set : function( v )
			{
				sio.preferences.unsaved_changes();
				// если значение соответствует текущему — не нужно ничего делать
				if( v == sio.domain_data.search_field ) return false;
				
				sio.preferences.forbid_auto_complete = true;
				
				if( v == 'default' )
				{
					if( sio.search.locate_field_on_page() == null )
					{
						// Поля на странице нет, откатить назад
						v = sio.domain_data.search_field;
						ge('spf_sf_cust_cb').checked = '';
						show('sioPfsNoFieldExplanation');
						hide("sioSpfSfIcons");
						return false;
					}
				}
				
				hide('sioPfsNoFieldExplanation');
				// вызвать метод destroy
				sio.search.destroy();
				// выпилить активные состояния с иконок
				for( var t in templates.search_fields )
				{
					removeClass('sioPfsSfSelect' + t, 'sio-pfs-sf-select-active');
				}
				
				// Если в качестве значение передана строка 'custom' — значит надо выбрать дефолтовую катомную иконку
				if( v == 'custom' )
				{
					// TODO : вынести дефолтовое поле в конфиг
					sio.domain_data.search_field = 'crnr-1'
					ge('spf_sf_def_cb').checked = '';
					ge('spf_sf_cust_cb').checked = 'checked';
					sio.preferences.search_layout.set("t_style",0);
					show("sioSpfSfIcons");
					
					addClass('sioPfsSfSelect' + 'crnr-1', 'sio-pfs-sf-select-active');
					//sio.search.generate_sbg();
				}
				else
				{
					sio.domain_data.search_field = v;
					
					if( v == 'default' )
					{
						ge('spf_sf_def_cb').checked = 'checked';
						ge('spf_sf_cust_cb').checked = '';
						sio.preferences.search_layout.set("default",0);
						
						hide("sioSpfSfIcons");
						
					}else
					{
						ge('spf_sf_def_cb').checked = '';
						ge('spf_sf_cust_cb').checked = 'checked';
						
						addClass('sioPfsSfSelect' + v, 'sio-pfs-sf-select-active');
						
						sio.preferences.search_layout.set("t_style",0);
						sio.search.generate_sbg();
					}
					sio.search.init(1);
				}
				
				sio.search.hideSearch();
				
			}
		},
		
		/* Настройки разметки поиска — шаблоны и тд */
		
		search_layout :
		{
			before_active : function()
			{
				hide( ge('sio_reset_button') );
			},
			
			// отрисовать блок
			draw : function()
			{
				
				var h = '';
				// два чекбокса для выбора типа разметки
				var is_def = typeof( sio.domain_data.search_layout ) == 'undefined' || sio.domain_data.search_layout == 'default' ? 1 : 0;
				var is_cust = is_def ? 0 : 1;
				
				//h += sio.preferences.checkbox(transl('use_drop_down_window'), 'onclick="sio.preferences.search_layout.set(\'default\');"', is_def,"spf_sl_def_cb", "sio-spf-sf-checkbox");
				//h += sio.preferences.checkbox(transl('use_great_t_style'), 'onclick="sio.preferences.search_layout.set(\'t_style\');"', is_cust,"spf_sl_t_cb", "sio-spf-sf-checkbox");
				h += '<div class="sio-pfs-text">\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0448\u0430\u0431\u043b\u043e\u043d \u043f\u043e\u0438\u0441\u043a\u0430</div>';
				
				var def_display = is_def ? 'block' : 'none';
				var cust_display = is_def ? 'none' : 'block';
				
				// отрендерить возможные шаблоны для выпадающих окон
				
				h += '<div id="sio_pfs_sl_def_tpls" class="sio-spf-sf-icons" style="display: ' + def_display +'">';
				for( var t_i in sio.templates.drop_down_windows )
				{
					var x = sio.templates.drop_down_windows[t_i];
					var is_active = sio.domain_data.drop_down_template == t_i ? 'sio-pfs-sf-select-active' : '';
					h += '<a id="sioSpfLTpl' + t_i + '" class="sio-pfs-sf-select sio-pfs-sf-select-layout ' + is_active + '" href="" onclick="sio.preferences.search_layout.set_template(\'drop_down_template\',\'' + t_i + '\');return false;"><img src="https://suggest.io/static/images/'+ x.thumbnail + '"></a>';
				};
				
				h += '</div>';
				
				
				// отрендерить щаблоны для тэшек
				h += '<div id="sio_pfs_sl_t_tpls" class="sio-spf-sf-icons" style="display: ' + cust_display + '">';
				
				for( var t_i in sio.templates.t_windows )
				{
					var x = sio.templates.t_windows[t_i];
					var is_active = sio.domain_data.t_style_template == t_i ? 'sio-pfs-sf-select-active' : '';
					h += '<a id="sioSpfLTpl' + t_i + '" class="sio-pfs-sf-select sio-pfs-sf-select-layout ' + is_active + '" href="" onclick="sio.preferences.search_layout.set_template(\'t_style_template\',\'' + t_i + '\');return false;"><img src="https://suggest.io/static/images/'+ x.thumbnail + '"></a>';
				};
				
				h += '</div>';
				
				return h;
				
			},
			set : function( v, is_reinit )
			{
				sio.preferences.unsaved_changes();
				// проверить, отличается ли значение от текущего
				if( v == sio.domain_data.search_layout ) return false;
				
				// вызвать метод destroy
				//if( is_reinit != 0 ) sio.search.destroy();
				
				sio.domain_data.search_layout = v;
				
				if( v == 'default' )
				{
					show("sio_pfs_sl_def_tpls");
					hide("sio_pfs_sl_t_tpls");
					
					sio.preferences.search_field.set("default");
				}else
				{
					
					this.set_template('t_style_template', templates.t_style_def_tpl );
					
					hide("sio_pfs_sl_def_tpls");
					show("sio_pfs_sl_t_tpls");
					
				}
				
				//if( is_reinit != 0 ) sio.search.init(1);
				
				ge('sio_pfs_block_dimensions').innerHTML = sio.preferences.dimensions.draw();
				ge('sio_pfs_block_colors').innerHTML = sio.preferences.colors.draw();
				
			},
			set_template : function( p, v )
			{
				sio.preferences.unsaved_changes();
				if( typeof( sio.search_field ) != 'undefined' ) sio.prev_user_search = sio.search_field.value;
				removeClass('sioSpfLTpl' + sio.domain_data[p], 'sio-pfs-sf-select-active');
				sio.domain_data[p] = v;
				addClass('sioSpfLTpl' + v, 'sio-pfs-sf-select-active');
				re("sio_search_window");
				sio.search.init(1);
				sio.search.showSearch();
				
				ge('sio_pfs_block_dimensions').innerHTML = sio.preferences.dimensions.draw();
				ge('sio_pfs_block_colors').innerHTML = sio.preferences.colors.draw();
				
			}
		},
		
		/* Настройка цветов */
		colors :
		{
			before_active : function()
			{
				show( ge('sio_reset_button') );
				
				ge('sio_reset_button').onclick = function()
				{
					ge('sio_reset_button').getElementsByTagName('a')[0].innerHTML = transl( 'saved_label' );
					sio.domain_data.colors = {};
					sio.search._generate_custom_template_style();
					ge('sio_pfs_block_colors').innerHTML = sio.preferences.colors.draw();
					setTimeout(function()
					{
						ge('sio_reset_button').getElementsByTagName('a')[0].innerHTML = transl( 'reset_label' );
					}, 300);
				};
				
				sio.search.destroy();
				sio.search.init(1);
				sio.search.showSearch();
				
				if( !this.colorpicker_initiated )
				{
				this.colorpicker_initiated = true;
				setTimeout(function()
				{
					
					ColorPicker(

        document.getElementById('slider'),
        document.getElementById('picker'),

        function(hex, hsv, rgb) {
          sio.preferences.colors.setColor(hex.replace('#',''));
        });
					
				}, 100);
				}
			},
			draw : function()
			{
				
				var h = this.colorsPane();
				return h;
				
			},
			colorsPane : function()
			{
				var c = '';
				var colors = [];
				
				colors = get_page_colors().slice(0,21);
				
				if( sio.domain_data.search_layout == 'default' )
				{
					var cw = sio.domain_data.drop_down_template;
					var params = templates.drop_down_windows[cw].params.colors;
				}
				else
				{
					var cw = sio.domain_data.t_style_template;
					var params = templates.t_windows[cw].params.colors;
				}
				
				this.c_params = params;
				
				c += '<div class="sio-pfs-colors-left"><div class="sio-pfs-text">' + transl('color_preferences') + '</div>';
				
				
				// Кастомный селектор
				c += '<div class="sio-pfs-color-selector">'
				
				var ap = this.active_param = params[0].p;
				var _dc = exists( sio.domain_data.colors[ap] ) ? sio.domain_data.colors[ap] : params[0].d;
				
				c += '<div id="sioPfsColorsActive" class="sio-pfs-color-selector-active" onclick="sio.preferences.colors.showColorsList();">' + this.generate_list_active( ap, params[0].n, _dc ) + '</div>';
				
				c += '<div class="options" id="sioPfsColorsList">';
				
				var _a_none_bdf = false;
				
				siomap( function( param, index )
				{
					
					if( typeof( sio.domain_data.colors ) == 'undefined' ) sio.domain_data.colors = {};
					var _dc = exists( sio.domain_data.colors[param.p] ) ? sio.domain_data.colors[param.p] : param.d;
					
					var _is_active_option = index == 0 ? 'active-option' : '';
					
					if( index == 0 )
					{
						_a_none_bdf = param.z == true ? true : false;
					}
					
					c += '<div id="sioColorsListParam' + param.p + '" onclick="sio.preferences.colors.setActiveParam(\'' + param.p + '\');" class="sio-pfs-color-selector-option ' + _is_active_option + '">' + param.n + '</div>';
					
				},params);
				
				c += '</div></div></div>';
				
				c += '<div class="sio-pfs-colors-right">';
				
				siomap( function( color )
				{
					c += "<div class='sio-pfs-color' style='background: #" + color + ";' onClick=\"sio.preferences.colors.setColor('" + color + "')\"><span></span></div>";
				}, colors );
				
				// без цвета
				var _nst = _a_none_bdf == true ? 'block' : 'none';
				c += "<div class='sio-pfs-color' id='colorsNoneColor' style='display: " + _nst + "; background: url(\"https://suggest.io/static/images2/sio-none-color.png\");' onClick=\"sio.preferences.colors.setColor('none')\"><span></span></div>";
				
				c += '<div class="sio-pfs-color sio-custom-color" onclick="sio.preferences.colors.showColorpicker();"><div id="sioColorpicker" class="sio-colorpicker"><div class="sio-picker" id="picker"></div><div class="sio-slider" id="slider"></div><div class="sio-pfs-color" onclick="sio.preferences.colors.hideColorpicker();"></div></div><span></span></div>';
				
				c += '</div>';
				
				return c;
			},
			showColorpicker : function()
			{
				document.getElementById('sioColorpicker').style.display='block';
			},
			hideColorpicker : function()
			{
				setTimeout(function(){
					document.getElementById('sioColorpicker').style.display='none';
				}, 10);
			},
			generate_list_active : function(param,name,c)
			{
				if( c == 'none' )
					c = "url('https://suggest.io/static/images2/sio-none-color.png') no-repeat center center";
					else
					c = '#' + c;
					
				return '<div class="sio-pfs-color-small" id="' + param +'_preview_color" style="background: ' + c + '"></div>' + name;
			},
			showColorsList : function()
			{
				show("sioPfsColorsList");
			},
			setActiveParam : function( n_param )
			{
				
				removeClass(ge('sioColorsListParam' + this.active_param), 'active-option');
				
				this.active_param = n_param;
				hide("sioPfsColorsList");
				
				addClass(ge('sioColorsListParam' + this.active_param), 'active-option');
				
				siomap( function( param, index )
				{
					if( param.p == n_param )
					{
						// Пробегаемся по списку параметров, находим наш родимый
						param.z == true ? show('colorsNoneColor') : hide('colorsNoneColor');
						
						var _dc = exists( sio.domain_data.colors[param.p] ) ? sio.domain_data.colors[param.p] : param.d;
						ge('sioPfsColorsActive').innerHTML = sio.preferences.colors.generate_list_active( param.p, param.n, _dc );
					}
				}, this.c_params);
				
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
					if( ge(x.p + '_cblock') != null )
					{
						ge(x.p + '_cblock').style.display = 'none';
						removeClass(ge(x.p + '_preview_color'),'selected-color');
					}
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
			
			setColor : function( color )
			{
				
				sio.preferences.unsaved_changes();
				var param = sio.preferences.colors.active_param;
				
				var template = sio.domain_data.search_layout == 'default' ? templates.drop_down_windows[sio.domain_data.drop_down_template] : templates.t_windows[sio.domain_data.t_style_template];
				
				for( var i in template.params.colors )
				{
					var p =template.params.colors[i];
					if( p.p == param ) var color_type = p.t;
				};
				
				
				if( color_type == 'rgba' )
				{
					color = sio.sio_hex_to_rgb( color );
					
					color = 'rgba(' + color.r + ',' + color.g + ',' + color.b + ',.95)';
					color_f = color;
				}
				else
				{
					color = color;
					color_f = '#' + color;
				}
				
				// Если у нас без цвета, none
				if( color == 'none' )
				{
					color_f = "url('https://suggest.io/static/images2/sio-none-color.png') no-repeat center center";
				}
				
				ge(param + '_preview_color').style.background = color_f;
				
				//this.hideAllColorBlocks();
				sio.domain_data['colors'][param] = color;
				
				if( param == 'sf_bg' )
				{
					sio.search.generate_sbg();
				}else
				{
					// сгенерить хуиты
					sio.search._generate_custom_template_style();
				}
				
			}
		},
		
		/* Настройки размеров */
		dimensions :
		{
			before_active : function()
			{
				
				show( ge('sio_reset_button') );
				
				ge('sio_reset_button').onclick = function()
				{
					sio.domain_data.dimensions = {};
					sio.search._generate_custom_template_style();
					
					ge('sio_pfs_block_dimensions').innerHTML = sio.preferences.dimensions.draw();
					ge('sio_reset_button').getElementsByTagName('a')[0].innerHTML = transl( 'saved_label' );
					
					sio.search.t_style_search_window.adjust();
					
					setTimeout(function()
					{
						ge('sio_reset_button').getElementsByTagName('a')[0].innerHTML = transl( 'reset_label' );
					}, 300);
					
				};
				
				sio.search.destroy();
				sio.search.init(1);
				sio.search.showSearch();
			},
			// отрисовать блок
			draw : function()
			{
				
				if( sio.domain_data.search_layout == 'default' )
				{
					
					var h = "<div class='sio-pfs-window-resizer'>"+
										"<a class='dimensions-button l-l' onmousedown=\"sio.preferences.dimensions.changeDimension('left','-')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button l-r' onmousedown=\"sio.preferences.dimensions.changeDimension('left','+')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button r-l' onmousedown=\"sio.preferences.dimensions.changeDimension('right','-')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button r-r' onmousedown=\"sio.preferences.dimensions.changeDimension('right','+')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>" +
										
										"<a class='dimensions-button t-t' onmousedown=\"sio.preferences.dimensions.changeDimension('top','-')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button t-b' onmousedown=\"sio.preferences.dimensions.changeDimension('top','+')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button b-t' onmousedown=\"sio.preferences.dimensions.changeDimension('bottom','-')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button b-b' onmousedown=\"sio.preferences.dimensions.changeDimension('bottom','+')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
									"</div>" +
									
									'<div class="sio-pfs-dimensions-text"><div class="sio-pfs-text"><p><strong>\u0420\u0430\u0437\u043c\u0435\u0440\u044b \u043e\u043a\u043d\u0430 \u0441 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u043c\u0438 \u043f\u043e\u0438\u0441\u043a\u0430</strong></p><p>\u0414\u043b\u044f \u0431\u043e\u043b\u0435\u0435 \u0442\u043e\u0447\u043d\u043e\u0439 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u044b\u0439 \u0437\u0430\u043f\u0440\u043e\u0441, \u043f\u043e \u043a\u043e\u0442\u043e\u0440\u043e\u043c\u0443 \u0431\u0443\u0434\u0435\u0442 \u043d\u0430\u0439\u0434\u0435\u043d\u043e \u0431\u043e\u043b\u044c\u0448\u043e\u0435 \u0447\u0438\u0441\u043b\u043e \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432</p></div></div>';
					
				}else
				{
					
					var h = '<div class="sio-pfs-c-size-selector"><div class="sio-pfs-text">\u0428\u0438\u0440\u0438\u043d\u0430 \u043a\u043e\u043b\u043e\u043d\u043a\u0438 \u0441 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u043c\u0438</div>' +
									"<div class='sio-pfs-plane-resizer'>"+
										"<a class='dimensions-button l-l' onmousedown=\"sio.preferences.dimensions.changeDimension('left','-')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button l-r' onmousedown=\"sio.preferences.dimensions.changeDimension('left','+')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button r-l' onmousedown=\"sio.preferences.dimensions.changeDimension('right','-')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
										"<a class='dimensions-button r-r' onmousedown=\"sio.preferences.dimensions.changeDimension('right','+')\" onclick=\"sio.preferences.dimensions.cancelHold();\"></a>"+
									"</div></div>";
							h += '<div class="sio-pfs-font-selector"><div class="sio-pfs-text">\u0420\u0430\u0437\u043c\u0435\u0440 \u0448\u0440\u0438\u0444\u0442\u0430 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u043f\u043e\u043b\u044f</div>'
							
							// Селектор размера шрифта
							
							var font_sizes = [40,50,60,70,80,100]
							var options = [];
							
							siomap( function(p)
							{
								
								var _o = {'label' : p, 'value' : p, 'callback' : function()
									{
										sio.preferences.dimensions.set_font_size(p);
									}
								};
								if( sio.domain_data.dimensions.font_size == p ) _o.is_active = true;
								
								options.push( _o );
								
							}, font_sizes )
							
							h += sio.preferences.c_select.draw("font_size",options);
							
							h += '</div>'
							
							h += '<div style="display: none;"><div class="sio-pfs-text">Advanced</div><input type="text" onblur="sio.domain_data.base_element = this.value;" value="' + sio.domain_data.base_element  + '"/></div>'
							
							h += '<div class="sio-clear"></div><div class="sio-pfs-text"><p>\u0414\u043b\u044f \u0431\u043e\u043b\u0435\u0435 \u0442\u043e\u0447\u043d\u043e\u0439 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u044b\u0439 \u0437\u0430\u043f\u0440\u043e\u0441, \u043f\u043e \u043a\u043e\u0442\u043e\u0440\u043e\u043c\u0443 \u0431\u0443\u0434\u0435\u0442 \u043d\u0430\u0439\u0434\u0435\u043d\u043e \u0431\u043e\u043b\u044c\u0448\u043e\u0435 \u0447\u0438\u0441\u043b\u043e \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432</p></div>';
				}
				
				// bind mouseup event for window
				bind(window, 'mouseup', function()
				{
					sio.preferences.dimensions.cancelHold();
				});
				
				return h;
			},
			
			set_font_size : function( v )
			{
				sio.preferences.unsaved_changes();
				sio.domain_data.dimensions.font_size = v;
				sio.search._generate_custom_template_style();
				sio.search.t_style_search_window.adjust();
			},
			
			hold_delay_default : 250,
			hold_delay : 250,
			hold_delay_min : 10,
			
			hold_change_default : 1,
			hold_change : 1,
			hold_change_max : 5,
			changeDimension : function( edge, direction )
			{
				sio.preferences.unsaved_changes();
				if( edge == 'left' )
				{
					
					if( sio.domain_data.search_layout == 'default' )
					{
						
						if( direction == '-' )
						{
							sio.domain_data.dimensions.window_width = parseInt( sio.domain_data.dimensions.window_width ) + this.hold_change;
							sio.domain_data.dimensions.window_margin = parseInt( sio.domain_data.dimensions.window_margin ) - this.hold_change;
						}
						else
						{
								sio.domain_data.dimensions.window_width = parseInt( sio.domain_data.dimensions.window_width ) - this.hold_change;
								sio.domain_data.dimensions.window_margin = parseInt( sio.domain_data.dimensions.window_margin ) + this.hold_change;
						}
						
					}
					else
					{
						
						if( direction == '-' )
						{
							sio.domain_data.dimensions.column_width = parseInt( sio.domain_data.dimensions.column_width ) + this.hold_change;
							sio.domain_data.dimensions.column_margin = parseInt( sio.domain_data.dimensions.column_margin ) - this.hold_change;
						}
						else
						{
								sio.domain_data.dimensions.column_width = parseInt( sio.domain_data.dimensions.column_width ) - this.hold_change;
								sio.domain_data.dimensions.column_margin = parseInt( sio.domain_data.dimensions.column_margin ) + this.hold_change;
						}
						
					}
				}
				
				if( edge == 'right' )
				{
					
					if( sio.domain_data.search_layout == 'default' )
					{
						if( direction == '-' )
						{
							sio.domain_data.dimensions.window_width = parseInt(sio.domain_data.dimensions.window_width) - this.hold_change;
						}
						else
						{
							sio.domain_data.dimensions.window_width = parseInt(sio.domain_data.dimensions.window_width) + this.hold_change;
						}
					}
					else
					{
						if( direction == '-' )
						{
							sio.domain_data.dimensions.column_width = parseInt(sio.domain_data.dimensions.column_width) - this.hold_change;
						}
						else
						{
							sio.domain_data.dimensions.column_width = parseInt(sio.domain_data.dimensions.column_width) + this.hold_change;
						}
					}
				}
				
				if( edge == 'top' )
				{
					if( direction == '-' )
					{
						sio.domain_data.dimensions.window_margin_top = parseInt(sio.domain_data.dimensions.window_margin_top) - this.hold_change;
						sio.domain_data.dimensions.window_height = parseInt(sio.domain_data.dimensions.window_height) + this.hold_change;
					}
					else
					{
						sio.domain_data.dimensions.window_margin_top = parseInt(sio.domain_data.dimensions.window_margin_top) + this.hold_change;
						sio.domain_data.dimensions.window_height = parseInt(sio.domain_data.dimensions.window_height) - this.hold_change;
					}
				}
				
				if( edge == 'bottom' )
				{
					if( direction == '-' )
					{
						sio.domain_data.dimensions.window_height = parseInt(sio.domain_data.dimensions.window_height) - this.hold_change;
					}
					else
					{
						sio.domain_data.dimensions.window_height = parseInt(sio.domain_data.dimensions.window_height) + this.hold_change;
					}
				}
				
				sio.search._generate_custom_template_style();
				
				this.hold_delay = this.hold_delay > this.hold_delay_min ? this.hold_delay - 30 : this.hold_delay_min;
				this.hold_change = this.hold_change < this.hold_change_max ? this.hold_change + 1 : this.hold_change_max;
				
				dimensionsHoldChangeTimer = setTimeout( function() { sio.preferences.dimensions.changeDimension( edge, direction ); }, this.hold_delay);
				
			},
			cancelHold : function()
			{
				if( typeof( dimensionsHoldChangeTimer ) != 'undefined' ) clearTimeout( dimensionsHoldChangeTimer );
				this.hold_delay = this.hold_delay_default;
				this.hold_change = this.hold_change_default;
			}
		},
		
		/* Настройки поиска */
		search_preferences :
		{
			before_active : function()
			{
				hide( ge('sio_reset_button') );
			},
			// отрисовать блок
			draw : function()
			{
				
				var h = '<div style="float: left; margin-right: 30px;">';
				
				h += '<div class="sio-pfs-text">' + transl( 'open_results_in' ) + '</div>';
				
				// Где открывать результаты поиска?
				var options = [
												{'label' : transl( 'current_window' ), 'value' : '_self', 'is_active' : true},
												{'label' : transl( 'blank_window' ), 'value' : '_blank'}];
				h += sio.preferences.c_select.draw("results_target",options);
				
				// Показывать картинке?
				{% with show_images=domain_data.show_images|atom_tolist %}
				h += '<div class="sio-pfs-text">' + transl( 'show_images_in_sr_label' ) + '</div>';
				
				options = [
												{'label' : transl( 'yes' ), 'value' : 'true'{% if show_images == "true" %}, 'is_active' : true{% endif %}},
												{'label' : transl( 'no' ), 'value' : 'false'{% if show_images != "true" %},'is_active' : true{% endif %}}];
				h += sio.preferences.c_select.draw("is_show_images_selector",options);
				{% endwith %}
				h += '</div><div style="float: left;">';
				
				// Какой языг?
				h += '<div class="sio-pfs-text">' + transl( 'interface_lang' ) + '</div>';
				
				var _av_langs = [{'l':'ru','v' : '\u0420\u0443\u0441\u0441\u043a\u0438\u0439'},
												 {'l':'en','v' : 'English'}]
				
				options = [];
				
				siomap( function( p )
				{
					var _o = {'label' : p.v, 'value' : p.l, 'callback' : function(p)
						{
							sio.domain_data.lang = p;
						}
					};
					
					if( sio.domain_data.lang == p.l ) _o.is_active = true;
					options.push( _o );
					
				}, _av_langs );
												
				h += sio.preferences.c_select.draw("interface_lang",options);
				
				h += '</div>';
				
				return h;
			}
		},
		
		/*
			Далее идут различные внутренние функции, которые используются для работы с настройками поиска
		*/
		
		all_changes_saved : function()
		{
			
			var _sb = ge('sio_save_button');
			
			removeClass(_sb, 'unsaved-changes');
			addClass(_sb, 'ok-saved-changes');
			
			_sb.getElementsByTagName('a')[0].innerHTML = transl( 'saved_label' );
			
			setTimeout(function()
			{
				var _sb = ge('sio_save_button');
				removeClass(_sb, 'ok-saved-changes');
				_sb.getElementsByTagName('a')[0].innerHTML = transl( 'save_label' );
			}, 1000);
			
			window.onbeforeunload = function()
			{
				
			}
			
		},
		
		unsaved_changes : function()
		{
			addClass(ge('sio_save_button'), 'unsaved-changes');
			window.onbeforeunload = function()
			{
				return transl('before_unload_message');
			}
		},
		
		/* Нарисовать чекбокс с лейблом */
		checkbox : function( label, extraAttibutes, is_checked, id, className )
		{
			if( !id ) var id = '';
			var is_checked = is_checked == 1 ? 'checked' : '';
			
			return '<div class="sio-checkbox ' + className + '"><input id="' + id + '" ' + is_checked +' ' + extraAttibutes + ' type="checkbox">' + label + '</div>';
		},
		
		/* Нарисовать кастомный селект */
		c_select : 
		{
			selectors : {},
			draw : function( id, options )
			{
				var s = '<div class="sio-pfs-color-selector">'
				var _ap = '';
				var _av = '';
				var _opts = '';
				
				this.selectors[id] = {'options' : options};
				
				siomap( function( opt )
				{
					if( opt.is_active )
					{
						var is_a_c = "active-option";
						_ap = opt.label;
						_av = opt.value;
					}
					else
					{
						var is_a_c = "";
					}
					
					_opts += '<div id="sio_cselect_elt_' + opt.value + '" onclick="sio.preferences.c_select.select(\'' + id + '\',\'' + opt.value + '\',\'' + opt.label + '\');" class="sio-pfs-color-selector-option ' + is_a_c + '">' + opt.label + '</div>';
					
				}, options );
				
				s += '<div class="sio-pfs-color-selector-active" id="sio_cselect_active_label_' + id + '" onclick="sio.preferences.c_select.show_list(\'' + id + '\');">' + _ap + '</div><input type="hidden" id="sio_cselect_active_value_' + id + '" value="' + _av + '"/>';
				s += '<div class="options" id="sio_cselect_' + id + '_options">';
				
				s += _opts;
				
				s += '</div></div>';
				
				return s;
				
			},
			hide_active : function()
			{
				if( this.list_shown == true )
				{
					this.list_shown = false;
					return false;
				}
				if( typeof( this.active_list ) == 'undefined' ) return false;
				ge('sio_cselect_' + this.active_list + '_options').style.display = 'none';
			},
			show_list : function( id )
			{
				if( this.active_list ) ge('sio_cselect_' + this.active_list + '_options').style.display = 'none';
				this.list_shown = true;
				this.active_list = id;
				var list_div = ge('sio_cselect_' + id + '_options');
				list_div.style.display = 'block';
				addClass('sio_cselect_' + id + '_options', 'sio-active-list');
			},
			select : function( id, active_value, active_label )
			{
				
				var list_div = ge('sio_cselect_' + id + '_options');
				list_div.style.display = 'none';
				removeClass('sio_cselect_' + id + '_options', 'sio-active-list');
				
				removeClass('sio_cselect_elt_' + ge('sio_cselect_active_value_' + id).value, 'active-option');
				addClass('sio_cselect_elt_' + active_value, 'active-option');
				
				ge('sio_cselect_active_label_' + id).innerHTML = active_label;
				
				ge('sio_cselect_active_value_' + id).value = active_value;
				
				var _ss = sio.preferences.c_select.selectors[id].options;
				
				for( var i in _ss )
				{
					if( _ss[i].value == active_value )
					{
						if( typeof( _ss[i].callback ) == 'function' ) _ss[i].callback( active_value );
					}
				}
				
			}
		},
		
		/* Скрыть / показать блок с настройками */
		toggle_block : function( block_id )
		{
			
			// TODO : надобэ запилить фичу: при повторном нажатии на активный блок сворачивать все настройки к чертям
			
			if( typeof( sio.preferences[block_id].before_active ) != 'undefined' ) sio.preferences[block_id].before_active();
			
			siomap( function( x )
			{
				hide('sio_pfs_block_' + x);
				removeClass('sio_pfs_tab_' + x, 'sio-active-tab');
			}, preferences.prefs );
			
			hide('sioPfsIntroText');
			
			addClass('sio_pfs_tab_' + block_id, 'sio-active-tab');
			show( 'sio_pfs_block_' + block_id );
			
		},
		
		/*
			Функция инициализации настроек
		*/
		
		init : function()
		{
			
			// подключаем колорпикер
			var script_tag = ce('script', {type:'text/javascript', src: config.sio_host + '/static/js/colorpicker.js'});
			ge_tag('head')[0].appendChild( script_tag );
			
			// если у нас уже существует контейнер то просто показать
			if( ge('sio_spf') != null )
			{
				show( ge('sio_spf') );
				return false;
			}
			
			bind(window, 'click', function()
			{
				sio.preferences.c_select.hide_active();
			});
			
			sio.is_preferences_active = true;
			
			// TODO : реализовать предупреждение юзера в случае, если были сделаны какие-то настройки, и он нажал на закрытие окна
			// window.onbeforeunload = closeEditorWarning;
			
			// Готовим html для окна с настройками
			var _tabs = '',
					_blocks = '';
			
			siomap(function(x)
			{
				var _block_id = 'sio_pfs_block_' + x;
				var _block_content = preferences[x].draw();
				_tabs += '<div class="sio-tab" id="sio_pfs_tab_' + x + '" onclick="sio.preferences.toggle_block(\'' + x + '\')">' + transl(x) + '</div>';
				_blocks += '<div class="sio-block sio-block-' + _block_id + '" id="' + _block_id + '">' + _block_content + '</div>';
				
			}, preferences.prefs);
			
			_blocks += '<div id="sioPfsIntroText" class="sio-block" style="display: block;"><div class="sio-pfs-text">' + transl('preferences_intro') + '</div></div>';
			
			//disableSelection//(_spc);
			
			// Создать контейнер для настроек и запилить в него подготовленный html
			var _spf_container = ce('div',{'class':'sio-preferences-container','id':'sio_spf'});
			_spf_container.innerHTML = '<div class="sio-pfs-tabs"><div class="sio-logo sio-pfs-left-column"><a href="" onclick="sio.preferences.hide(); return false;"></a></div>' +
																	_tabs +
																	'</div>'+
																 '<div class="sio-pfs-blocks"><div class="sio-pfs-left-column">'+
																 '<div id="sio_save_button" class="sio-pfs-save-button" onclick="sio._set_domain_data(); return false;"><a href="" onclick="return false;">' + transl( 'save_label' ) + '</a></div>' +
																 '<div id="sio_reset_button" class="sio-pfs-reset-button"><a href="" onclick="return false;">' + transl( 'reset_label' ) + '</a></div>' +
																 '</div>' + _blocks + '</div>';
			
			ge_tag( 'body' )[0].appendChild( _spf_container );
			
			bind(ge('sio_search_window'), 'click', function( event )
			{
				event.stopPropagation();
			});
			
			disableSelection(_spf_container);
			
		}
		
	}
	sio.preferences = preferences;
	{% endif %}
	
	// Если имеет место быть быстрая установка — отрендерить клиенту необходимые окна
	if( _if_render_installer() === true ) _qi_complete();
	
	search.init();
	window.sio = sio;
	
})();
