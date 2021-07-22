package models.adv.build

import io.suggest.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 22:04
  * Description: Модель внешнего (по отн.к билдеру) контекста.
  */

/** Модель общего контекста всей adv-build-операции.
  *
  * @param tagNodesMap Карта узлов-тегов: (tagFace|id) -> MNode.
  *                    Смысл ключа зависит от контекста исполнения
  */
case class MCtxOuter(
  tagNodesMap: Map[String, MNode],
)
