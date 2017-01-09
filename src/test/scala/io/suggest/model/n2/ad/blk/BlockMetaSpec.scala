package io.suggest.model.n2.ad.blk

import io.suggest.model.PlayJsonTestUtil
import org.scalatest.FlatSpec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 13:09
 * Description: Тесты для модели [[BlockMeta]].
 */
class BlockMetaSpec extends FlatSpec with PlayJsonTestUtil {

  override type T = BlockMeta

  "play JSON" should "support model" in {
    jsonTest( BlockMeta.DEFAULT )
  }

}
