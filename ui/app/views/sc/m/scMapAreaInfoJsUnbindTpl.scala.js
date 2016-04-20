@(delim: String)

@* Код js unbind для модели m.sc.FocusedAdsSearchArgs для js-роутера. *@

@import stuff.m._
@import io.suggest.sc.map.ScMapConstants.Mqs._

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@Full.ENVELOPE_TOP_LEFT_LON");
    add("@Full.ENVELOPE_TOP_LEFT_LAT");
    add("@Full.ENVELOPE_BOTTOM_RIGHT_LON");
    add("@Full.ENVELOPE_BOTTOM_RIGHT_LAT");
    add("@ZOOM_FN", true);
  }
}
