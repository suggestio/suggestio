@()

@* Код функции для ScReqArgs qsb.javascriptUnbind().
   Она нужна для разбиндивания модели из JSON на стороне js. *@

@import io.suggest.sc.ScConstants.ReqArgs._

function(k, v) {
  var s = "";

  @* Кешируем %-закодированный ключ-префикс qs-аргумента в константе: *@
  var ke = encodeURIComponent(k + ".");

  @* Для упрощения вызова конкатенации, используем эту функцию. *@
  var add = function(kk, withoutAmp) {
    var vv = v[kk];
    if (typeof vv != 'undefined' && vv != null) {
      var suf;
      if (!withoutAmp) {
        suf = "&";
      } else {
        suf = "";
      }
      s = s.concat(ke, encodeURIComponent(kk), "=", encodeURIComponent(vv), suf)
    }
  }

  add("@GEO");
  add("@SCREEN");
  add("@WITH_WELCOME");
  add("@VSN", true);

  return s;
}
