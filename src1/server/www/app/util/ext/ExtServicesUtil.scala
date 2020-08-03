package util.ext

import io.suggest.ext.svc.{MExtService, MExtServices}
import javax.inject.{Inject, Singleton}
import models.mctx.Context
import models.mext.MExtServicesJvm
import play.api.inject.Injector

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 12:14
  * Description: Утиль для овладевания хелперами внешних сервисов.
  */
@Singleton
class ExtServicesUtil @Inject() (
                                  injector: Injector,
                                ) {

  private def _buildHelpers(services: MExtService*): Iterator[(MExtService, IExtServiceHelper)] = {
    for {
      msvc <- services.iterator
      if msvc.hasAdvExt
    } yield {
      val msvcJvm = MExtServicesJvm.forService( msvc )
      val helper = injector.instanceOf( msvcJvm.advExt.helperCt )
      msvc -> helper
    }
  }

  /** Все доступные сервис-хелперы, объявленные в модели сервисов.
    *
    * Для лени, можно сделать lazy val + List, либо val + Stream.
    * Для отладки или минимизации ресурсов/логики лучше всего val + List.
    */
  def HELPERS = _buildHelpers( MExtServices.values: _* )


  /**
   * Поиск подходящего сервиса для указанного хоста.
   * @param host Хостнейм искомого сервиса.
   * @return Сервис, если такой есть.
   */
  def findForHost(host: String): Option[IExtServiceHelper] = {
    (for {
      (_, helper) <- HELPERS
      if helper.isForHost(host)
    } yield helper)
      .nextOption()
  }

  def helperFor(mExtService: MExtService): Option[IExtServiceHelper] = {
    _buildHelpers( mExtService )
      .map(_._2)
      .nextOption()
  }


  def applicationUrl(extSvc: MExtService, appId: String)(implicit ctx: => Context): String = {
    extSvc match {
      case MExtServices.GooglePlay =>
        s"${extSvc.mainPageUrl}store/apps/details?id=$appId"
      case MExtServices.AppleITunes =>
        val lang = ctx.messages.lang
        val countryCode = Option( lang.country )
          .filter(_.nonEmpty)
          .getOrElse( lang.code.replaceFirst("-[a-zA-Z]+$", "") )
        s"${extSvc.mainPageUrl}${countryCode}/app/suggest/${appId}"
      case _ =>
        throw new UnsupportedOperationException(s"$extSvc does not support apps distibution.")
    }
  }

}


/** Интерфейс для DI-поля с инстансом [[ExtServicesUtil]]. */
trait IExtServicesUtilDi {
  def extServicesUtil: ExtServicesUtil
}
