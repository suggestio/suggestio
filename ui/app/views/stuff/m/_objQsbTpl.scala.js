@(delim: String)(content: JavaScript)

@* common-утиль для поддержки разбиндивания js-объектов. *@

var s = "";

@* Кешируем %-закодированный ключ-префикс qs-аргумента в константе: *@

var d = "@delim";
@* Для упрощения вызова конкатенации, используем эту функцию.
   kk являет собой суффикс ключа параметра в qs (без разделителя).
   withoutAmp - не добавлять после значения какой-либо разделитель? *@
var add = function(kk, withoutAmp, kPref, v1) {
  var kPref2 = kPref || k
  var v2 = v1 || v;
  var vv = v2[kk];
  if (typeof vv != 'undefined' && vv != null) {
    if (typeof vv === 'object') {
      @* object означает, что нужно сериализовать в список под-параметров согласно именам полей. http://stackoverflow.com/a/2869372 *@
      for (var kx in vv) {
        @* skip loop if the property is from prototype *@
        if (vv.hasOwnProperty(kx)) {
          var kPref3 = kPref2.concat(d, kk);
          add(kx, false, kPref3, vv);
        }
      }
    } else {
      var suf;
      if (!withoutAmp) {
        suf = "&";
      } else {
        suf = "";
      }
      s = s.concat(encodeURIComponent(kPref2.concat(d, kk)), "=", encodeURIComponent(vv), suf);
    }
  }
}

@content

return s;
