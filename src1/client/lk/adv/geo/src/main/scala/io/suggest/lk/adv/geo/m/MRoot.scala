package io.suggest.lk.adv.geo.m

import io.suggest.adv.geo.MFormS
import io.suggest.maps.m.MAdvGeoS
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
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
    (a.geo ===* b.geo) &&
      (a.other ===* b.other) &&
      (a.adv ===* b.adv) &&
      (a.popups ===* b.popups)
  }


  implicit final class RootExt( private val mroot: MRoot ) extends AnyVal {

    /**
      * Заворачивание основных данных модели в инстанс MFormS.
      * @return Состояние формы размещения, пригодное для сериализации и отправки на сервер.
      */
    def toFormData: MFormS = {
      MFormS(
        mapProps        = mroot.geo.mmap.toMapProps,
        onMainScreen    = mroot.other.onMainScreen,
        adv4freeChecked = mroot.adv.free.map(_.checked),
        rcvrsMap        = mroot.adv.rcvr.rcvrsMap,
        tagsEdit        = mroot.adv.tags.props,
        datePeriod      = mroot.adv.datePeriod,
        radCircle       = mroot.geo.radEnabled.map(_.circle),
        tzOffsetMinutes = DomQuick.tzOffsetMinutes,
      )
    }

  }


  val geo = GenLens[MRoot]( _.geo )
  val other = GenLens[MRoot]( _.other )
  val adv = GenLens[MRoot]( _.adv )
  val popups = GenLens[MRoot]( _.popups )

}


/** Корневой контейнер состояния lk-adn-map.
  */
final case class MRoot(
                        geo           : MAdvGeoS,
                        other         : MOther,
                        adv           : MAdvS,
                        popups        : MPopupsS                = MPopupsS(),
                      )
