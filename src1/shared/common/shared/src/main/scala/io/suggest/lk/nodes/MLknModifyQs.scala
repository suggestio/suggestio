package io.suggest.lk.nodes

import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.2020 22:00
  * Description: Модель-контейнер qs-аргументов для вызова LkNodes.modifyNode().
  */
object MLknModifyQs {

  object Fields {
    def ON_NODE_RK = "n"
    def AD_ID = "a"
    def OP_KEY = "k"
    def OP_VALUE = "v"
  }

  implicit def lknModifyQsJson: OFormat[MLknModifyQs] = {
    val F = Fields
    (
      (__ \ F.ON_NODE_RK).format[RcvrKey] and
      (__ \ F.AD_ID).formatNullable[String] and
      (__ \ F.OP_KEY).format[MLknOpKey] and
      (__ \ F.OP_VALUE).format[MLknOpValue]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MLknModifyQs] = UnivEq.derive

}


/** QS-контейнер данных для LkNodes.modifyNode().
  *
  * @param onNodeRk Узел.
  * @param adIdOpt Рекламная карточка, если есть.
  * @param opKey Действие.
  * @param opValue Значение.
  */
final case class MLknModifyQs(
                               onNodeRk    : RcvrKey,
                               adIdOpt     : Option[String],
                               opKey       : MLknOpKey,
                               opValue     : MLknOpValue,
                             )
