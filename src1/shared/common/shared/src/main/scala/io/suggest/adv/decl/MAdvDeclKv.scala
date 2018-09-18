package io.suggest.adv.decl

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.18 16:47
  * Description: Модель из двух полей, совместимая с картой.
  */
object MAdvDeclKv {

  @inline implicit def univEq: UnivEq[MAdvDeclKv] = UnivEq.derive

  implicit def mAdvDeclKvFormat: OFormat[MAdvDeclKv] = (
    (__ \ "k").format[MAdvDeclKey] and
    (__ \ "v").format[MAdvDeclSpec]
  )(apply, unlift(unapply))

  /** Доп утиль для коллекций с абстрактными спеками размещений. */
  implicit class AdvDeclKvsExtOps(val kvs: TraversableOnce[MAdvDeclKv]) extends AnyVal {
    def declsToMap: AdvDeclsMap_t = {
      kvs
        .toIterator
        .map( _.tuple )
        .toMap
    }
  }

}


/** Контейнер данных
  *
  * @param key Ключ размещения (цель для размещения).
  * @param spec Описание размещения в данной цели.
  */
case class MAdvDeclKv(
                       key    : MAdvDeclKey,
                       spec   : MAdvDeclSpec
                     ) {

  def tuple = (key, spec)

  /** Какой ключ используется для группировки unbilled-эджей?
    * На момент написания, тут группировались только AdvDirect / Receiver.Self эджи.
    */
  def unbilledEdgesGroupingKey = {
    (key.itype, spec.isShowOpened)
  }

}
