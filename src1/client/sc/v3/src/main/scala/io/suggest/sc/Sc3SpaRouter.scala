package io.suggest.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.boot.MSpaRouterState
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.spa.{SioPages, SioPagesUtil}
import io.suggest.text.UrlUtil2
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, Redirect, Router, RouterConfigDsl}
import japgolly.scalajs.react.vdom.html_<^._

import java.net.URI

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.17 14:25
  * Description: react-роутер для [[Sc3Main]].
  * В отличии от scalajs-spa-tutorial, этот роутер живёт за пределами [[Sc3Main]], чтобы не разводить кашу в коде.
  */
class Sc3SpaRouter(
                    renderSc3F        : (SioPages.Sc3) => VdomElement,
                  )
  extends Log
{

  /** Всё состояние роутера и связанные данные живут здесь: */
  val state: MSpaRouterState = RouterConfigDsl[SioPages.Sc3].use { dsl =>
    import dsl._

    // в cordova не будет работать адресация без # и без исходного длинного path.
    val helper = if (CordovaConstants.isCordovaPlatform())
      new ScRouterUrlHelper.UrlHash
    else
      new ScRouterUrlHelper.PlainUrl

    // Конфиг роутера. TODO Надо qs перегонять в JSON и переводить в состояние. И обратно, без рукопашных парсеров/токенайзеров.
    val mainScreenOptRoute = ("?" ~ string(".+"))
      .option
      .pmap { qsOpt =>
        qsOpt.map( SioPagesUtil.parseSc3FromQs )
      } { mainScreen =>
        val queryString = SioPagesUtil.sc3ToQs( mainScreen )
        Some( queryString )
      }
      .option

    // Сразу парсим роуту, т.к. она передаётся в circuit.
    val canonicalRoute = try {
      (for {
        link        <- DocumentVm().head.links
        if link.isCanonical
        href        <- link.href
        urlHash2    <- helper.getUrlHash( href )
        parsed <- {
          val r = mainScreenOptRoute.route
            .parse( Path(urlHash2) )
            .flatten
          if (r.isEmpty)
            logger.error( ErrorMsgs.URL_PARSE_ERROR, msg = urlHash2 )
          r
        }
      } yield {
        parsed
      })
        .nextOption()
    } catch {
      case ex: Throwable =>
        logger.error( ErrorMsgs.CANONICAL_URL_FAILURE, ex )
        None
    }

    // var-флаг для подавления повторных десериализаций состояния из link rel=canonical.
    // Выставляется в true после подобной десериализации.
    var isAlreadyUsedCanonical = false
    // Поддержка 3p-доменов: выдача тут инициализируется через link rel canonical.
    val mainScreenDfltRoute = mainScreenOptRoute
      .withDefault {
        // Залезть в link rel=canonical и распарсить там чего-нибудь.
        // Это используется при подключении сторонних доменов к s.io на уровне siteTpl.
        OptionUtil.maybeOpt( !isAlreadyUsedCanonical ) {
          isAlreadyUsedCanonical = true
          canonicalRoute
        }
          .getOrElse( SioPages.Sc3.empty )
      }

    // Кэшируем компонент ScRootR вне функций роутера, т.к. за ним следит только Sc3Circuit, а не роутер.
    val mainScreenRule = dynamicRouteCT[SioPages.Sc3](mainScreenDfltRoute) ~> dynRender( renderSc3F )

    val routerCfg = mainScreenRule
      .notFound {
        redirectToPage( SioPages.Sc3.empty )( Redirect.Replace )
      }

    val (r, rCtl) = Router.componentAndCtl[SioPages.Sc3](
      baseUrl = helper.baseUrl,
      cfg     = routerCfg
    )

    MSpaRouterState(
      router          = r,
      routerCtl       = rCtl,
      canonicalRoute  = canonicalRoute
    )
  }

}


trait ScRouterUrlHelper {
  def baseUrl: BaseUrl
  def getUrlHash(url: String): Option[String]
}

object ScRouterUrlHelper {

  /** В кордове URL начинается с file, поэтому любые переменные части должны быть после #. */
  class UrlHash extends ScRouterUrlHelper {
    override def baseUrl = BaseUrl.until_# + UrlUtil2.URL_HASH_PREFIX_NOQS
    override def getUrlHash(url: String): Option[String] = {
      for {
        urlHash <- UrlUtil2.getUrlHash(url)
        if urlHash.nonEmpty
      } yield {
        urlHash.replace(UrlUtil2.URL_HASH_PREFIX_NOQS, "") + "&"
      }
    }
  }

  /** URL-адресация без #, когда напрямую редактируется серверная ссылка. */
  class PlainUrl extends ScRouterUrlHelper {
    override def baseUrl = BaseUrl.fromWindowOrigin_/
    override def getUrlHash(url: String): Option[String] = {
      val hrefUrl = new URI( url )
      for {
        urlQuery     <- Option( hrefUrl.getRawQuery )
        if urlQuery.nonEmpty
      } yield {
        "?" + urlQuery + "&"
      }
    }
  }

}
