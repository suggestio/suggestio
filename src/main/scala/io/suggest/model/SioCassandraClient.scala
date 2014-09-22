package io.suggest.model

import com.datastax.driver.core.Cluster
import io.suggest.util.MyConfig.CONFIG
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.14 17:21
 * Description: Клиент для cassandra-кластера.
 */
object SioCassandraClient {

  def CONTRACT_NODES: Seq[String] = CONFIG.getStringList("cassandra.cluster.nodes.connect.on.start").map(_.toSeq) getOrElse Seq("localhost")

  val cluster = Cluster.builder()
    .addContactPoints(CONTRACT_NODES: _*)
    .build()


  implicit val session = cluster.newSession()

}
