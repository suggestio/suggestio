@(name: String = io.suggest.routes.JsRoutesConst.GLOBAL_NAME, csrfAll: Boolean = false,
  cdn: Boolean = false)(routes: play.api.routing.JavaScriptReverseRoute*)(implicit ctx: Context)

@* JS-шаблон генерации play js reverse router.
   Синтаксис вызова шаблона аналогичен вызову helper.javascriptRoutes().
   csrfAll: true заставляет все запросы подписывать по CSRF, а не только POST`ы [false].
   cdn: Если true, то роутер будет пытаться проводить через CDN все URL, начинающиеся с ~. [false]
 *@

@import io.suggest.common.qs.QsConstants._
@import org.apache.commons.text.StringEscapeUtils.{ escapeEcmaScript => esc }
@import ctx.request
@import ctx.api.ctxUtil.HTTPS_ENABLED
@import ctx.api.cdn.ctx2CdnHost
@import io.suggest.common.empty.OptionUtil

"use strict";

var @(name) = {};
(function(_root){

  @* Этот код примерно как-то скопипасчен из play JSRR. Хз, как именно работает, т.к. местами минифицирован до непонятности. *@
  var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
  var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
  var _s = function(p,s){return p + @if(HTTPS_ENABLED){ ((s===true||(s&&s.secure))?'s':'') + }'://'}
  var hostEsc = '@esc( request.host )';

  @* 2016.dec.14: Запилена поддержка CSRF в JsRoutes для POST-запросов.
   * Это безопасно, когда js-роутер заинлайнен в html-страницу.
   * Если js-роутер раздаётся напрямую (как js-ответ), то CSRF тут не должно быть вообще никогда: это дыра в CSRF будет.
   *@
  var csrfQs = @JavaScript( play.filters.csrf.CSRF.getToken.fold("undefined")(t => s"'${t.name}=${t.value}'") );
  var csrfQsExist = typeof csrfQs == "string";

  @*
   * Функция _wA() вызывается напрямую из генерируемых роут (захардкожена в route.f):
   * _wA({method:"GET", url:"/" + "sc/fads" + _qS([(_o2qs)("a", a0)])})
   *@
  var _wA = function(r) {
    var method = r.method;
    var url = r.url;
    var viaCdn = url.startsWith("/~");
    @* Выставление CSRF-токена: влияет наличие CSRF в реквесте, method или значение csrfAll,
     * или наличие '/~' в начале URL (viaCdn) в качестве принудительного глобального запрета CSRF для роуты. *@
    if (csrfQsExist && !viaCdn@if(!csrfAll){ && (method == "POST" || method == "DELETE")}) {
      var delim;
      var qmark = '?';
      if (url.indexOf(qmark) >= 0) {
        delim = '&';
      } else {
        delim = qmark;
      }
      url = url + delim + csrfQs;
    }
    @* Роутинг GET-запросов, начинающихся с /~, должен идти через CDN. *@
    var absUrlHost;
    @OptionUtil.maybeOpt(cdn)(ctx2CdnHost).fold {
      absUrlHost = hostEsc;
    } { cdnHost =>
      if (viaCdn && method == "GET") {
        absUrlHost = '@cdnHost';
      } else {
        absUrlHost = hostEsc;
      }
    }

    @* Если запрос через CDN, то надо и относительный url подменять на CDN URL. *@
    var relUrl;
    if (absUrlHost != hostEsc) {
      relUrl = '//'+absUrlHost+url;
    } else {
      relUrl = url;
    }

    return {
      method: method,
      url: relUrl,
      absoluteURL: function(s){return _s('http',s)+absUrlHost+url},
      webSocketURL: function(s){return _s('ws',s)+absUrlHost+url}
    }
  }

  @* Код сериализация JSON object-ов в qs-строку, портированный из _objQsbTpl. *@
  var _d = "@KEY_PARTS_DELIM_STR";
  var @JSRR_OBJ_TO_QS_F = function(k,v) {
    @* Если пришла строка с префиксом ~~~~, то сразу просто возвращаем эту строку, игноря ключ. Это нужно для безопасного проброса signed qs. *@
    var dntP = "@DO_NOT_TOUCH_PREFIX";
    if (typeof v == "string" && v.startsWith(dntP)) {
      return v.substring( dntP.length );
    }

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
            kp1, @* //2018-06-08 Без encodeURIComponent, т.к. ломает имена c индексами [1] *@
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
