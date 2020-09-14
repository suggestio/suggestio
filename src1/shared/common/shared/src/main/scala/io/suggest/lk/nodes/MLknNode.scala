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
    (__ \ "t").formatNullable[MNodeType] and
    (__ \ "n").formatNullable[String] and
    (__ \ "e").formatNullable[Boolean] and
    (__ \ "a").formatNullable[Boolean] and
    (__ \ "d").formatNullable[MLknAdv] and
    (__ \ "f").formatNullable[MTfDailyInfo] and
    (__ \ "l").formatNullable[Boolean] and
    (__ \ "p").formatNullable[String]
  )(apply, unlift(unapply))


  def adv = GenLens[MLknNode](_.adv)

}


/** Контейнер модели данных по узлу для формы-дерева LkNodes.
  *
  * @param id Уникальный id узла.
  * @param ntype Тип узла по модели MNodeTypes.
  *              None - сервер не возвращает, т.к. не имеет смысла для запроса или данному клиенту не положено знать.
  * @param name Отображаемое название узла.
  *             None - означает, что или оно отсутствует.
  *             None - или (скорее всего) для данного юзера/запроса оно не возвращается.
  * @param isEnabled Является ли узел активным сейчас?
  *                  None - сервер не проверял и не возвращает это значение.
  * @param isAdmin Может ли текущий юзер управлять значением флага isEnabled или удалять узел?
  *                admin-привелегии в lk-nodes-форме зависят также от типа узла.
  *                None значит, что сервер не интересовался этим вопросом.
  * @param adv Имеется ли размещение текущей рекламной карточки на указанном узле?
  * @param tf Данные по тарифу размещения. None значит, что сервер не уточнял этот вопрос.
  * @param isDetailed Это детализованный ответ сервера или краткий?
  *                   Чтобы различать отстуствие тарифа/размещения и т.д. и просто невозврат этих полей сервером.
  * @param parentName Опциональное описание.
  */
final case class MLknNode(
                           override val id        : String,
                           ntype                  : Option[MNodeType],
                           name                   : Option[String]            = None,
                           isEnabled              : Option[Boolean]           = None,
                           isAdmin                : Option[Boolean]           = None,
                           adv                    : Option[MLknAdv]           = None,
                           tf                     : Option[MTfDailyInfo]      = None,
                           isDetailed             : Option[Boolean]           = None,
                           parentName             : Option[String]            = None,
                         )
  extends IId[String]
{

  def nameOrEmpty = name getOrElse ""

}
