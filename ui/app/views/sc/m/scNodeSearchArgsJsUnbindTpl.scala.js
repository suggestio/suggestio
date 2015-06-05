@(delim: String)

@* Код функции для ScReqArgs qsb.javascriptUnbind().
   Она нужна для разбиндивания модели из JSON на стороне js. *@

@import io.suggest.sc.NodeSearchConstants._
@import views.js.stuff.m._

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@FTS_QUERY_FN");
    add("@GEO_FN");
    add("@OFFSET_FN");
    add("@LIMIT_FN");
    add("@CURR_ADN_ID_FN");
    add("@NODE_SWITCH_FN");
    add("@WITH_NEIGHBORS_FN");
    add("@API_VSN_FN", true);
  }
}
