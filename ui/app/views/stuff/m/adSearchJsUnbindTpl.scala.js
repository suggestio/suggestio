@(delim: String, after: Option[JavaScript] = None)

@* Код функции для qsb.javascriptUnbind() для AdSearch. *@

@import io.suggest.ad.search.AdSearchConstants._
@import io.suggest.sc.ScConstants.ReqArgs.VSN_FN

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@PRODUCER_ID_FN");
    add("@LEVEL_ID_FN");
    add("@FTS_QUERY_FN");
    add("@RESULTS_LIMIT_FN");
    add("@RESULTS_OFFSET_FN");
    add("@RECEIVER_ID_FN");
    add("@FIRST_AD_ID_FN");
    add("@GENERATION_FN");
    add("@GEO_MODE_FN");
    add("@SCREEN_INFO_FN");
    add("@WITHOUT_IDS_FN");
    add("@AGP_POINT_FN");
    @after
    add("@VSN_FN", true);
  }
}
