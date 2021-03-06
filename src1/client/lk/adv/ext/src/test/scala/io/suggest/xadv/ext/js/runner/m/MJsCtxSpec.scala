package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MAskActions
import io.suggest.ext.svc.{MExtServiceInfo, MExtServices}
import minitest._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 10:10
 * Description: minitest-спека для тестирования модели MJsCtx.
 */
object MJsCtxSpec extends SimpleTestSuite {

  test("serialize and deserialize initial ctx") {
    val mad = MAdCtx(
      madId = "asdasdasdas123",
      content = MAdContentCtx(
        fields = Seq(
          MAdContentField("asdasdasd asd erg erg 34tsergerg serg"),
          MAdContentField("Asd asdf aw34 a23fa23fawef awef awef awefawef")
        ),
        title = Some("AASDF asdf asdfaw f aw3f a3a3fw"),
        descr = Some("as dfaSDFASDF awefawef w3f aw3 wf awf asfawef awefa wef awef awef awefawefawef234")
      ),
      picture = Some(MAdPictureCtx(
        sizeId = Some("someSizeId"),
        upload = Some(MPicS2sUploadCtx(url = "http://pu.vk.com/asdasdasd", partName = "photo")),
        url = Some("http://www.suggest.io/pic4a/123123"),
        saved = Some("{asdasd: 4535}")
      )),
      scUrl = Some("http://www.suggest.io/pic/12312312.jpg")
    )

    val ctx0 = MJsCtx(
      action = MAskActions.EnsureReady,
      mads = Seq(mad),
      service = Some( MExtServiceInfo(MExtServices.VKontakte, appId = Some("12312332")) ),
      domains = Seq("vk.com"),
      target = Some(MExtTarget(
        id          = "someid",
        tgUrl       = "http://vk.com/soddom123",
        onClickUrl  = "http://suggest.io/m/asd/asd/12",
        name        = Some("some name"),
        customRaw   = None
      )),
      custom = None
    )
    assert(MJsCtx.fromJson(ctx0.toJson) == ctx0)
  }

}
