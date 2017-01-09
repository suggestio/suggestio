package util.di

import util.adn.NodesUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 10:42
 * Description: Интерфейс для DI-поля, содержащего инжектируемый экземпляр [[util.adn.NodesUtil]].
 */
trait INodesUtil {

  def nodesUtil: NodesUtil

}
