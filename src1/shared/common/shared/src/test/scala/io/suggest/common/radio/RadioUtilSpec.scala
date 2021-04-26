package io.suggest.common.radio

import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.11.16 17:55
  * Description: Тесты для [[RadioUtil]].
  */
object RadioUtilSpec extends SimpleTestSuite {

  private def ~~(x: Option[Double] ): Int = {
    x.fold[Int](throw new IllegalArgumentException("no distance calculated")) { distM =>
      distM.toInt
    }
  }

  test("Calculate acceptable distances (accuracies) for ibeacon at 1 meter") {
    assertEquals( ~~(RadioUtil.calculateAccuracy(1, -61, Some(-61))), 1 )
  }

  test("ibeacon at 2 meters") {
    assertEquals( ~~(RadioUtil.calculateAccuracy(1, -61, Some(-70))), 2 )
  }

  test("EddyStone at 1 meter") {
    assertEquals( ~~(RadioUtil.calculateAccuracy(0, -24, Some(-66))), 1 )
  }

  test("Buggy experimental MS EddyStone at 1 meter (2016.nov.25)") {
    assertEquals( ~~(RadioUtil.calculateAccuracy(0, -102, Some(-66))), 1 )
  }

}
