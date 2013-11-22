package io.suggest.model

import org.scalatest._
import matchers.ShouldMatchers
import MVirtualIndex._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.11.13 17:19
 * Description: Тесты для функционала модели MVirtualIndex. Модель целиком существует только в памяти, поэтому всё можно
 * протестировать без сайд-эффектов.
 */
class MVirtualIndexTest extends FlatSpec with ShouldMatchers {

  val vinPrefix = "abcdefg"
  
  "esShardNameFor()" should "generate proper ES shard names" in {
    esShardNameFor(vinPrefix="abcdefg", shardCount=1, shardN=0)  should equal ("abcdefg1_0")
    esShardNameFor(vinPrefix="abcdefg", shardCount=2, shardN=1)  should equal ("abcdefg2_1")
    esShardNameFor(vin="abcdefg1", shardN=0)                     should equal ("abcdefg1_0")
  }


  "apply(vin)" should "properly parse 1-shard vins" in {
    val vin = vinPrefix + "1"
    val mvi = MVirtualIndex(vin)
    mvi.vin         should equal (vin)
    mvi.vinPrefix   should equal (vinPrefix)
    mvi.getShardIds should equal (Seq(0))
    mvi.getShards   should equal (Seq(vin + "_0"))
    mvi.head        should equal ("abcdefg1_0")
    mvi.shardCount  should equal (1)
  }

  it should "properly understand multi-shard vins" in {
    val vin = vinPrefix + "3"
    val mvi = MVirtualIndex(vin)
    mvi.vin         should equal (vin)
    mvi.vinPrefix   should equal (vinPrefix)
    mvi.getShards   should equal (Seq(vin+"_0", vin+"_1", vin+"_2"))
    mvi.getShardIds should equal (Seq(0, 1, 2))
    mvi.head        should equal (vin + "_0")
    mvi.shardCount  should equal (3)
  }


  "apply(vinPrefix, shardCount)" should "properly generate MVI" in {
    val shardCount = 1
    val vin = vinPrefix + shardCount
    val mvi = MVirtualIndex(vinPrefix, shardCount)
    mvi.vin         should equal (vin)
    mvi.vinPrefix   should equal (vinPrefix)
    mvi.getShardIds should equal (Seq(0))
    mvi.getShards   should equal (Seq(vin + "_0"))
    mvi.head        should equal (vin + "_0")
    mvi.shardCount  should equal (shardCount)
  }

}
