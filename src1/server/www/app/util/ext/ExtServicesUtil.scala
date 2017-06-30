package util.ext

import javax.inject.{Inject, Singleton}
import models.mext.{IExtService, MExtServices}
import models.mproj.ICommonDi

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 12:14
  * Description: Утиль для овладевания хелперами внешних сервисов.
  */
@Singleton
class ExtServicesUtil @Inject() (
  mCommonDi     : ICommonDi
) {

  /** Все доступные сервис-хелперы, объявленные в модели сервисов.
    *
    * Для лени, можно сделать lazy val + List, либо val + Stream.
    * Для отладки или минимизации ресурсов/логики лучше всего val + List.
    */
  val HELPERS: Seq[IExtServiceHelper] = {
    MExtServices.valuesT
      .iterator
      .map { msvc =>
        mCommonDi.current
          .injector
          .instanceOf( msvc.helperCt )
      }
      .toList
  }


  /**
   * Поиск подходящего сервиса для указанного хоста.
   * @param host Хостнейм искомого сервиса.
   * @return Сервис, если такой есть.
   */
  def findForHost(host: String): Option[IExtServiceHelper] = {
    HELPERS.find( _.isForHost(host) )
  }

  def helperFor(mExtService: IExtService): Option[IExtServiceHelper] = {
    HELPERS.find(_.mExtService == mExtService)
  }

}


/** Интерфейс для DI-поля с инстансом [[ExtServicesUtil]]. */
trait IExtServicesUtilDi {
  def extServicesUtil: ExtServicesUtil
}
