@(delim: String)

@* Код js unbind для модели m.sc.FocusedAdsSearchArgs для js-роутера. *@

@import stuff.m._
@import io.suggest.sc.map.ScMapConstants.Mqs.Full._

@jsUnbindBase() {
  @_objQsbTpl(delim) {
    add("@AREA_ENVELOPE_TOP_LEFT_LON");
    add("@AREA_ENVELOPE_TOP_LEFT_LAT");
    add("@AREA_ENVELOPE_BOTTOM_RIGHT_LON");
    add("@AREA_ENVELOPE_BOTTOM_RIGHT_LAT");
    add("@AREA_ZOOM", true);
  }
}
