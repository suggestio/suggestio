@()(content: JavaScript)

@* common-утиль для поддержки разбиндивания js-объектов. *@

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

@content

return s;
