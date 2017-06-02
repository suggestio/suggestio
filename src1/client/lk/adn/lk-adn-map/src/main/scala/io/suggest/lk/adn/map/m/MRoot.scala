package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.adn.mapf.MLamForm
import io.suggest.adv.free.MAdv4Free
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.adv.m.MPriceS
import io.suggest.maps.m.{MExistGeoS, MMapS}
import io.suggest.sjs.common.controller.DomQuick

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
        IRadOpts.IRadOptsFastEq.eqv(a, b) &&
        (a.current eq b.current) &&
        (a.adv4free eq b.adv4free) &&
        (a.price eq b.price) &&
        (a.datePeriod eq b.datePeriod)
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
                  adv4free      : Option[MAdv4Free],
                  price         : MPriceS,
                  datePeriod    : MAdvPeriod
                )
  extends IRadOpts[MRoot]
{

  def withMap(map2: MMapS) = copy(mmap = map2)
  override def withRad(rad2: MLamRad) = copy(rad = rad2)
  def withCurrent(current2: MExistGeoS) = copy(current = current2)
  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)
  def withPrice(price2: MPriceS) = copy(price = price2)
  def withDatePeriod(dp2: MAdvPeriod) = copy(datePeriod = dp2)

  /** Создать снимок основных данных, пригодный для отправки на сервер. */
  def toForm: MLamForm = {
    MLamForm(
      mapProps          = mmap.props,
      mapCursor         = rad.circle,
      datePeriod        = datePeriod,
      adv4freeChecked   = adv4free.map(_.checked),
      tzOffsetMinutes   = DomQuick.tzOffsetMinutes
    )
  }

}
