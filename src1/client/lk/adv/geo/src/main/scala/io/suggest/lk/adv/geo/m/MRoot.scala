package io.suggest.lk.adv.geo.m

import io.suggest.adv.free.MAdv4Free
import io.suggest.adv.geo.MFormS
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.tags.edit.m.MTagsEditState
import io.suggest.maps.m.{MExistGeoS, MMapS, MRad}
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:16
  * Description: Корневая js-only модель diode-формы.
  * В ней хранится вообще всё, но сериализуемые для отправки на сервер данные хранятся в отдельных полях.
  */

object MRoot {

  @inline implicit def univEq: UnivEq[MRoot] = UnivEq.derive

  /** Реализация поддержки FastEq для инстансов [[MRoot]]. */
  implicit lazy val mrootFeq = FastEqUtil[MRoot] { (a, b) =>
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


  implicit final class RootExt( private val mroot: MRoot ) extends AnyVal {

    /**
      * Заворачивание основных данных модели в инстанс MFormS.
      * @return Состояние формы размещения, пригодное для сериализации и отправки на сервер.
      */
    def toFormData: MFormS = {
      MFormS(
        mapProps        = mroot.mmap.toMapProps,
        onMainScreen    = mroot.other.onMainScreen,
        adv4freeChecked = mroot.adv4free.map(_.checked),
        rcvrsMap        = mroot.rcvr.rcvrsMap,
        tagsEdit        = mroot.tags.props,
        datePeriod      = mroot.datePeriod,
        radCircle       = mroot.radEnabled.map(_.circle),
        tzOffsetMinutes = DomQuick.tzOffsetMinutes,
      )
    }

    def radEnabled: Option[MRad] =
      mroot.rad.filter(_.enabled)

  }


  val mmap = GenLens[MRoot]( _.mmap )
  val rcvr = GenLens[MRoot]( _.rcvr )
  val other = GenLens[MRoot]( _.other )
  val tags = GenLens[MRoot]( _.tags )
  val rad = GenLens[MRoot]( _.rad )
  val geoAdv = GenLens[MRoot]( _.geoAdv )
  val datePeriod = GenLens[MRoot]( _.datePeriod )
  val popups = GenLens[MRoot]( _.popups )
  val bill = GenLens[MRoot]( _.bill )
  val adv4free = GenLens[MRoot]( _.adv4free )

}


/** Корневой контейнер состояния lk-adn-map.
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

  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)

}
