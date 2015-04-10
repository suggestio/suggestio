package models.msc

import models._
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:16
 * Description: qs-аргументы запроса к sc/index.
 */

object ScReqArgs {

  val GEO_SUF               = ".geo"
  val SCREEN_SUF            = ".screen"
  val WITH_WELCOME_SUF      = ".wc"

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],
                   intOptB: QueryStringBindable[Option[Int]],
                   devScreenB: QueryStringBindable[Option[DevScreen]],
                   boolOptB: QueryStringBindable[Option[Boolean]] ): QueryStringBindable[ScReqArgs] = {
    new QueryStringBindable[ScReqArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScReqArgs]] = {
        for {
          maybeGeo                <- strOptB.bind(key + GEO_SUF, params)
          maybeDevScreen          <- devScreenB.bind(key + SCREEN_SUF, params)
          maybeWithWelcomeAd      <- intOptB.bind(key + WITH_WELCOME_SUF, params)
        } yield {
          Right(new ScReqArgsDflt {
            override val geo = {
              GeoMode.maybeApply(maybeGeo)
                .filter(_.isWithGeo)
                .getOrElse(GeoIp)
            }
            // Игнорим неверные размеры, ибо некритично.
            override lazy val screen: Option[DevScreen] = maybeDevScreen
            override val withWelcomeAd: Boolean = {
              maybeWithWelcomeAd.fold(
                {_ => true},
                {vOpt => vOpt.isEmpty || vOpt.get > 0}
              )
            }
          })
        }
      }

      override def unbind(key: String, value: ScReqArgs): String = {
        List(
          strOptB.unbind(key + GEO_SUF, value.geo.toQsStringOpt),
          devScreenB.unbind(key + SCREEN_SUF, value.screen),
          intOptB.unbind(key + WITH_WELCOME_SUF, if (value.withWelcomeAd) None else Some(0))
        )
          .filter { us => !us.isEmpty }
          .mkString("&")
      }
    }
  }

  def empty: ScReqArgs = new ScReqArgsDflt {}

}

trait ScReqArgs extends SyncRenderInfo {
  def geo                 : GeoMode
  def screen              : Option[DevScreen]
  def withWelcomeAd       : Boolean
  /** Заинлайненные отрендеренные элементы плитки. Передаются при внутренних рендерах, вне HTTP-запросов и прочего. */
  def inlineTiles         : Seq[RenderedAdBlock]
  def focusedContent      : Option[Html]
  def inlineNodesList     : Option[Html]
  /** Текущая нода согласно геоопределению, если есть. */
  def adnNodeCurrentGeo   : Option[MAdnNode]

  override def toString: String = {
    import QueryStringBindable._
    ScReqArgs.qsb.unbind("a", this)
  }
}
trait ScReqArgsDflt extends ScReqArgs with SyncRenderInfoDflt {
  override def geo                  : GeoMode = GeoNone
  override def screen               : Option[DevScreen] = None
  override def inlineTiles          : Seq[RenderedAdBlock] = Nil
  override def focusedContent       : Option[Html] = None
  override def inlineNodesList      : Option[Html] = None
  override def adnNodeCurrentGeo    : Option[MAdnNode] = None
  override def withWelcomeAd        : Boolean = true
}
/** Враппер [[ScReqArgs]] для имитации вызова copy(). */
trait ScReqArgsWrapper extends ScReqArgs {
  def reqArgsUnderlying: ScReqArgs
  override def geo                  = reqArgsUnderlying.geo
  override def screen               = reqArgsUnderlying.screen
  override def inlineTiles          = reqArgsUnderlying.inlineTiles
  override def focusedContent       = reqArgsUnderlying.focusedContent
  override def inlineNodesList      = reqArgsUnderlying.inlineNodesList
  override def adnNodeCurrentGeo    = reqArgsUnderlying.adnNodeCurrentGeo
  override def withWelcomeAd        = reqArgsUnderlying.withWelcomeAd

  override def jsStateOpt           = reqArgsUnderlying.jsStateOpt
}

