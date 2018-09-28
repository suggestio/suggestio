package io.suggest.bill.cart

import io.suggest.mbill2.m.gid.Gid_t
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 10:59
  * Description: Конфиг компонента корзины. Может задаваться сервером.
  */
object MCartConf {

  /** Поддержка play-json. */
  implicit def mCartConfFormat: OFormat[MCartConf] = (
    (__ \ "o").formatNullable[Gid_t] and
    (__ \ "n").formatNullable[String]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MCartConf] = UnivEq.derive

}


/** Контейнер констант корзины.
  *
  * @param orderId id заказа.
  *                Может быть None, если заказ-корзина на сервере не инициализирована.
  *                Тогда по идее и сделать с корзиной ничего нельзя, т.к. нет item'ов тоже.
  * @param onNodeId На каком узле открыта корзина.
  *                 Исторически, сервер рендерит с точки зрения узла, хотя корзина к узлу не привязана.
  */
case class MCartConf(
                      orderId         : Option[Gid_t],
                      onNodeId        : Option[String],
                    )
