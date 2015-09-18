@(delim: String)

@* Код функции для ScReqArgs qsb.javascriptUnbind().
   Она нужна для разбиндивания модели из JSON на стороне js. *@

@import io.suggest.sc.TagSearchConstants.Req._
@import views.js.stuff.m._

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@FACE_FTS_QUERY_FN");
    add("@LIMIT_FN");
    add("@OFFSET_FN", true)
  }
}
