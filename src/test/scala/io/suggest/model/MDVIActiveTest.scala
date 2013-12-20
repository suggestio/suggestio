package io.suggest.model

import org.scalatest._
import MDVIActive._
import org.joda.time.LocalDate
import io.suggest.util.DateParseUtil.{toDaysCount, dateFromDaysCount}
import MVirtualIndex.{vinFor, esShardNameFor}
import MDVISubshardInfo.getTypename

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.13 19:19
 * Description: Тесты для модели MDVIActive.
 */
class MDVIActiveTest extends FlatSpec with Matchers {

  // Тестируется выборка хранилища в виртуальном индексе
  private val dkey = "suggest.io"

  /** Сериализовать и сразу десериализовать экземплпяр MDVIActive. */
  private def sd(dvi: MDVIActive) = {
    val s = serializeForHBase(dvi)
    val dsResult = deserializeRaw(rowkey=s.rowkey, qualifier=s.qualifier, value=s.value)
    dsResult should equal (dvi)
    deserializeWithDkey(dkey=dvi.getDkey, qualifier=s.qualifier, value=s.value)  should equal (dvi)
    deserializeWithVin(rowkey=s.rowkey, vin=dvi.getVin, value=s.value)           should equal (dvi)
    deserializeWithDkeyVin(dkey=dvi.getDkey, vin=dvi.getVin, value=s.value)      should equal (dvi)
    dsResult
  }


  "MDVIActive" should "have proper results from exported methods" in {
    val vin = "asdasdas1"
    val dvi = new MDVIActive(dkey=dkey, vin=vin, generation=123123L)
    dvi.getShards               should equal (Seq(vin + "_0"))
    dvi.getAllTypes             should equal (Seq(getTypename(dkey, 0)))
    dvi.getVirtualIndex         should equal (MVirtualIndex(vin))
  }


  "serialize/deserialize()" should "handle single-subshard MDVIActive" in {
    val dkey = "aversimage.ru"
    val dvi1 = new MDVIActive(dkey, "asdasdasd1", 0)
    sd(dvi1)      should equal (dvi1)
    sd(sd(dvi1))  should equal (dvi1)
  }

  it should "handle multi-subshard MDVIActive" in {
    val dkey = "suggest.io"
    val shards = List(
      MDVISubshardInfo(123123123, List(1,2)),
      MDVISubshardInfo(0,         List(3,4))
    )
    val dvi1 = new MDVIActive(dkey=dkey, vin=vinFor("asdasd", 1), generation=14, subshardsInfo=shards)
    sd(dvi1)      should equal (dvi1)
    sd(sd(dvi1))  should equal (dvi1)
  }


  "getInxTypeForDate()" should "return correct inx/types on 1-shard MVI 1-subshard MDVIA" in {
    val mdvia = new MDVIActive(vin="adasdasd1", dkey=dkey, generation=123123L)
    val result = mdvia.getShards.head -> getTypename(dkey, 0L)
    import mdvia.getInxTypeForDate
    getInxTypeForDate(LocalDate.now)                should equal  (result)
    getInxTypeForDate(new LocalDate(2004, 3, 2))    should equal  (result)
    getInxTypeForDate(new LocalDate(2044, 10, 14))  should equal  (result)
    getInxTypeForDate(new LocalDate(1944, 10, 11))  should equal  (result)
    getInxTypeForDate(new LocalDate(1644, 10, 11))  should equal  (result)
    getInxTypeForDate(new LocalDate(2555, 12, 12))  should equal  (result)
  }

  it should "return correct inx/types on 1-shard MVI 3-subshard MDVIA" in {
    val days09 = toDaysCount(new LocalDate(2009, 1, 1))
    val days06 = toDaysCount(new LocalDate(2006, 1, 1))
    val days0  = 0
    val vin = vinFor("asdasd", 1)
    val mdvia = new MDVIActive(
      dkey          = dkey,
      vin           = vin,
      generation    = 123456L,
      subshardsInfo = List(days09, days06, days0) map { MDVISubshardInfo(_) }
    )
    val resultInx = mdvia.getVirtualIndex.getShards.head
    def typename(daysCount: Int) = getTypename(dkey, daysCount)

    // Начинаем сами тесты
    import mdvia.getInxTypeForDate

    // На текущих, свежих и будущих датах должна возвращаться последняя шарда
    val resultLast = resultInx -> typename(days09)
    getInxTypeForDate(new LocalDate(2013, 1, 1))    should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2014, 10, 10))  should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2019, 5, 6))    should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2010, 9, 8))    should equal  (resultLast)

    // Между 2006-2009 годами - должна выходит вторая шарда
    val result06 = resultInx -> typename(days06)
    getInxTypeForDate(new LocalDate(2008, 5, 5))    should equal  (result06)
    getInxTypeForDate(new LocalDate(2007, 5, 5))    should equal  (result06)
    getInxTypeForDate(new LocalDate(2006, 1, 2))    should equal  (result06)

    // За 2006 годом должна срабатывать последняя шарда
    val resultOld = resultInx -> typename(days0)
    getInxTypeForDate(new LocalDate(2004, 1, 1))    should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1999, 2, 3))    should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1990, 3, 4))    should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1944, 1, 1))    should equal  (resultOld)
  }

  it should "return correct inx/type on 2-shard MVI 3-subshard MDVIA" in {
    val days09 = toDaysCount(new LocalDate(2009, 1, 1))
    val days06 = toDaysCount(new LocalDate(2006, 1, 1))
    val days0  = 0
    val vin = vinFor("bdsm", 3)
    val mdvia = new MDVIActive(
      dkey = dkey,
      vin = vin,
      generation = 123456L,
      subshardsInfo = {
        List(days09 -> 0, days06 -> 1, days0 -> 1)
          .map { case (daysCount, mviShardId) => MDVISubshardInfo(daysCount, List(mviShardId)) }
      }
    )
    def typename(daysCount: Int) = getTypename(dkey, daysCount)

    // Начинаем сами тесты
    import mdvia.getInxTypeForDate

    // На текущих/свежих/будущих датах должен быть нулевая шарда и head-подшарда
    val resultLast = esShardNameFor(vin=vin, shardN=0)  ->  typename(days09)
    getInxTypeForDate(new LocalDate(2013, 1, 1))    should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2023, 2, 2))    should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2044, 5, 6))    should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2010, 7, 8))    should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2011, 9, 10))   should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2009, 11, 12))  should equal  (resultLast)
    getInxTypeForDate(new LocalDate(2009, 1, 2))    should equal  (resultLast)

    // На постаревших датах из 2009-2006 годов должна быть шарда 1 и вторая подшарда
    val result06 = esShardNameFor(vin=vin, shardN=1)  ->  typename(days06)
    getInxTypeForDate(new LocalDate(2008, 12, 31))  should equal  (result06)
    getInxTypeForDate(new LocalDate(2007, 11, 11))  should equal  (result06)
    getInxTypeForDate(new LocalDate(2006, 1, 2))    should equal  (result06)

    // На старых датах (до 2006 г) должна вылетать шарда 1 и последняя подшарда
    val resultOld = esShardNameFor(vin, 1)  ->  typename(days0)
    getInxTypeForDate(new LocalDate(2005, 1, 1))    should equal  (resultOld)
    getInxTypeForDate(new LocalDate(2003, 10, 10))  should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1999, 11, 11))  should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1990, 12, 12))  should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1944, 11, 11))  should equal  (resultOld)
    getInxTypeForDate(new LocalDate(1755, 12, 12))  should equal  (resultOld)
  }


}
