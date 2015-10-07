package io.suggest.swfs.client.proto

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 22:48
 * Description: Тесты для модели [[Replication]].
 */
class ReplicationSpec extends FlatSpec {

  /*
   * 000: no replication
   * 001: replicate once on the same rack
   * 010: replicate once on a different rack, but same data center
   * 100: replicate once on a different data center
   * 200: replicate twice on two different data center
   * 110: replicate once on a different rack, and once on a different data center
   */

  "toString" should "000: no-replication" in {
    Replication().toString                shouldBe  "000"
    Replication(0,0,0).toString           shouldBe  "000"
  }

  it should "001: same rack replication" in {
    Replication(sameRack = 1).toString    shouldBe "001"
    Replication(0,0,1).toString           shouldBe "001"
  }

  it should "010: different rack" in {
    Replication(otherRack = 1).toString   shouldBe  "010"
    Replication(0,1,0).toString           shouldBe  "010"
  }

  it should "100: different DC" in {
    Replication(otherDc = 1).toString     shouldBe  "100"
    Replication(1,0,0).toString           shouldBe  "100"
  }

  it should "200: twice on two different DCs" in {
    Replication(otherDc = 2).toString     shouldBe  "200"
    Replication(2,0,0).toString           shouldBe  "200"
  }

  it should "110: once on a different rack, and once on a different data center" in {
    Replication(otherDc = 1, otherRack = 1).toString    shouldBe  "110"
    Replication(1, 1, 0).toString                       shouldBe  "110"
  }

}
