package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.dt.MAdvPeriod
import io.suggest.geo.MGeoCircle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:53
  * Description: Модели состояния формы георазмещения, расшаренные между клиентом и сервером.
  *
  * Для передачи модели между клиентом и сервером используется бинарный boopickle + BASE64 (RFC4648_URLSAFE желательно).
  * Бинарщина быстрая и обфусцированная даже на предмет типов и имён полей.
  *
  * 2016.12.28: Было решено, что эта модель должна быть более эфемерна, чем было изначально.
  * Т.е. должна быть js-only модель MRoot, с полями, которые соответствуют компонентам,
  * MRoot при необходимости может экспортировать одноразовые инстансы со своими кусками в виде вот этой
  * клиент-серверной модели. Это поможет выкинуть поле adId, сгуппировать разные разбросанные модели по полям.
  */

object MFormS {

  // TODO Opt Можно проверсти тюнинг boopickle на предмет скорости.
  // https://github.com/ochrons/boopickle#optimizations-strategies

  implicit val pickler: Pickler[MFormS] = {
    implicit val mmapsP = MMapProps.mmapsPickler
    implicit val datePeriodP = MAdvPeriod.mAdvPeriodPickler
    implicit val circleP = MGeoCircle.mGeoCirlePickler
    generatePickler[MFormS]
  }

  def TZ_OFFSET_IGNORE = Int.MinValue

}


/** Корневая модель состояния формы георазмещения на базе diode circuit.
  * Этот класс пошарен между сервером и клиентом, поэтому
  *
  * param mapState id георазмещаемой рекламной карточки.
  * param existCircles Текущие кружки, если есть.
  *
  * @param tzOffsetMinutes С сервера на клиент -- вообще любое число (игнорится клиентом).
  *                        На клиенте: вычисляется на лету и отсылается на сервер.
  *                        Т.е. оно типа обязательное с клиента на сервер, но ненужное с сервера на клиент.
  */
case class MFormS(
                   mapProps         : MMapProps,
                   onMainScreen     : Boolean,
                   adv4freeChecked  : Option[Boolean],
                   rcvrsMap         : RcvrsMap_t,
                   tagsEdit         : MTagsEditProps,
                   datePeriod       : MAdvPeriod,
                   radCircle        : Option[MGeoCircle],
                   tzOffsetMinutes  : Int
                 ) {

  def withRcvrsMap(rcvrsMap2: RcvrsMap_t) = copy(rcvrsMap = rcvrsMap2)

}
