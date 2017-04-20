@(name: String = io.suggest.js.JsRoutesConst.GLOBAL_NAME,
  csrfAll: Boolean = false)(routes: play.api.routing.JavaScriptReverseRoute*)(implicit ctx: Context)

@* JS-шаблон генерации play js reverse router.
   Синтаксис вызова шаблона аналогичен вызову helper.javascriptRoutes().
   csrfAll: true заставляет все запросы подписывать по CSRF, а не только POST`ы [false].
 *@

@import io.suggest.common.qs.QsConstants._
@import org.apache.commons.lang3.StringEscapeUtils.{ escapeEcmaScript => esc }
@import ctx.request
@import ctx.api.ctxUtil.HTTPS_ENABLED

"use strict";

var @(name) = {};
(function(_root){

  @* Этот код примерно как-то скопипасчен из play JSRR. Хз, как именно работает, т.к. местами минифицирован до непонятности. *@
  var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
  var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
  var _s = function(p,s){return p + @if(HTTPS_ENABLED){ ((s===true||(s&&s.secure))?'s':'') + }'://'}
  var hostEsc = '@esc( request.host )';

  @* 2016.dec.14: Запилена поддержка CSRF в JsRoutes для POST-запросов. *@
  var csrfQs = @JavaScript( play.filters.csrf.CSRF.getToken.fold("undefined")(t => s"'${t.name}=${t.value}'") );

  var _wA = function(r) {
    var method = r.method;
    var url;
    if (typeof csrfQs == "string"@if(!csrfAll){ && method == "POST"}) {
      var delim;
      var qmark = '?'
      if (r.url.indexOf(qmark) >= 0) {
        delim = '&';
      } else {
        delim = qmark;
      }
      url = r.url + delim + csrfQs;
    } else {
      url = r.url;
    }
    return {
      method: method,
      url: url,
      absoluteURL: function(s){return _s('http',s)+hostEsc+url},
      webSocketURL: function(s){return _s('ws',s)+hostEsc+url}
    }
  }

  @* Код сериализация JSON object-ов в qs-строку, портированный из _objQsbTpl. *@
  var _d = "@KEY_PARTS_DELIM_STR";
  var @JSRR_OBJ_TO_QS_F = function(k,v) {
    @* Аккамулятор строки-результата. Лучше сделать списком или каким-то string builder-ом. *@
    var a = "";

    @* kp1 = key prefix: x, x.a, x.a.e, etc...
       v1 = value *@
    var f = function(kp1, v1) {
      var v1t = typeof v1;
      if (v1t != 'undefined' && v1 != null) {

        if (v1t === 'object') {
          if (Array.isArray(v1) && v1.length > 0) {
            @* Это непустой массив. Нужно добавить квадратных скобочек перед текущим qs-ключом. *@
            v1.forEach(function(e, index, arr) {
              var kp2 = kp1.concat('@QS_KEY_INDEX_PREFIX', index, '@QS_KEY_INDEX_SUFFIX');
              f(kp2, e);
            });

          } else if (v1 != {}) {
            @* Это непустой объект. В него надо залезать и сериализовать согласно именам полей.
               http://stackoverflow.com/a/2869372 *@
            for (var v1k in v1) {
              @* skip loop if the property is from prototype *@
              if (v1.hasOwnProperty(v1k)) {
                var kp2 = kp1.concat(_d, v1k);
                f(kp2, v1[v1k]);
              }
            }
          }

        } else {
          @* Это значения. Сериализовать в текущем qs-ключе. *@
          var ap;
          if ( a.length > 0 ) {
            ap = '&';
          } else {
            ap = '';
          }
          a = a.concat(
            ap,
            encodeURIComponent(kp1),
            '=',
            encodeURIComponent(v1)
          );
        }

      }
    }

    f(k, v);

    return a;
  }

  @for( route <- routes ) {
    @defining( route.name.split('.') ) { nameParts =>
      _nS('@esc( nameParts.dropRight(1).mkString(".") )');
      _root@for(np <- nameParts){['@esc(np)']} = @JavaScript(route.f);
    }
  }
})(@name);
