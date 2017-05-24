package models.adv.build

import models.MNode

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 22:04
  * Description: Модель внешнего (по отн.к билдеру) контекста.
  */
trait ICtxOuter {

  /** Карта узлов-тегов: (tagFace|id) -> MNode.
    * Смысл ключа зависит от контекста исполнения
    */
  def tagNodesMap: Map[String, MNode]

}

object MCtxOuter {

  def empty = apply()

  def emptyFut = Future.successful(empty)

}

/** Дефолтовая реализация модели [[ICtxOuter]]. */
case class MCtxOuter(
  override val tagNodesMap: Map[String, MNode] = Map.empty
)
  extends ICtxOuter