package models.msc

import io.suggest.geo.MNodeGeoLevel
import io.suggest.model.n2.node.MNode


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.14 16:58
 * Description: Файл с моделями, которые относятся к выдаче маркета.
 */

/** Результат работы детектора текущего узла. */
case class GeoDetectResult(
                            ngl   : MNodeGeoLevel,
                            node  : MNode
)


/**
 * Найденные узлы с одного геоуровня. Используеся в ctl.Sc для списка узлов.
 * @param nodes Список узлов в порядке отображения.
 * @param nameOpt messages-код отображаемого слоя. Если None, то не будет ничего отображено.
 * @param expanded Отображать уже развёрнутов? false по умолчанию.
 */
case class GeoNodesLayer(
                          nodes     : Seq[MNode],
                          ngl       : MNodeGeoLevel,
                          nameOpt   : Option[String] = None,
                          expanded  : Boolean = false
)
