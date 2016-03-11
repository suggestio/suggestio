package models.mext.tw

import controllers.routes
import models.adv.ext.Mad2ImgUrlCalc
import models.im.OutImgFmts
import models.mctx.Context
import models.mext.tw.card.{PhotoCardArgs, TwImgSizes}
import models.mext.{IExtService, MExtServices}
import models.{IRenderable, MNode}
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.adv.OAuth1ServiceActorFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:14
 * Description: Абстрактная реализация twitter-сервиса.
 */
trait TwitterService
  extends IExtService
  with OAuth1Support
  with TwMpUpload
  with PlayMacroLogsImpl
{

  /** Ссылка на главную твиттера, и на собственный акк, если юзер залогинен. */
  override def mainPageUrl = "https://twitter.com/"

  override protected def _loggerClass = classOf[TwitterService]

  override def nameI18N = "Twitter"

  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?twitter\\.com".r.pattern.matcher(host).matches()
  }
  override def dfltTargetUrl = Some(mainPageUrl)

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  override def postImgSzWithName(n: String) = TwImgSizes.maybeWithName(n)

  override def advPostMaxSz(tgUrl: String) = TwImgSizes.Photo

  /** Префикс ключей конфигурации. Конфиг расшарен с secure-social. */
  def confPrefix = "securesocial.twitter"

  /** twitter работает через OAuth1. */
  override def extAdvServiceActorFactoryCt = ClassTag( classOf[OAuth1ServiceActorFactory] )

  /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
  override def oauth1Support = Some(this)

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  override def myUserName = Some("@suggest_io")

  /**
    * Генератор HTML-мета-тегов для описания рекламной карточки.
    *
    * @param mad1 Экземпляр рекламной карточки.
    * @return экземпляры моделй, готовых для запуска рендера.
    */
  override def adMetaTagsRender(mad1: MNode)(implicit ec: ExecutionContext): Future[List[IRenderable]] = {
    val acc0Fut = super.adMetaTagsRender(mad1)
    // Собираем через враппер, т.к. для генерации метаданных нужен доступ к Context.
    val ir = new IRenderable {
      override def render()(implicit ctx: Context): Html = {
        // Калькулятор ссылки
        val calc = new Mad2ImgUrlCalc {
          override def mad            = mad1
          override def tgUrl          = mainPageUrl
          override def adRenderMaxSz  = TwImgSizes.Photo
          override def service        = MExtServices.TWITTER
          override val madRenderInfo  = super.madRenderInfo
        }
        // Аргументы для рендера карточки и рендер.
        val pca = PhotoCardArgs(
          imgUrl = routes.MarketShowcase.onlyOneAdAsImage( calc.adRenderArgs ).url,
          url    = Some(ctx.request.uri),
          title  = mad1.guessDisplayName,                // TODO Брать из карточки.
          imgWh  = Some(calc.madRenderInfo)
        )
        pca.render()
      }
    }
    acc0Fut map { acc0 =>
      ir :: acc0
    }
  }

  /** Поддержка аплоада в твиттер. */
  override def maybeMpUpload = Some(this)

  /** Дефолтовый формат изображения, если не переопределен в конфиге. */
  override def imgFmtDflt = OutImgFmts.PNG

  /** В твиттер надо всегда постить горизонтальные карточки. */
  override def isAdvExtWide(mad: MNode) = true

}
