package io.suggest.model

import org.scalatest._
import matchers.ShouldMatchers
import MDVIActive._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.13 19:19
 * Description:
 */
class MDVIActiveTest extends FlatSpec with ShouldMatchers {
  val msg_sd = "be serializable & deserializable"

  def sd(dvi: MDVIActive) = deserializeFromTupleDkey(dvi.dkey, serializeToDkeylessTuple(dvi))

  "simple/default subj" should msg_sd in {
    val dkey = "aversimage.ru"
    val dvi1 = MDVIActive(dkey, "asdasdasd1", 0)
    sd(dvi1) should equal (dvi1)
  }

  "more complex subj" should msg_sd in {
    val dkey = "suggest.io"
    val shards = List(
      MDVISubshardInfo(123123123, List(1,2)),
      MDVISubshardInfo(0,         List(3,4))
    )
    val dvi1 = MDVIActive(dkey, "asdasd", 14, shards)
    sd(dvi1) should equal (dvi1)
  }

}
