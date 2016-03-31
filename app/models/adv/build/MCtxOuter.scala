package models.adv.build

import models.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 22:04
  * Description: Модель внешнего (по отн.к билдеру) контекста.
  */
trait ICtxOuter {

  /** Карта узловв-тегов: tagFace -> MNode. */
  def tagFacesNodesMap: Map[String, MNode]

}

object MCtxOuter {

  def empty = apply()

}

/** Дефолтовая реализация модели [[ICtxOuter]]. */
case class MCtxOuter(
  override val tagFacesNodesMap: Map[String, MNode] = Map.empty
)
  extends ICtxOuter