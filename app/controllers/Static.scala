package controllers

/**
 * User: Alexander Pestrikov <alexander.pestrikov@cbca.ru>
 * Date: 16.05.13
 * Time: 13:34
 * Статика всякая.
 * 2014.oct.24: Вычищение старой верстки. Ссылки на неё всплывают в поисковиках.
 */

import play.api.mvc._
import util.ContextT
import util.acl._
import views.html.static._

object Static extends Controller with ContextT {

  private def booklet = routes.Market.marketBooklet().url

  /** Страница /about. Раньше там были слайд-презентация s.io live search. */
  def about = Action { implicit request =>
    MovedPermanently(booklet)
  }

  /** Страница /showcase. Там была плитка из превьюшек сайтов, которые используют s.io live search. */
  def showcase = Action { implicit request =>
    MovedPermanently(booklet)
  }


  def badbrowser = MaybeAuth { implicit request =>
    Ok(badbrowserTpl())
  }


  /** Страница /help. Пока редирект на буклет. Когда помощь появится, то её корень лучше всего сделать тут. */
  def help = MaybeAuth { implicit request =>
    Redirect(booklet)
  }

  /** Тематические страницы помощи. Пока рендерим буклет, т.к. другой инфы нет. */
  def helpPage(page:String) = MaybeAuth { implicit request =>
    // 2014.oct.24: в sio1 live search тут были страницы: "registration", "search_settings", "images_settings", "design_settings", "setup".
    Redirect(booklet)
  }


  /**
   * Костыль в связи с проблемами в play-html-compressor в play-2.3 https://github.com/mohiva/play-html-compressor/issues/20
   * Без этого костыля, запрос html'ки просто подвисает.
   */
  def tinymceColorpicker = Action { implicit request =>
    Ok(tinymce.colorpicker.indexTpl())
      .withHeaders(CACHE_CONTROL -> "public, max-age=3600")
  }

}