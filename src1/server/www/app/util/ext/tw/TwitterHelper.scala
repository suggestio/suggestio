package util.ext.tw

import javax.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.ext.svc.MExtServices
import io.suggest.img.MImgFormats
import io.suggest.n2.node.MNode
import io.suggest.util.logs.MacroLogsImpl
import models.adv.ext.Mad2ImgUrlCalcOuter
import models.mctx.{Context, ContextUtil}
import models.mext.tw.card.{PhotoCardArgs, TwImgSizes}
import models.mproj.{ICommonDi, IRenderable}
import play.api.libs.ws.WSClient
import play.twirl.api.Html
import util.adv.AdvUtil
import util.ext.{IExtServiceHelper, OAuth1Support}
import util.n2u.N2NodesUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 11:05
  * Description: Утиль для взаимодействия с твиттером.
  */
@Singleton
class TwitterHelper @Inject() (
                                override val ctxUtil      : ContextUtil,
                                override val n2NodesUtil  : N2NodesUtil,
                                override val wsClient     : WSClient,
                                override val advUtil      : AdvUtil,
                                override val mCommonDi    : ICommonDi
                              )
  extends IExtServiceHelper
  with OAuth1Support
  with TwMpUpload
  with MacroLogsImpl
  with Mad2ImgUrlCalcOuter
{
  that =>

  override def mExtService = MExtServices.Twitter

  // TODO Нужно нормальную длину узнать. Там какой-то гемор с ссылками, что даже 100 символов - многовато.
  override def LEAD_TEXT_LEN      = 90
  override def WITH_URL           = true
  override def MK_MESSAGE_URL     = "https://api.twitter.com/1.1/statuses/update.json"
  override def AC_TOK_VERIFY_URL  = "https://api.twitter.com/1.1/account/verify_credentials.json?include_entities=false&skip_status=true"

  override protected[this] def ssConfPrefix = "securesocial.twitter"

  override def REQUEST_TOKEN_URL_DFLT = "https://api.twitter.com/oauth/request_token"
  override def ACCESS_TOKEN_URL_DFLT  = "https://api.twitter.com/oauth/access_token"
  override def AUTHORIZATION_URL      = "https://api.twitter.com/oauth/authorize"


  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?twitter\\.com".r.pattern.matcher(host).matches()
  }

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  override def postImgSzWithName(n: String) = TwImgSizes.withValueOpt(n)

  override def advPostMaxSz(tgUrl: String) = TwImgSizes.Photo


  /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
  override def oauth1Support = Some(this)

  /**
    * Генератор HTML-мета-тегов для описания рекламной карточки.
    *
    * @param mad1 Экземпляр рекламной карточки.
    * @return экземпляры моделй, готовых для запуска рендера.
    */
  override def adMetaTagsRender(mad1: MNode): Future[List[IRenderable]] = {
    val acc0Fut = super.adMetaTagsRender(mad1)
    // Собираем через враппер, т.к. для генерации метаданных нужен доступ к Context.
    val ir = new IRenderable {
      override def render()(implicit ctx: Context): Html = {
        // Калькулятор ссылки
        val calc = new Mad2ImgUrlCalc {
          override def mad            = mad1
          override def tgUrl          = mExtService.mainPageUrl
          override def adRenderMaxSz  = TwImgSizes.Photo
          override def serviceHelper  = that
          override val madRenderInfo  = super.madRenderInfo
        }
        // Аргументы для рендера карточки и рендер.
        val pca = PhotoCardArgs(
          imgUrl = controllers.sc.routes.ScOnlyOneAd.onlyOneAdAsImage( calc.adRenderArgs ).url,
          url    = Some( ctx.request.uri ),
          title  = mad1.guessDisplayName,
          imgWh  = Some( calc.madRenderInfo.wh )
        )
        pca.render()
      }
    }
    for (acc0 <- acc0Fut) yield {
      ir :: acc0
    }
  }

  /** Поддержка аплоада в твиттер. */
  override def maybeMpUpload = Some(this)

  /** Дефолтовый формат изображения, если не переопределен в конфиге. */
  override def imgFmtDflt = MImgFormats.PNG

  /** В твиттер надо всегда постить горизонтальные карточки. */
  override def isAdvExtWide(mad: MNode) = true

}
