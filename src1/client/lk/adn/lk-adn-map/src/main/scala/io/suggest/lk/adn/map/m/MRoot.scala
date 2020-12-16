package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.adn.mapf.{MLamConf, MLamForm}
import io.suggest.adv.free.MAdv4Free
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.adv.m.MPriceS
import io.suggest.maps.m.{MExistGeoS, MMapS}
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
      (a.mmap eq b.mmap) &&
        (a.conf eq b.conf) &&
        (a eq b) &&
        (a.current eq b.current) &&
        (a.rcvrs eq b.rcvrs) &&
        (a.adv4free eq b.adv4free) &&
        (a.price eq b.price) &&
        (a.datePeriod eq b.datePeriod)
    }
  }

  val mmap = GenLens[MRoot]( _.mmap )
  val adv4free = GenLens[MRoot]( _.adv4free )
  val rad = GenLens[MRoot]( _.rad )
  val current = GenLens[MRoot]( _.current )
  val price = GenLens[MRoot]( _.price )
  val datePeriod = GenLens[MRoot]( _.datePeriod )
  val rcvrs = GenLens[MRoot]( _.rcvrs )


  implicit final class MRootExt( private val mroot: MRoot) extends AnyVal {

    /** Создать снимок основных данных, пригодный для отправки на сервер. */
    def toForm: MLamForm = {
      MLamForm(
        mapProps          = mroot.mmap.toMapProps,
        mapCursor         = mroot.rad.circle,
        datePeriod        = mroot.datePeriod,
        adv4freeChecked   = mroot.adv4free.map(_.checked),
        tzOffsetMinutes   = DomQuick.tzOffsetMinutes,
      )
    }

  }

}


/** Класс корневой модели состояния формы.
  *
  * @param mmap Состояние географической карты.
  * @param rad Состояние rad-компонента на карте.
  *            Не-опционально, т.к. используется для всех возможных режимов сразу.
  */
case class MRoot(
                  mmap          : MMapS,
                  conf          : MLamConf,
                  rad           : MLamRad,
                  current       : MExistGeoS        = MExistGeoS(),
                  rcvrs         : MLamRcvrs         = MLamRcvrs(),
                  adv4free      : Option[MAdv4Free],
                  price         : MPriceS,
                  datePeriod    : MAdvPeriod
                )
