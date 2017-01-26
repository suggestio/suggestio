package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.adv.free.MAdv4Free
import io.suggest.adv.geo.MFormS
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.tags.edit.m.MTagsEditState

import scala.scalajs.js.Date

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:16
  * Description: Корневая js-only модель diode-формы.
  * В ней хранится вообще всё, но сериализуемые для отправки на сервер данные хранятся в отдельных полях.
  *
  * @param geoAdv Данные по текущим георазмещениям на карте.
  * @param tags Контейнер данных по тегам.
  */
case class MRoot(
                  mmap          : MMap,
                  other         : MOther,
                  adv4free      : Option[MAdv4Free],
                  tags          : MTagsEditState,
                  rcvr          : MRcvr,
                  rad           : Option[MRad],
                  geoAdv        : MGeoAdvs                = MGeoAdvs(),
                  datePeriod    : MAdvPeriod
                ) {

  def withMapState(ms2: MMap) = copy(mmap = ms2)
  def withOther(other2: MOther) = copy(other = other2)
  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)
  def withTagsEditState(tes: MTagsEditState) = copy(tags = tes)
  def withRad(radOpt: Option[MRad]) = copy(rad = radOpt)
  def withRcvr(rcvr2: MRcvr) = copy(rcvr = rcvr2)
  def withCurrGeoAdvs(cga2: MGeoAdvs) = copy(geoAdv = cga2)
  def withDatePeriod(ivl: MAdvPeriod) = copy(datePeriod = ivl)

  /**
    * Заворачивание основных данных модели в инстанс MFormS.
    * @return Состояние формы размещения, пригодное для сериализации и отправки на сервер.
    */
  def toFormData: MFormS = {
    val tzOffsetMinutes = new Date().getTimezoneOffset()
    MFormS(
      mapProps        = mmap.props,
      onMainScreen    = other.onMainScreen,
      adv4freeChecked = adv4free.map(_.checked),
      rcvrsMap        = rcvr.rcvrsMap,
      tagsEdit        = tags.props,
      datePeriod      = datePeriod,
      radCircle       = rad.map(_.circle),
      tzOffsetMinutes = tzOffsetMinutes
    )
  }

}


object MRoot {

  /** Реализация поддержки FastEq для инстансов [[MRoot]]. */
  implicit object MRootFastEq extends FastEq[MRoot] {
    override def eqv(a: MRoot, b: MRoot): Boolean = {
      (a.mmap eq b.mmap) &&
        (a.other eq b.other) &&
        (a.adv4free eq b.adv4free) &&
        (a.tags eq b.tags) &&
        (a.rcvr eq b.rcvr) &&
        (a.rad eq b.rad) &&
        (a.geoAdv eq b.geoAdv) &&
        (a.datePeriod eq b.datePeriod)
    }
  }

}
