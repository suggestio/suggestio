package io.suggest.lk.nodes

import boopickle.Default._

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
