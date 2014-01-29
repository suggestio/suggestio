





	
(function() {
	
	var sio = {};

	
	/* Локализация */
	var langs = 
	{
		'en' : 
		{
			'before_unload_message' : 'Unsaved changes! Press &laquo;Save&raquo; button before quit!',
			'save_label' : 'Save',
			'saved_label' : 'Saved!',
			'reset_label' : 'Reset',
			'preferences_intro' : '<p>Here you can setup all needed stuff</p><p>Before you start, please, type in search request for better admin experience.</p>',
			'search_field' : 'Search field',
			'search_layout' : 'Search layout',
			'colors' : 'Colors',
			'dimensions' : 'Dimensions',
			'search_preferences' : 'Search preferences',
			'use_default_field' : 'Use default field',
			'use_custom_field' : 'Use great custom field',
			'use_drop_down_window' : 'Use drop down search',
			'use_great_t_style' : 'Use awesome t-style search',
			'color_preferences' : 'Color preferences',
			'sf_bg_color' :'',
			'sf_text_color' : '',
			'border_color' :'Border color',
			'inner_border_color' :'Inner border color',
			'bg_color' :'Background color',
			'title_color' :'Title color',
			'desc_color' :'Description color',
			'link_color' :'Link color',
			'outer_border_color' : 'Outer border color',
			'save_changes' : 'Save changes',
			'column_bg' : 'Column background',
			'result_bg' : 'Result background',
			'highlight_bg' : 'Highlight background',
			'open_results_in' : 'Open found pages in',
			'interface_lang' : 'Interface language',
			'show_images_in_sr_label' : 'Show images in search results?',
			'yes' : 'Yes',
			'no' : 'No',
			'current_window' : 'Current window',
			'blank_window' : 'Blank window'
		},
		'ru' :
		{
			'before_unload_message' : '\u0412\u043d\u0435\u0441\u0435\u043d\u043d\u044b\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f \u043d\u0435 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u044b! \u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043a\u043d\u043e\u043f\u043a\u0443 "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c" \u043f\u0440\u0435\u0436\u0434\u0435 \u0447\u0435\u043c \u0437\u0430\u043a\u0440\u044b\u0442\u044c \u043e\u043a\u043d\u043e!',
			'save_label' : '\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c',
			'saved_label' : 'Ok!',
			'reset_label' : '\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c',
			'preferences_intro' : '<p>\u0412 \u0434\u0430\u043d\u043d\u043e\u043c \u0440\u0435\u0434\u0430\u043a\u0442\u043e\u0440\u0435 \u0432\u044b \u043c\u043e\u0436\u0435\u0442\u0435 \u043d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0432\u0441\u0435 \u043d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u044b\u0435 \u043f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b \u043f\u043e\u0438\u0441\u043a\u0430.<\/p><p>\u041f\u0435\u0440\u0435\u0434 \u043d\u0430\u0447\u0430\u043b\u043e\u043c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438, \u043f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430, \u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u044b\u0439 \u0437\u0430\u043f\u0440\u043e\u0441 \u0434\u043b\u044f \u0431\u043e\u043b\u0435\u0435 \u043d\u0430\u0433\u043b\u044f\u0434\u043d\u043e\u0439 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438.<\/p>',
			'search_field' : '\u041f\u043e\u043b\u0435 \u043f\u043e\u0438\u0441\u043a\u0430',
			'search_layout' : '\u0428\u0430\u0431\u043b\u043e\u043d',
			'colors' : '\u0426\u0432\u0435\u0442\u0430',
			'dimensions' : '\u0420\u0430\u0437\u043c\u0435\u0440\u044b',
			'search_preferences' : '\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u043f\u043e\u0438\u0441\u043a\u0430',
			'use_default_field' : '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u043f\u043e\u043b\u0435 \u043d\u0430 \u0441\u0430\u0439\u0442\u0435',
			'use_custom_field' : '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u043a\u0440\u0430\u0441\u0438\u0432\u0443\u044e \u0438\u043a\u043e\u043d\u043a\u0443',
			'use_drop_down_window' : '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0432\u044b\u043f\u0430\u0434\u0430\u044e\u0449\u0435\u0435 \u043e\u043a\u043d\u043e \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u043e\u0432',
			'use_great_t_style' : '\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u043e\u043a\u043d\u043e \u0422-\u0441\u0442\u0438\u043b\u044f',
			'color_preferences' : '\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0446\u0432\u0435\u0442\u0430',
			'sf_bg_color' :'\u0426\u0432\u0435\u0442 \u0444\u043e\u043d\u0430 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u043f\u043e\u043b\u044f',
			'sf_text_color' : '\u0426\u0432\u0435\u0442 \u0442\u0435\u043a\u0441\u0442\u0430 \u043f\u043e\u0438\u0441\u043a\u043e\u0432\u043e\u0433\u043e \u043f\u043e\u043b\u044f',
			'border_color' :'\u0426\u0432\u0435\u0442 \u043e\u0431\u0432\u043e\u0434\u043a\u0438',
			'inner_border_color' :'\u0426\u0432\u0435\u0442 \u0432\u043d\u0443\u0442\u0440\u0435\u043d\u043d\u0435\u0439 \u043e\u0431\u0432\u043e\u0434\u043a\u0438',
			'bg_color' :'\u0426\u0432\u0435\u0442 \u0444\u043e\u043d\u0430',
			'title_color' :'\u0426\u0432\u0435\u0442 \u0437\u0430\u0433\u043e\u043b\u043e\u0432\u043a\u0430',
			'desc_color' :'\u0426\u0432\u0435\u0442 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u044f',
			'link_color' :'\u0426\u0432\u0435\u0442 \u0441\u0441\u044b\u043b\u043a\u0438',
			'outer_border_color' : '\u0426\u0432\u0435\u0442 \u0432\u043d\u0435\u0448\u043d\u0435\u0439 \u043e\u0431\u0432\u043e\u0434\u043a\u0438',
			'save_changes' : '\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438',
			'column_bg' : '\u0424\u043e\u043d \u043a\u043e\u043b\u043e\u043d\u043a\u0438',
			'result_bg' : '\u0424\u043e\u043d \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430',
			'highlight_bg' : '\u0426\u0432\u0435\u0442 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430',
			
			'open_results_in' : '\u041e\u0442\u043a\u0440\u044b\u0432\u0430\u0442\u044c \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u044b \u043f\u043e\u0438\u0441\u043a\u0430 \u0432:',
			'interface_lang' : '\u042f\u0437\u044b\u043a \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430',
			'show_images_in_sr_label' : '\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f \u0432 \u0440\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u0430\u0445?',
			'yes' : '\u0414\u0430',
			'no' : '\u041d\u0435\u0442',
			
			'current_window' : '\u0422\u0435\u043a\u0443\u0449\u0435\u043c \u043e\u043a\u043d\u0435',
			'blank_window' : '\u041d\u043e\u0432\u043e\u043c \u043e\u043a\u043d\u0435'
		}
	}
	
	var transl = function( what )
	{
		var _l = sio.domain_data ? sio.domain_data.lang : config.lang;
		if( !langs[_l][what] )
		{
			return what;
		}
		else
		{
			return langs[config.lang][what];
		}
	}
	
	/* Шаблоны */
	/* Тут нужен адовый рефакторинг например */
	var templates = 
	{
		// Сгенерить цсс для иконки поиска
		generate_sf_css : function( tpl, _dc )
		{
			
			var _sf_o = templates.search_fields[tpl];
			
			if( is_retina() == true )
			{
				var _dms = _sf_o.gen_params['ds_retina'].split('x');
				return '.sio-custom-icon { width: ' + _dms[0]/2 + 'px!important; height: ' + _dms[1]/2 + 'px!important; background: url(\'https://suggest.io/static/images_generated/' + _dc + '-' + tpl + '-retina.png?v=1\') no-repeat!important; background-size: ' + _dms[0]/2 + 'px ' + _dms[1]/2 + 'px!important; }';
			}
			else
			{
				var _dms = _sf_o.gen_params['ds'].split('x');
				return '.sio-custom-icon { width: ' + _dms[0] + 'px!important; height: ' + _dms[1] + 'px!important; background: url(\'https://suggest.io/static/images_generated/' + _dc + '-' + tpl + '.png?v=1\') no-repeat!important; }';
			}
				
		},
		search_fields :
		{
			'crnr-1':{
				gen_params : {'prefix':'crnr-1','ds':'34x34','ds_retina':'66x66'}
			},
			'crnr-2':{
				gen_params : {'prefix':'crnr-2','ds':'37x37','ds_retina':'82x82'}
			},
		//	'crnr-3':{
		//		gen_params : {'prefix':'crnr-3','ds':'37x37','ds_retina':'72x72'}
		//	},
			'flag-1':{
				gen_params : {'prefix':'flag-1','ds':'26x36','ds_retina':'52x72'}
			},
			'flag-2':{
				gen_params : {'prefix':'flag-2','ds':'28x42','ds_retina':'54x82'}
			},
			'flag-3':{
				gen_params : {'prefix':'flag-3','ds':'27x35','ds_retina':'52x70'}
			},
			'flag-4':{
				gen_params : {'prefix':'flag-4','ds':'28x44','ds_retina':'54x92'}
			},
			'flag-5':{
				gen_params : {'prefix':'flag-5','ds':'23x37','ds_retina':'48x74'}
			},
			
			'flag-6':{
				gen_params : {'prefix':'flag-6','ds':'18x46','ds_retina':'34x88'}
			},
			'flag-7':{
				gen_params : {'prefix':'flag-7','ds':'22x40','ds_retina':'44x80'}
			},
			'flag-8':{
				gen_params : {'prefix':'flag-8','ds':'34x32','ds_retina':'68x64'}
			},
			'flag-9':{
				gen_params : {'prefix':'flag-9','ds':'24x28','ds_retina':'48x56'}
			}
		},
		drop_down_windows :
		{
			'default' :
			{
				css : '.sio-search-window { margin-left: <<window_margin>>px!important; margin-top: <<window_margin_top>>px!important; width: <<window_width>>px!important; } .sio-sw-cont.default .sio-sw-search-results { max-height: <<window_height>>px!important; } .sio-sw-cont.default .sio-sw-search-results { background-color: #<<bg_color>>!important; } .sio-sw-cont.default .sio-sw-inner, .sio-sw-cont.default .sio-result { border-color : #<<border_color>>!important; } .sio-sw-cont.default .sio-result-title { color: #<<title_color>>!important; } .sio-sw-cont.default .sio-result-desc { color: #<<desc_color>>!important; } .sio-sw-cont.default .sio-result-link { color: #<<link_color>>!important; } .sio-sw-cont.default em { background: #<<highlight_color>>!important; }',
				params : {'colors' :
									[{p:'bg_color',n:	transl('bg_color'),d:'ffffff'},
									 {p:'border_color',n:transl('border_color'),d:'ececec'},
									 {p:'title_color',n:transl('title_color'),d:'0f0f0f'},
									 {p:'desc_color',n:transl('desc_color'),d:'868686'},
									 {p:'highlight_color',n:transl('highlight_bg'),d:'none',z:true},
									 {p:'link_color',n:transl('link_color'),d:'5bb6d5'}],
									'dimensions':
										[{p:'window_width',n:'window_width',d:600},
										 {p:'window_margin',n:'window_margin',d:0},
										 {p:'window_margin_top',n:'window_margin_top',d:0},
										 {p:'window_height',n:'window_height',d:500}
										]
									},
				thumbnail : 'sio-dd-ololo-preview.png'
			},
			'fatborder' : 
			{
				css : '.sio-search-window { margin-left: <<window_margin>>px!important; margin-top: <<window_margin_top>>px!important; width: <<window_width>>px!important; } .sio-sw-cont.fatborder .sio-sw-search-results { max-height: <<window_height>>px!important; } .sio-sw-cont.fatborder .sio-sw-inner, .sio-sw-cont.fatborder .sio-sw-ads { border-color: #<<stroke_color>>!important; background-color: #<<second_stroke_color>>!important; } .sio-sw-cont.fatborder .sio-sw-search-results, .sio-sw-ads a { background-color: #<<bg_inner>>!important; } .sio-sw-cont.fatborder .sio-result-title { color: #<<title_color>>!important; } .sio-sw-cont.fatborder .sio-result-desc { color: #<<desc_color>>!important; } .sio-sw-cont.fatborder em { color: #<<highlight_bg>>!important; } .sio-sw-cont.fatborder .sio-result-link { color: #<<link_color>>!important; }',
				params : {'colors' :[{p:'stroke_color',n:transl('border_color'),d:'1f2b2d'},
														 {p:'second_stroke_color',n:transl('inner_border_color'),d:'ffffff'},
														 {p:'bg_inner',n:transl('bg_color'),d:'1f2b2d'},
														 {p:'title_color',n:transl('title_color'),d:'ffffff'},
														 {p:'desc_color',n:transl('desc_color'),d:'cccccc'},
														 {p:'highlight_bg',n:transl('highlight_bg'),d:'ffffff'},
														 {p:'link_color',n:transl('link_color'),d:'5FB6D3'}],
														'dimensions':
										[{p:'window_width',n:'window_width',d:600},
										 {p:'window_margin',n:'window_margin',d:0},
										 {p:'window_margin_top',n:'window_margin_top',d:0},
										 {p:'window_height',n:'window_height',d:500}
										]
								},
				thumbnail : 'sio-dd-ololo-preview.png'
			},
			'plaintext' :
			{
				css : '.sio-search-window { margin-left: <<window_margin>>px!important; margin-top: <<window_margin_top>>px!important; width: <<window_width>>px!important; } .sio-sw-cont.plaintext .sio-sw-search-results { max-height: <<window_height>>px!important; } .sio-sw-cont.plaintext .sio-sw-inner { border-color:#<<border_color>>!important; } .sio-sw-cont.plaintext .sio-sw-inner-2, .sio-sw-cont.plaintext .sio-sw-ads { background-color: #<<bg_color>>!important; } .sio-sw-cont.plaintext .sio-result-title { color: #<<title_color>>!important; }  .sio-sw-cont.plaintext .sio-result-desc { color: #<<desc_color>>!important; }  .sio-sw-cont.plaintext .sio-result-link { color: #<<link_color>>!important; } .sio-sw-cont.plaintext em { color: #<<hightlight_bg>>!important; }',
				params : {'colors' : [{p:'bg_color',n:transl('bg_color'),d:'FFFFFF'},
															{p:'border_color',n:transl('border_color'),d:'A4A4A4'},
															{p:'title_color',n:transl('title_color'),d:'213845'},
															{p:'highlight_bg',n:transl('highlight_bg'),d:'478BA2'},
									 						{p:'desc_color',n:transl('desc_color'),d:'478BA2'},
									 						{p:'link_color',n:transl('link_color'),d:'478BA2'}],
									'dimensions':
										[{p:'window_width',n:'window_width',d:600},
										 {p:'window_margin',n:'window_margin',d:0},
										 {p:'window_margin_top',n:'window_margin_top',d:0},
										 {p:'window_height',n:'window_height',d:500}
										]
								 },
				thumbnail : 'sio-dd-ololo-preview.png'
			}
			/*
			'roundcorner' :
			{
				css : '.sio-search-window { margin-left: <<window_margin>>px!important; margin-top: <<window_margin_top>>px!important; width: <<window_width>>px!important; } .sio-sw-cont.roundcorner .sio-sw-search-results { max-height: <<window_height>>px!important; }',
				params : {'colors' :
									[{p:'bg_color',n:transl('border_color'),d:'213845'},
									 {p:'title_color',n:transl('title_color'),d:'000000'},
									 {p:'desc_color',n:transl('result_description_color'),d:'868686'},
									 {p:'link_color',n:transl('link_color'),d:'5bb6d5'},
									 {p:'highlight_color',n:transl('highlight_color'),d:'cddae6'}
									],
									'dimensions':
										[{p:'window_width',n:'window_width',d:600},
										 {p:'window_margin',n:'window_margin',d:0},
										 {p:'window_margin_top',n:'window_margin_top',d:0},
										 {p:'window_height',n:'window_height',d:500}
										]
									},
				thumbnail : 'sio-dd-ololo-preview.png'
			},
			'normalone' :
			{
				css : '.sio-search-window { margin-left: <<window_margin>>px!important; margin-top: <<window_margin_top>>px!important; width: <<window_width>>px!important; } .sio-sw-cont.normalone .sio-sw-ads { background: #<<border_color>>!important; } .sio-sw-cont.normalone .sio-sw-inner { border-color: #<<border_color>>!important; background-color: #<<inner_border_color>>!important; }',
				params : {'colors' :
									[{p:'border_color',n:transl('border_color'),d:'00728b'},
									 {p:'inner_border_color',n:transl('inner_border_color'),d:'003945'},
									 {p:'title_color',n:transl('title_color'),d:'5bb6d5'},
									 {p:'em_color',n:transl('highlight_color'),d:'478BA2'},
									 {p:'link_color',n:transl('link_color'),d:'5bb6d5'},
									 {p:'desc_color',n:transl('result_description_color'),d:'a4a4a4'}],
									'dimensions':
										[{p:'window_width',n:'window_width',d:600},
										 {p:'window_margin',n:'window_margin',d:0},
										 {p:'window_margin_top',n:'window_margin_top',d:0},
										 {p:'window_height',n:'window_height',d:500}
										]
									},
				thumbnail : 'sio-dd-ololo-preview.png'
			}
			*/
			
			
			
		},
		
		t_windows :
		{
			'sio-kk' :
			{
				css : '.sio-custom-field input, .sio-csf-label { line-height: <<font_size>>px!important; font-size: <<font_size>>px!important; } .sio-kk em { color: #<<highlight_bg>>!important; } .sio-t-column.sio-kk .sio-t-c-inner { background: <<column_bg>>!important; } .sio-t-c-inner { width: <<column_width>>px!important; margin-left: <<column_margin>>px!important; } .sio-custom-field input { color: #<<sf_text_color>>!important; } .sio-custom-field { background: #<<sf_bg>>!important; } .sio-kk .sio-result .sio-result-inner { background-color: #<<result_bg>>!important; } .sio-kk .sio-result-title { color: #<<result_title_color>>!important; } .sio-kk .sio-result-desc { color: #<<desc_color>>!important; } .sio-kk .sio-result-link { color: #<<link_color>>!important; }',
				params : {'colors' :
										[{p:'sf_bg',n:transl('sf_bg_color'),d:'c0c0c0'},
										 {p:'sf_text_color',n:transl('sf_text_color'),d:'ffffff'},
										 {p:'column_bg',n:	transl('column_bg'),d:'rgba(153,153,153,.95)',t:'rgba'},
										 {p:'result_bg',n:	transl('result_bg'),d:'ffffff'},
										 {p:'result_title_color',n: transl('title_color'),d:'000000'},
										 {p:'highlight_bg',n:transl('highlight_bg'),d:'000000'},
										 {p:'desc_color',n:	transl('desc_color'),d:'868686'},
										 {p:'link_color',n:	transl('link_color'),d:'5bb6d5'}
										],
									'dimensions':
										[{p:'column_width',n:'Column width',d:550,min:200,max:1000},
										 {p:'column_margin',n:'Column margin',d:-275,min:-1000,max:0},
										 {p:'font_size',n:'Font size',d:40}
										]
									},
				thumbnail : 'sio-kk-preview.png'
			},
			'sio-dd-def' :
			{
				css : '.sio-custom-field input, .sio-csf-label { line-height: <<font_size>>px!important; font-size: <<font_size>>px!important; } .sio-dd-def em { color: #<<highlight_bg>>!important; } .sio-dd-def .sio-result { border-color: #<<border_color>>!important; } .sio-t-column.sio-dd-def .sio-t-c-inner { background: <<column_bg>>!important; } .sio-t-c-inner { width: <<column_width>>px!important; margin-left: <<column_margin>>px!important; } .sio-dd-def .sio-result-title { color: #<<result_title_color>>!important; } .sio-custom-field input { color: #<<sf_text_color>>!important; } .sio-custom-field { background: #<<sf_bg>>!important; } .sio-dd-def .sio-result-desc { color: #<<desc_color>>!important; } .sio-dd-def .sio-result-link { color: #<<link_color>>!important; }',
				params : {'colors' :
										[{p:'sf_bg',n:transl('sf_bg_color'),d:'c0c0c0'},
										 {p:'sf_text_color',n:transl('sf_text_color'),d:'ffffff'},
										 {p:'column_bg',n:	transl('column_bg'),d:'rgba(0,0,0,.95)',t:'rgba'},
										 {p:'border_color',n:transl('border_color'),d:'0074ad'},
										 {p:'result_title_color',n: transl('title_color'),d:'2190af'},
										 {p:'highlight_bg',n:transl('highlight_bg'),d:'2190af'},
										 {p:'desc_color',n:	transl('desc_color'),d:'666666'},
										 {p:'link_color',n:	transl('link_color'),d:'666666'}
										],
									'dimensions':
										[{p:'column_width',n:'Column width',d:550,min:200,max:1000},
										 {p:'column_margin',n:'Column margin',d:-275,min:-1000,max:0},
										 {p:'font_size',n:'Font size',d:40}
										]
									},
				thumbnail : 'sio-dd-preview.png'
			},
			'sio-bb' :
			{
				css : '.sio-custom-field input, .sio-csf-label { line-height: <<font_size>>px!important; font-size: <<font_size>>px!important; } .sio-bb em { color: #<<highlight_bg>>!important; } .sio-t-column.sio-bb .sio-t-c-inner { background: <<column_bg>>!important; } .sio-bb .sio-result { background: <<result_bg>>!important; border-color: #<<border_color>>!important; } .sio-t-c-inner { width: <<column_width>>px!important; margin-left: <<column_margin>>px!important; }  .sio-custom-field input { color: #<<sf_text_color>>!important; } .sio-custom-field { background: #<<sf_bg>>!important; } .sio-bb .sio-result-title { background: #<<border_color>>!important; color: #<<title_color>>!important; } .sio-bb .sio-result-desc { color: #<<desc_color>>!important; } .sio-bb .sio-result-link { color: #<<link_color>>!important; }',
				params : {'colors' :
										 [{p:'sf_bg',n:transl('sf_bg_color'),d:'c0c0c0'},
										 {p:'sf_text_color',n:transl('sf_text_color'),d:'ffffff'},
										 {p:'border_color',n:	transl('border_color'),d:'2190af'},
										 {p:'column_bg',n:	transl('column_bg'),d:'rgba(0,0,0,.95)',t:'rgba'},
										 {p:'result_bg',n:	transl('result_bg'),d:'rgba(255,255,255,.95)',t:'rgba'},
										 {p:'title_color',n:	transl('title_color'),d:'ffffff'},
										 {p:'highlight_bg',n:transl('highlight_bg'),d:'ffffff'},
										 {p:'desc_color',n:	transl('desc_color'),d:'666666'},
										 {p:'link_color',n:	transl('link_color'),d:'666666'}
										],
									'dimensions':
										[{p:'column_width',n:'column_width',d:550,min:200,max:1000},
										 {p:'column_margin',n:'column_margin',d:-275,min:-1000,max:0},
										 {p:'font_size',n:'Font size',d:40}
										]
									},
				thumbnail : 'sio-bb-preview.png'
			}
		},
		
		t_style_def_tpl : 'sio-kk',
		drop_down_def_tpl : 'fatborder',
		drop_down_base_tpl : function( params )
		{
			return '<div class="sio-sw-cont ' + params.className + '">' +
						 	'<div class="sio-sw-inner">' +
						 		'<div class="sio-sw-inner-2">' +
						 			'<div class="sio-sw-search-results" id="sio_searchResults">' +
						 			'</div>' +
						 		'</div>' +
						 	'</div>' +
						 	'<div class="sio-sw-ads"><a href="https://suggest.io/" target="_blank"><img src="https://suggest.io/static/images2/sio-label-ads.png"></a></div>'
						 '</div>'
		}
	};
	sio.templates = templates;
	
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
	
	/* Является ли переданный объект массивом? */
	var is_array = function(o)
	{
		return Object.prototype.toString.call( o ) == '[object Array]';
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
	
	var _include_css = function( url )
	{
		var c = ce( 'link', {'rel':'stylesheet','type':'text/css','id':'sio_css', 'href' : url} );
		ge_tag('head')[0].appendChild( c );
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
		/* Инициализация поиска */
		/* если preferences_reveived == 1 — значит настройки получены, можно стартовать поиск */
		init : function( got_prefs )
		{
			
			if( typeof( document.getElementsByTagName('body')[0] ) == 'undefined' )
			{
				setTimeout(function()
				{
					sio.search.init();
				}, 100);
				return false;
			}
			
			var got_prefs = got_prefs || 0;
			
			// если настроек для домена у нас еще нет — запросить их, после чего вернуть false
			if( !got_prefs )
			{
				_get_domain_data();
				// заодно можно подключить css, чтобы время не тратить зря
				_include_css( config.sio_host + config.sio_css );
				return false;
			}
			
			// если настройки есть, значит все ок и можно жарить дальше
			// найдем поле поиска
			sio.search_field = this.get_search_field();
			
			/* проверяем, какой тип поиска надо заюзать
				 возможные варианты:
				 - в настройках явно указан тип
				 - в настройках пусто — 
			*/
			
			this.init_layout();
			
			sio.search._generate_custom_template_style();
			
		},
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
			/* Если нет — выбрать рандомное слово на странице и запилить из него поисковый запрос */
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
		/* Нарисовать выпадающее окошко */
		drop_down_search_window : {
			init: function()
			{
				if( ge('sio_search_window') != null ) return false;
				
				var sf = ce('div',{'class':'sio-search-window','id':'sio_search_window'});
				
				if ( typeof( sio.domain_data.drop_down_template ) != 'undefined' )
				{
					var cn = sio.domain_data.drop_down_template;
				}
				else
				{
					var cn = typeof( sio.domain_data.search_window ) != 'undefined' ? sio.domain_data.search_window : templates.drop_down_def_tpl;
				}
				sio.domain_data.drop_down_template = cn;
				
				sf.innerHTML = templates.drop_down_base_tpl({'className': cn });
				
				ge_tag('body')[0].appendChild( sf );
				this.set_position();
				
				bind(window, 'resize', function()
				{
					sio.search.drop_down_search_window.set_position();
				});
				
				bind(window, 'click', function()
				{
					sio.search.drop_down_search_window.set_position();
				});
				
				bind(window, 'click', function()
				{
					if( sio.is_preferences_active == true ) return false;
					sio.search.hideSearch();
				});
				
				bind(ge('sio_search_window'), 'click', function( event )
				{
					event.stopPropagation();
				});
				
				bind(sio.search_field, 'focus', function()
				{
					this.value = '';
					this.style.outline = 'none';
					sio.search.drop_down_search_window.set_position();
				});
				
				bind(sio.search_field, 'keydown', function()
				{
					sio.search.search_field_keydown_event();
				});
				
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
				if( typeof( sio.domain_data.t_style_template ) == 'undefined' ) sio.domain_data.t_style_template = templates.t_style_def_tpl;
				if( ge('sio_search_window') != null ) return false;
				var sf = ce('div',{'class':'sio-t-window','id':'sio_search_window'});
				
				var cn = typeof( sio.domain_data.t_style_template ) != 'undefined' ? sio.domain_data.t_style_template : templates.t_style_def_tpl;
				
				sf.innerHTML = '<div class="sio-t-column ' + cn + '"><div class="sio-t-c-inner" id="sio_searchResults"></div></div>';
				ge_tag('body')[0].appendChild( sf );
				
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
		// Функция поиска поискового поля на странице
		get_search_field : function()
		{
			
			// Если поле ввода в настройках не указано, или указано как default
			if( ( typeof( sio.domain_data.search_field ) != 'undefined' && sio.domain_data.search_field === 'default' ) || typeof( sio.domain_data.search_field ) == 'undefined' )
			{
				
				// Если у нас используется тешечаа — нужно кастомное поле
				if( typeof( sio.domain_data.search_layout ) != 'undefined' && sio.domain_data.search_layout != 'default' )
				{
					sio.domain_data.search_field = 'crnr-1';
					return this.get_search_field();
				}
				
				// ищем поле на странице
				var _field_on_page = this.locate_field_on_page();
				if( _field_on_page != null ) return _field_on_page;
				
				// если все же полей нету — запилить кастомное дефолтовое
				sio.domain_data.search_field = 'crnr-1';
				return this.get_search_field();
		
			
			}
			else
			{
				this.render_search_field();
				f = ge('sio_search_field');
				sio.csf = ge('sio_csf');
				
				return f;
				}
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
		// определить, является ли DOM объект поисковым полем
		is_search_field : function(e)
		{
			var search_pattern = new RegExp('search', "gi");
			var r = false;
			
			try
			{
				
				if( e.type == 'search') r = true;
				if( typeof( e.className ) != 'undefined' && e.className.match(search_pattern)) r = true;
				if( typeof( e.action ) != 'undefined' && e.action.match(search_pattern)) r = true;
				if( typeof( e.name ) != 'undefined' && e.name != '' && e.name.match(search_pattern)) r = true;
				if( typeof( e.id ) != 'undefined' && e.id.match(search_pattern)) r = true;
				if( typeof( e.placeholder ) != 'undefined' && e.placeholder.match(search_pattern)) r = true;
				
				if( typeof( e.value ) != 'undefined' && e.value.match(search_pattern)) r = true;
				
			}catch(err){}
			
			return r;
			
		},
		
		// функиця для отрисовки кастомного поля
		render_search_field : function( with_trigger )
		{
			if( ge('sio_csf') != null ) return false;
			
			// кастомное поле
			var sf = ce('div',{'class':'sio-custom-field','id':'sio_csf'});
			sf.innerHTML = '<div class="sio-csf-inner"><div class="sio-search-icon"></div><a class="lsbs-ads" href="https://suggest.io"></a><div id="sio_csf_label" class="sio-csf-label">\u0438\u0441\u043a\u0430\u0442\u044c...</div><div class="sio-close-search-icon"><a onclick="sio.search.hideSearch(); return false;" href=""></a></div><input type="text" id="sio_search_field"></div>';
			ge_tag('body')[0].appendChild( sf );
			
			// Кастомная иконка для поиска
			var si = ce('div',{'class':'sio-custom-icon sio-' + sio.domain_data.search_field,'id':'sio_csi'});
			si.innerHTML = '<div class="sio-csi"></div>';
			ge_tag('body')[0].appendChild( si );
			
			bind(ge("sio_csf_label"), "click", function()
			{
				ge('sio_csf_label').style.display = 'none';
				sio.search_field.focus();
			});
			
			bind(ge("sio_csi"), "click", function()
			{
				sio.search.showSearch();
			});
			
		},
		
		// функция для обработки нажатия клавиш
		search_field_keydown_event : function()
		{
			if( typeof( sio.search_timer ) != 'undefined' ) clearTimeout( sio.search_timer );
			sio.search_timer = setTimeout("sio.search.process_query()",config.searchRequestDelay);
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
		// отправить запрос на сервак
		sendRequest : function( request_url )
		{
			var script = document.createElement('script');
			script.type = 'text/javascript'
			script.src = request_url;
			script.id = 'sio_search_request';
			
			document.getElementsByTagName('head')[0].appendChild( script );
		},
		_s_add_result : function(data)
		{
			
			try
			{
				var query = data.q;
				var search_results = data.search_result;
				var status = data.status;
				
				if( status == 'found_none' )
				{
					//set_sio_window_state('found_none');
					
					//cache[query] = [];
					//query = query.length > 10 ? query.substring(0,10) + '...' : query;
					//var no_results_message =  config.no_results_label + '<span class="found-none-highlight sio-highlight">' + query + '</span>';
					
					//ge('sioNotFoundQ').innerHTML = query;
					hide("sio_search_window");
					
				}
				else
				{
					//set_sio_window_state('results');
					//cache[query] = search_results;
					show("sio_search_window");
					sio.search._draw_result( search_results, query );
				}
				
			}
				catch(err)
			{
				
			}
			
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
		
		
		return false;
		
		
	}
	
	var _qi_complete = function()
	{
		sio._qi_completed = true;
		
		
		
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
	
	
	
	// Если имеет место быть быстрая установка — отрендерить клиенту необходимые окна
	if( _if_render_installer() === true ) _qi_complete();
	
	search.init();
	window.sio = sio;
	
})();

