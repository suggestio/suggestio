package models.mext.tw

import _root_.util.adv.OAuth1ServiceActor
import controllers.routes
import models.blk.OneAdQsArgs
import models.im.OutImgFmts
import models.mext.tw.card.PhotoCardArgs
import models.{Context, IRenderable, MAd}
import models.mext.IExtService
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:14
 * Description: Абстрактнаня реализация twitter-сервиса.
 */
trait TwitterService extends IExtService {

  override def nameI18N = "Twitter"

  override def isForHost(host: String): Boolean = {
    "(?i)(www\\.)?twitter\\.com".r.pattern.matcher(host).matches()
  }
  override def dfltTargetUrl = None

  /** Найти стандартный (в рамках сервиса) размер картинки. */
  override def postImgSzWithName(n: String) = ???  // TODO

  override def advPostMaxSz(tgUrl: String) = ???    // TODO

  /** Префикс ключей конфигурации. Конфиг расшарен с secure-social. */
  def confPrefix = "securesocial.twitter"

  /** twitter работает через OAuth1. */
  override def extAdvServiceActor = OAuth1ServiceActor

  lazy val oa1Support = new OAuth1Support(confPrefix)

  /** Поддержка OAuth1 на текущем сервисе. None означает, что нет поддержки. */
  override def oauth1Support = Some(oa1Support)

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  override def myUserName = Some("@suggest_io")

  /**
   * Генератор HTML-мета-тегов для описания рекламной карточки.
   * @param mad Экземпляр рекламной карточки.
   * @return экземпляры моделй, готовых для запуска рендера.
   */
  override def adMetaTagsRender(mad: MAd)(implicit ec: ExecutionContext): Future[List[IRenderable]] = {
    val acc0Fut = super.adMetaTagsRender(mad)
    // TODO Нужно задействовать wide-рендер.
    val oneAdQs = OneAdQsArgs(
      adId   = mad.id.get,
      szMult = 1.0F,
      vsnOpt = mad.versionOpt,
      imgFmt = OutImgFmts.JPEG
    )
    val ir = new IRenderable {
      override def render()(implicit ctx: Context): Html = {
        val pca = PhotoCardArgs(
          imgUrl = routes.MarketShowcase.onlyOneAdAsImage(oneAdQs).url,
          url    = Some(ctx.request.uri),   // TODO Нужен нормальный URL, а не это.
          title  = Some("TODO"),    // TODO Брать из карточки.
          imgWh  = None             // TODO Высчитывать.
        )
        pca.render()
      }
    }
    acc0Fut map { acc0 =>
      ir :: acc0
    }
  }

}
