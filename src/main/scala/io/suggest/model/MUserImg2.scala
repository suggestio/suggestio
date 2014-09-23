package io.suggest.model

import java.nio.ByteBuffer
import java.util.UUID
import io.suggest.util.UuidUtil
import org.joda.time.DateTime
import com.websudos.phantom.Implicits._
import SioCassandraClient.session

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.14 15:24
 * Description: Cassandra-модель для хранения картинок (в виде блобов).
 */
case class MUserImg2(
  img       : ByteBuffer,
  timestamp : DateTime = DateTime.now,
  id        : UUID = UUID.randomUUID()
) extends IdStr {

}





trait IdStr {
  def id: UUID
  def idStr = UuidUtil.uuidToBase64(id)
}
