package controllers

import com.google.inject.Inject
import controllers.cstatic.{CorsPreflight, RobotsTxt, SiteMapsXml}
import models.mproj.ICommonDi
import play.api.Play.isProd
import play.api.mvc._
import util.acl.{IsAuth, IsSuperuserOrDevelOr404, MaybeAuth}
import util.seo.SiteMapUtil
import util.xplay.SecHeadersFilter
import views.html.static._

/**
 * Authors: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 *          Alexander Pestrikov <alexander.pestrikov@cbca.ru>
 * Date: 16.05.13 13:34
 * Статика всякая.
 * 2014.oct.24: Вычищение старой верстки. Ссылки на неё всплывают в поисковиках.
 */

class Static @Inject() (
  override val siteMapUtil        : SiteMapUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MaybeAuth
  with IsSuperuserOrDevelOr404
  with IsAuth
  with RobotsTxt
  with SiteMapsXml
  with CorsPreflight
{

  import mCommonDi._

  private def booklet = routes.Market.marketBooklet().url

  /** Страница /about. Раньше там были слайд-презентация s.io live search. */
  def about = Action { implicit request =>
    MovedPermanently(booklet)
  }

  /** Страница /showcase. Там была плитка из превьюшек сайтов, которые используют s.io live search. */
  def showcase = Action { implicit request =>
    MovedPermanently(booklet)
  }


  /** Страница /help. Пока редирект на буклет. Когда помощь появится, то её корень лучше всего сделать тут. */
  def help = Action { implicit request =>
    cacheControlShort {
      Redirect(booklet)
    }
  }

  /** Тематические страницы помощи. Пока рендерим буклет, т.к. другой инфы нет. */
  def helpPage(page:String) = Action { implicit request =>
    // 2014.oct.24: в sio1 live search тут были страницы: "registration", "search_settings", "images_settings", "design_settings", "setup".
    cacheControlShort {
      Redirect(booklet)
    }
  }

  /** Удаление blog-контроллера привело к тому, что часть ссылок в поисковиках накрылась медным тазом.
    * Тут -- compat-костыль для редиректа обращений к блогу на буклет. */
  def blogIndex = Action { implicit request =>
    cacheControlShort {
      Redirect(booklet)
    }
  }
  def blogPage(path: String) = blogIndex


  /**
   * Страница с политикой приватности.
   * @return 200 Ok и много букв.
   */
  def privacyPolicy = MaybeAuth { implicit request =>
    Ok( privacyPolicyTpl() )
      .withHeaders( CACHE_CONTROL -> "public, max-age=600" )
  }

  /** Содержимое проверочного попап-окна. */
  def popupCheckContent = MaybeAuth { implicit request =>
    Ok(popups.popupCheckTpl()).withHeaders(
      CACHE_CONTROL -> "public, max-age=86400"
    )
  }

  /**
   * Костыль в связи с проблемами в play-html-compressor в play-2.3 https://github.com/mohiva/play-html-compressor/issues/20
   * Без этого костыля, запрос html'ки просто подвисает.
   */
  def tinymceColorpicker(filename: String) = MaybeAuth { implicit request =>
    Ok(tinymce.colorpicker.indexTpl())
      .withHeaders(
        CACHE_CONTROL -> "public, max-age=3600",
        SecHeadersFilter.X_FRAME_OPTIONS_HEADER -> "SAMEORIGIN"
      )
  }

  /**
   * Доступ к привилегированным ассетам: js.map и прочие сорцы.
   * @param path Путь.
   * @param asset filename.
   * @return Экшен раздачи ассетов с сильно урезанным кешированием на клиенте.
   */
  def vassetsSudo(path: String, asset: Assets.Asset) = IsSuperuserOrDevelOr404.async { implicit request =>
    // TODO Запретить раздачу привелигированных ассетов через CDN в продакшене? Чтобы отладка главной страницы шла только по vpn.
    val resFut = Assets.versioned(path, asset)(request)
    // Для привелегированных ассетов нужно запретить промежуточные кеширования.
    resFut.map { res =>
      val ttl = if (isProd) 300 else 10
      res.withHeaders(CACHE_CONTROL -> s"private, max-age=$ttl")
    }
  }

  def assetsSudo(path: String, asset: Assets.Asset) = vassetsSudo(path, asset)


  /**
   * Экшен для скрытого продления сессии в фоне. Может дергаться в js'ом незаметно.
   * @return 204 No Content - всё ок.
   *         Другой код - сессия истекла.
   */
  def keepAliveSession = IsAuth { implicit request =>
    NoContent
  }

}
