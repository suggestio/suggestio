package models.msc

import io.suggest.common.menum.EnumMaybeWithId
import models.mctx.Context
import play.api.mvc.QueryStringBindable
import play.twirl.api.{Template2, Html}
import util.PlayMacroLogsImpl
import views.html.sc.script._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 13:35
 * Description: Версии API системы выдачи, чтобы сервер мог подстраиватсья под клиентов разных поколений.
 */
object MScApiVsns extends Enumeration with EnumMaybeWithId with PlayMacroLogsImpl {

  /** Экземпляр модели версий. */
  protected[this] abstract class Val(val versionNumber: Int) extends super.Val(versionNumber) {
    override def toString(): String = id.toString

    /** Всегда рендерить инструменты для закрытия выдачи. */
    // TODO Удалить вместе с Coffee API
    def forceScCloseable: Boolean

    /** По шаблонам должны быть распиханы URL для ajax-запросов? */
    def renderActionUrls: Boolean

    /** Сервер сам сочиняет id блоков при рендере? */
    def serverSideBlockIds: Boolean

    /** В v1 была чрезвычайно эффективная адресация div'ов узлов в node list. */
    def geoNodeIdAsClass: Boolean = false

    /** Сервер ли отвечает за развернутость текущего слоя в списке узлов? */
    def nodeListLayersServerSideExpand: Boolean

    /** Нужно ли при начальном рендере рендерить также базовое содержимое smFocusedAds (пустая карусель и проч.) */
    def withEarlyFocAdsRootContainers: Boolean

    /** Шаблон для рендера скрипта выдачи. */
    def scriptTpl: Template2[IScScriptRenderArgs, Context, Html]

    /** Тут возможность отключения рендера DOCTYPE в sc/siteTpl. */
    def withScSiteDoctype: Boolean = true

    /** Можно добавки в head затолкать. */
    def headAfterHtmlOpt: Option[Template2[IScScriptRenderArgs, Context, Html]] = None

    /** Какой id suffix использовать для focused-карточек? */
    def scFocIdSuffix(args: IAdBodyTplArgs): Any
  }

  override type T = Val


  /** Выдача, написанная одним файлом на coffee-script. Со временем будет удалена. */
  val Coffee: T = new Val(1) {

    override def forceScCloseable = true
    override def renderActionUrls  = true

    /** Coffee-версия сама придумывает id по-порядку и управляет ими. */
    override def serverSideBlockIds = false
    override def geoNodeIdAsClass   = true
    override def nodeListLayersServerSideExpand = false
    override def withEarlyFocAdsRootContainers = false

    /** У v1 часть скрипта заинлайнена в шаблоне. */
    override def scriptTpl = _scriptV1Tpl

    /** В sc v1 что-то ломается, и у контейнера плитки появляется внизу белая полоса на 10-15% экрана.  */
    override def withScSiteDoctype = false

    /** id focused-карточек заканчивается порядковым индексом карточки. */
    override def scFocIdSuffix(args: IAdBodyTplArgs): Any = {
      args.index
    }

  }


  /** Выдача, переписанная на scala.js. Исходная версия. */
  val Sjs1: T = new Val(2) {

    /** Рендерить утиль для "закрытия" выдачи нужно только при реальной необходимости. */
    override def forceScCloseable = false

    /** sc-sjs использует jsRoutes для сборки ссылок. Полуготовые ссылки ей не нужны. */
    override def renderActionUrls = false

    /** sc-sjs опирается на id блоков, сформированных сервером на основе id карточек. */
    override def serverSideBlockIds = true

    /** sc-sjs слушается сервера на тему списка узлов. */
    override def nodeListLayersServerSideExpand = true

    /** sc-sjs требует ранние контейнеры для focused ads и прочее. */
    override def withEarlyFocAdsRootContainers = true

    /** У v2-выдачи своя особая магия. */
    override def scriptTpl = _scriptV2Tpl

    override def headAfterHtmlOpt = Some( _headAfterV2Tpl )

    /** Изначально id'шники focused-карточек суффиксировались по-старинке через  */
    override def scFocIdSuffix(args: IAdBodyTplArgs): Any = {
      // Без get на случай вызова preview в редакторе карточек при создании карточки: там id неизвестен.
      args.brArgs.mad.id.getOrElse {
        LOGGER.debug("scFocIdSuffix(): Unexpected empty mad.id for: " + args)
        ""
      }
    }

  }


  /** Какую версию использовать, если версия API не указана? */
  def unknownVsn: T = {
    Coffee
  }

  /** Биндинги для url query string. */
  implicit def qsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] = {
    new QueryStringBindable[MScApiVsn] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScApiVsn]] = {
        val optRes = for {
          maybeVsn <- intB.bind(key, params)
        } yield {
          maybeVsn.right.flatMap { vsnNum =>
            maybeWithId(vsnNum) match {
              case Some(vsn) =>
                Right(vsn)
              case None =>
                // Довольно неожиданная ситуация, что выкинута версия, используемая на клиентах. Или ксакеп какой-то ковыряется.
                val msg = "Unknown API version: " + vsnNum
                LOGGER.warn(msg, new Throwable)
                Left(msg)
            }
          }
        }
        // Если версия не задана вообще, то выставить её в дефолтовую. Первая выдача не возвращала никаких версий.
        optRes.orElse {
          Some( Right(unknownVsn) )
        }
      }

      override def unbind(key: String, value: MScApiVsn): String = {
        intB.unbind(key, value.versionNumber)
      }
    }
  }

}
