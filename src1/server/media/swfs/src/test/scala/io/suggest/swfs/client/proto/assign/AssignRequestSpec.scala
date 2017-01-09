package io.suggest.swfs.client.proto.assign

import io.suggest.swfs.client.proto.Replication
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 9:54
 * Description: Тесты для модели [[AssignRequest]].
 */
class AssignRequestSpec extends FlatSpec {

  "Query string serialization" should "support empty model" in {
    AssignRequest().toQs  shouldBe  ""
  }

  it should "support dataCenter param" in {
    val ar = AssignRequest(
      dataCenter = Some("dc1")
    )
    ar.toQs  shouldBe  "?dataCenter=dc1"
  }

  it should "support replication param" in {
    val ar = AssignRequest(
      replication = Some(Replication(0,0,1))
    )
    ar.toQs  shouldBe  "?replication=001"
  }

  it should "support full-filled model" in {
    val ar = AssignRequest(
      dataCenter  = Some("dc1"),
      replication = Some(Replication(1,2,3))
    )
    ar.toQs  shouldBe  "?dataCenter=dc1&replication=123"
  }

}
