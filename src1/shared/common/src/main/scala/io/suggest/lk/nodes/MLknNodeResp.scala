package io.suggest.lk.nodes

import boopickle.Default._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 13:38
  * Description: Кроссплатформенная модель инфы о дереве узлов.
  * Рекурсия на уровне модели ленивая, т.е. верхний слой узлов может не содержать под-узлов.
  * На раннем этапе: это достаточно для списка маячков.
  * На последующих этапах: подгружать под-узлы с сервера по мере погружения юзера в глубины иерархии.
  */

object MLknNodeResp {

  /** Поддержка сериализации/десериализации. */
  implicit val mLknSubNodesRespPickler: Pickler[MLknNodeResp] = {
    implicit val treeNodeP = MLknNode.lknNodePickler
    generatePickler[MLknNodeResp]
  }

  implicit def univEq: UnivEq[MLknNodeResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  /** Поддержка play-json. */
  implicit def mLknNodeRespFormat: OFormat[MLknNodeResp] = (
    (__ \ "i").format[MLknNode] and
    (__ \ "c").format[Seq[MLknNode]]
  )(apply, unlift(unapply))

}

/**
  * Класс модели ответа на запрос под-списка узлов.
  * @param info Инфа по узлу, если требуется.
  * @param children Дочерние узлы.
  */
case class MLknNodeResp(
                         info       : MLknNode,
                         children   : Seq[MLknNode]
                       )
