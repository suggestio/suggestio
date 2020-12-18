package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.adn.mapf.{MLamConf, MLamForm}
import io.suggest.adv.free.MAdv4Free
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.adv.m.MPriceS
import io.suggest.maps.m.MAdvGeoS
import io.suggest.sjs.dom2.DomQuick
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:32
  * Description: Корневая модель состояния компонента fsm-mvm.
  */

object MRoot {

  /** Поддержка FastEq. */
  implicit object MRootFastEq extends FastEq[MRoot] {
    override def eqv(a: MRoot, b: MRoot): Boolean = {
      (a.geo eq b.geo) &&
        (a.conf eq b.conf) &&
        (a.rcvrs eq b.rcvrs) &&
        (a.adv4free eq b.adv4free) &&
        (a.price eq b.price) &&
        (a.datePeriod eq b.datePeriod)
    }
  }

  val geo = GenLens[MRoot]( _.geo )
  val adv4free = GenLens[MRoot]( _.adv4free )
  val price = GenLens[MRoot]( _.price )
  val datePeriod = GenLens[MRoot]( _.datePeriod )
  val rcvrs = GenLens[MRoot]( _.rcvrs )


  implicit final class MRootExt( private val mroot: MRoot) extends AnyVal {

    /** Создать снимок основных данных, пригодный для отправки на сервер. */
    def toForm: MLamForm = {
      MLamForm(
        mapProps          = mroot.geo.mmap.toMapProps,
        // TODO .get - тут нужно опционализировать сие, вместе с внедрением тегов и галочек.
        mapCursor         = mroot.geo.rad.get.circle,
        datePeriod        = mroot.datePeriod,
        adv4freeChecked   = mroot.adv4free.map(_.checked),
        tzOffsetMinutes   = DomQuick.tzOffsetMinutes,
      )
    }

  }

}


/** Класс корневой модели состояния формы.
  */
case class MRoot(
                  geo           : MAdvGeoS,
                  conf          : MLamConf,
                  rcvrs         : MLamRcvrs         = MLamRcvrs(),
                  adv4free      : Option[MAdv4Free],
                  price         : MPriceS,
                  datePeriod    : MAdvPeriod
                )
