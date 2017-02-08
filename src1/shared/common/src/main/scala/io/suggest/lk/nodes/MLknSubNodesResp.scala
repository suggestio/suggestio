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

object MLknSubNodesResp {

  /** Поддержка сериализации/десериализации. */
  implicit val mLknSubNodesRespPickler: Pickler[MLknSubNodesResp] = {
    implicit val nodeP = ILknTreeNode.lknTreeNodePickler
    generatePickler[MLknSubNodesResp]
  }

}

/**
  * Класс модели ответа на запрос под-списка узлов.
  * @param nodes Узлы.
  */
case class MLknSubNodesResp(
                             nodes: Seq[ILknTreeNode]
                           )
