var signinLink = document.getElementById('signin');

if (signinLink) {
	signinLink.onclick = function() {
  	user_initiated_login = true;
  	navigator.id.request();
		return false;
	}
}

var currentUser = null;

navigator.id.watch({
	loggedInUser: currentUser,
	onlogin: function(assertion) {
		
		$.ajax({
			type: 'POST',
			url: persona_conf.login_url, // This is a URL on your website.
			data: {assertion: assertion},
			success: function( d ) {
				
				if( typeof( user_initiated_login ) != 'undefined' && user_initiated_login == true )
				{
					admin_redirect();
				}else{
					
					if( $('#signinLink').length > 0)
					{
						signinLink.onclick = function()
						{
							admin_redirect();
							return false;
						};
					}
				}
			},
			error: function() {
				console.log("login failure");
			}
		});
	},
	onlogout: function() {
		$.ajax({
			type: 'POST',
			url: persona_conf.logout_url, // This is a URL on your website.
			success: function(res, status, xhr) { window.location.reload(); },
			error: function(xhr, status, err) { alert("logout failure" + res); }
		});
	}
});

var admin_redirect = function() { window.location = persona_conf.admin_location; };

