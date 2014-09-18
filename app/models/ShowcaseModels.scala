package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.14 16:58
 * Description: Файл с моделями, которые относятся к выдаче маркета.
 */

/** Результат работы детектора текущего узла. */
case class GeoDetectResult(ngl: NodeGeoLevel, node: MAdnNode)


/**
 * Найденные узлы с одного геоуровня. Используеся в ctl.MarketShowcase для списка узлов.
 * @param nodes Список узлов в порядке отображения.
 * @param glevel Уровень.
 * @param withLayerSelector Рендерить вне селектора.
 * @param renderAstGrouped Надо бы сгруппировать узлы по отображаемому типу.
 */
case class GeoNodesLayer(
  nodes: Seq[MAdnNode],
  glevel: NodeGeoLevel,
  withLayerSelector: Boolean = true,
  renderAstGrouped: Boolean = false
)

