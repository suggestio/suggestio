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
 * @param nameOpt messages-код отображаемого слоя. Если None, то не будет ничего отображено.
 */
case class GeoNodesLayer(
  nodes: Seq[MAdnNode],
  nameOpt: Option[String] = None
)
