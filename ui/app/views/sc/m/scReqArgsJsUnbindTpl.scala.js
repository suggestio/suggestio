@(delim: String)

@* Код функции для ScReqArgs qsb.javascriptUnbind().
   Она нужна для разбиндивания модели из JSON на стороне js. *@

@import io.suggest.sc.ScConstants.ReqArgs._
@import views.js.stuff.m._

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@GEO_FN");
    add("@LOC_ENV_FN");
    add("@SCREEN_FN");
    add("@WITH_WELCOME_FN");
    add("@PREV_ADN_ID_FN");
    add("@ADN_ID_FN");
    add("@VSN_FN", true);
  }
}
