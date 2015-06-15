@(delim: String)

@* Код js unbind для модели m.sc.FocusedAdsSearchArgs для js-роутера. *@

@import stuff.m._
@import io.suggest.ad.search.AdSearchConstants._

@after = {
  add("@LAST_PROD_ID_FN");
}

@adSearchJsUnbindTpl(delim, Some(after))
