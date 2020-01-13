package models.adv.build

import io.suggest.n2.node.MNode

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 22:04
  * Description: Модель внешнего (по отн.к билдеру) контекста.
  */
object MCtxOuter {

  def empty = apply()

  def emptyFut = Future.successful(empty)

}

/** Модель общего контекста всей adv-build-операции.
  *
  * @param tagNodesMap Карта узлов-тегов: (tagFace|id) -> MNode.
  *                    Смысл ключа зависит от контекста исполнения
  */
case class MCtxOuter(
  tagNodesMap: Map[String, MNode] = Map.empty
)
