package util.cal

import java.net.URL
import javax.inject.Inject
import controllers.routes
import de.jollyday.HolidayManager
import de.jollyday.parameter.UrlManagerParameter
import io.suggest.async.AsyncUtil
import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import io.suggest.playx.CacheApiUtil
import models.mcal.{MCalCtx, MCalsCtx}
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 22:32
  * Description: Утиль для календарей.
  * Изначально жила внутри биллинга прямых размещений первого поколения биллинга.
  */
final class CalendarUtil @Inject() (
                                     injector                : Injector,
                                   ) {

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val asyncUtil = injector.instanceOf[AsyncUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val configuration = injector.instanceOf[Configuration]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /**
    * Ссылка на самого себя для нужд календарей.
    * Передать в менеджер календарей календарь пока получилось только через абсолютный URL
    * к SysCalendar-контроллеру.
    */
  private val MYSELF_URL_PREFIX: String = configuration.getOptional[String]("mmp.daily.localhost.url.prefix").getOrElse {
    val myPort = Option(System.getProperty("http.port")).fold(9000)(_.toInt)
    s"http://localhost:$myPort"
  }


  /** Сгенерить localhost-ссылку на xml-текст календаря. */
  // Часть блокировок подавляет кеш на стороне jollyday.
  def getCalMgr(calId: String): Future[HolidayManager] = {
    // 2015.dec.25: Усилены асинхронность и кеширование, т.к. под высоко-параллельной тут возникал deadlock,
    // а jollyday-кеш (v0.4.x) это не ловил это, а блокировал всё.
    // TODO Нужно зарегать свой протокол для URL, который возвращает тексты календарей. Замудрёная архитектура jollyday мешает сделать какую-нить упрощенку здесь.
    import scala.concurrent.duration._
    cacheApiUtil.getOrElseFut(calId + ".holyman", expiration = 2.minute) {
      val calUrl = new URL(MYSELF_URL_PREFIX + routes.SysCalendar.getCalendarXml(calId))
      val args = new UrlManagerParameter(calUrl, null)
      Future {
        HolidayManager.getInstance(args)
      }(asyncUtil.singleThreadCpuContext)
    }
  }


  /** Сборка контекста календарей цен размещения.
    *
    * @param calIds id календарей, из которых требуется сформировать контекст.
    * @return Фьючерс с инстансом контекста.
    */
  def getCalsCtx(calIds: Iterable[String]): Future[MCalsCtx] = {
    import esModel.api._

    val mcalsFut = mNodes.multiGetMapCache( calIds )

    val calsFut = Future.traverse( calIds.toSeq ) { calId =>
      for {
        mgr   <- getCalMgr(calId)
        mcals <- mcalsFut
      } yield {
        calId -> MCalCtx(calId, mcals(calId), mgr)
      }
    }

    for (cals <- calsFut) yield {
      MCalsCtx( cals.toMap )
    }
  }

}
