/* Special experiment */
/* TODO:
	- сделать, чтобы скролл на контейнере с результатами срабатывал, когда скролишь над полем ввода
	- доработать фичу с уменьшением кегля поля ввода в зависимости от числа введенных 
 */

$('#topSearchField').bind('focus', function()
{
	sio.init_suit();
});

var sio = 
{
	createElement : function ( tag, attributes, innerHTML )
	{
		new_elt = document.createElement( tag );
		for( var attr in attributes )
			new_elt.setAttribute(attr, attributes[attr]);
		
		if( innerHTML ) new_elt.innerHTML = innerHTML;
		return new_elt;
	},
	init_suit : function()
	{
		$('body').css({'overflow':'hidden'});
		var sio_suit = document.createElement('div');
		sio_suit.className = 'sio-suit';
		sio_suit.id="sio_suit";
		
		sio_suit.innerHTML = '<div class="sio-suit-header"><a onclick="$(\'#sio_suit\').remove(); return false;" class="sio-suit-close" href=""></a><div class="sio-header-info"><img src="/images/favicon.ico"/>Живой поиск по официальному сайту ЛДПР</div><div class="sio-suit-header-inner"><input id="sioSuitInput" type="text" class="sio-suit-input"/></div></div>'+
		
		'<div class="sio-results-container"><div class="right"><div id="sioResultsContainer"></div></div></div>';
		
		document.getElementsByTagName('body')[0].appendChild(sio_suit);
		
		$('#sioSuitInput').focus();
		$('#sioSuitInput').bind('keydown', function()
		{
			setTimeout(function()
			{
				if( $('#sioSuitInput').val() == '' )
					$('#sioSuitLabel').show();
					else
					$('#sioSuitLabel').hide();
				
				var search_request = $('#sioSuitInput').val().toLowerCase();
				$('#sioSuitInput').val( search_request.toUpperCase() );
				
				if(  search_request.length > 6 )
				{
					if(  search_request.length > 12 )
					{
						if(  search_request.length > 18 )
						{
						_cn = 'supersmall-letters';
						}else
						{
						_cn = 'extrasmall-letters';
						}
					}else
					{
						_cn = 'small-letters';
					}
					
				}
				else
				{
					_cn = 'large-letters';
				}
				
				document.getElementById('sioSuitInput').className = 'sio-suit-input ' +  _cn;
				
				do_search( search_request );
			}, 50);
		});	
	},
	sendRequest : function(url)
	{
		var script = document.createElement('script');
		script.type = 'text/javascript'
		script.src = url;
		script.id = 'sio_search_request';
		
		document.getElementsByTagName('head')[0].appendChild( script );
	},
	_s_add_result : function(data)
	{
		$('#sioResultsContainer').html('');
		var _result_html = '';
		for( var i in data['search_result'] )
		{
			var result_line = data['search_result'][i];
			
			if( !result_line['image_rel_url'] )
				_result_html += '<div class="sio-suit-search-result no-image-sr">'
				else
				_result_html += '<div class="sio-suit-search-result"><img width="180" src="https://suggest.io/' + result_line['image_rel_url'] + '"/>';
			
			_result_html += '<div class="sr-desc"><h1>' + result_line['title'] + '</h1><div class="desc">';
			
			_result_html += result_line['content_text'] + '</div></div><div class="clear"></div></div>';
		}
		
		$('#sioResultsContainer').html( _result_html );
		
	}
}

var do_search = function( request )
{
	
	var url = "https://suggest.io/search?h=ldpr.ru&q=" + request;
	sio.sendRequest( url );

}

var sioCss = sio.createElement( 'link', {'rel':'stylesheet','type':'text/css','id':'suggestioCss', 'href' : 'http://suggest.io/static/css/sio.suit.css'} );
document.getElementsByTagName('head')[0].appendChild( sioCss );
