package io.suggest.xadv.ext.js.vk.m

import minitest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 16:20
 * Description: Тесты для модели [[VkTargetInfo]].
 */
object VkTargetInfoSpec extends SimpleTestSuite {

  test("Must serialize and deserialize VkTargetInfo") {
    val e = VkTargetInfo(
      id = 5663634,
      tgType = VkTargetTypes.User,
      name = Some("VKontakte")
    )
    assertEquals(VkTargetInfo.fromJson(e.toJson), e)
  }

}
