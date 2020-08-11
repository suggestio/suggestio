package io.suggest.lk.nodes

import io.suggest.bill.tf.daily.MTfDailyInfo
import io.suggest.n2.node.MNodeType
import io.suggest.primo.id.IId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 14:57
  * Description: Кросс-платформенная модель данных по одному узлу для формы управления узлами (маячками).
  */

object MLknNode {

  @inline implicit def univEq: UnivEq[MLknNode] = UnivEq.derive

  implicit def mLknNodeFormat: OFormat[MLknNode] = (
    (__ \ "i").format[String] and
    (__ \ "n").format[String] and
    (__ \ "t").format[MNodeType] and
    (__ \ "e").format[Boolean] and
    (__ \ "a").formatNullable[Boolean] and
    (__ \ "d").formatNullable[MLknAdv] and
    (__ \ "f").formatNullable[MTfDailyInfo]
  )(apply, unlift(unapply))


  def adv = GenLens[MLknNode](_.adv)

}


/**
  * Класс модели данных по узлу.
  * По идее, это единственая реализация [[MLknNode]].
  */
case class MLknNode(
                     /** Уникальный id узла. */
                     override val id        : String,
                     /** Отображаемое название узла. */
                     name                   : String,
                     /** Тип узла по модели MNodeTypes. */
                     ntype                  : MNodeType,
                     /** Является ли узел активным сейчас? */
                     isEnabled              : Boolean,
                     /** Опциональные данные текущей видимости узла для других пользователей s.io.
                       * None значит, что сервер не определял это значение.
                       */
                     //isPublic             : Boolean,
                     /** Может ли юзер управлять значением флага isEnabled или удалять узел?
                       * None значит, что сервер не интересовался этим вопросом.
                       */
                     canChangeAvailability  : Option[Boolean],

                     /** Имеется ли размещение текущей рекламной карточки на указанном узле? */
                     adv                    : Option[MLknAdv]           = None,
                     /** Данные по тарифу размещения. None значит, что сервер не уточнял этот вопрос. */
                     tf                     : Option[MTfDailyInfo],
                   )
  extends IId[String]
