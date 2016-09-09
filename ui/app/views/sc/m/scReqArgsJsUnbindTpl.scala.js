@(delim: String)

@* Код функции для ScReqArgs qsb.javascriptUnbind().
   Она нужна для разбиндивания модели из JSON на стороне js. *@

@import io.suggest.sc.ScConstants.ReqArgs._
@import views.js.stuff.m._

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@GEO");
    add("@SCREEN");
    add("@WITH_WELCOME");
    add("@PREV_ADN_ID_FN");
    add("@ADN_ID_FN");
    add("@VSN", true);
  }
}
