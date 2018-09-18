package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.adv.free.MAdv4Free
import io.suggest.adv.geo.MFormS
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.tags.edit.m.MTagsEditState
import io.suggest.maps.m.{MExistGeoS, MMapS, MRad}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

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
                  mmap          : MMapS,
                  other         : MOther,
                  adv4free      : Option[MAdv4Free],
                  tags          : MTagsEditState,
                  rcvr          : MRcvr,
                  rad           : Option[MRad],
                  geoAdv        : MExistGeoS              = MExistGeoS(),
                  datePeriod    : MAdvPeriod,
                  popups        : MPopupsS                = MPopupsS(),
                  bill          : MBillS
                ) {

  def withMapState(ms2: MMapS) = copy(mmap = ms2)
  def withOther(other2: MOther) = copy(other = other2)
  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)
  def withTagsEditState(tes: MTagsEditState) = copy(tags = tes)
  def withRad(radOpt: Option[MRad]) = copy(rad = radOpt)
  def withRcvr(rcvr2: MRcvr) = copy(rcvr = rcvr2)
  def withCurrGeoAdvs(cga2: MExistGeoS) = copy(geoAdv = cga2)
  def withDatePeriod(ivl: MAdvPeriod) = copy(datePeriod = ivl)
  def withPopups(popups2: MPopupsS) = copy(popups = popups2)
  def withBill(bill2: MBillS) = copy(bill = bill2)

  def radEnabled = rad.filter(_.enabled)

  /**
    * Заворачивание основных данных модели в инстанс MFormS.
    * @return Состояние формы размещения, пригодное для сериализации и отправки на сервер.
    */
  def toFormData: MFormS = {
    MFormS(
      mapProps        = mmap.toMapProps,
      onMainScreen    = other.onMainScreen,
      adv4freeChecked = adv4free.map(_.checked),
      rcvrsMap        = rcvr.rcvrsMap,
      tagsEdit        = tags.props,
      datePeriod      = datePeriod,
      radCircle       = radEnabled.map(_.circle),
      tzOffsetMinutes = DomQuick.tzOffsetMinutes
    )
  }

}


object MRoot {

  @inline implicit def univEq: UnivEq[MRoot] = UnivEq.derive

  /** Реализация поддержки FastEq для инстансов [[MRoot]]. */
  implicit object MRootFastEq extends FastEq[MRoot] {
    override def eqv(a: MRoot, b: MRoot): Boolean = {
      (a.mmap ===* b.mmap) &&
        (a.other ===* b.other) &&
        (a.adv4free ===* b.adv4free) &&
        (a.tags ===* b.tags) &&
        (a.rcvr ===* b.rcvr) &&
        (a.rad ===* b.rad) &&
        (a.geoAdv ===* b.geoAdv) &&
        (a.datePeriod ===* b.datePeriod) &&
        (a.popups ===* b.popups) &&
        (a.bill ===* b.bill)
    }
  }

}
