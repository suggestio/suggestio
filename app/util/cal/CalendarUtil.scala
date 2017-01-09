package util.cal

import java.net.URL

import com.google.inject.{Inject, Singleton}
import controllers.routes
import de.jollyday.HolidayManager
import de.jollyday.parameter.UrlManagerParameter
import io.suggest.async.AsyncUtil
import models.mcal.{MCalCtx, MCalendars, MCalsCtx}
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 22:32
  * Description: Утиль для календарей.
  * Изначально жила внутри биллинга прямых размещений первого поколения биллинга.
  */
@Singleton
class CalendarUtil @Inject() (
  mCalendars              : MCalendars,
  asyncUtil               : AsyncUtil,
  mCommonDi               : ICommonDi
) {

  import mCommonDi._

  /**
    * Ссылка на самого себя для нужд календарей.
    * Передать в менеджер календарей календарь пока получилось только через абсолютный URL
    * к SysCalendar-контроллеру.
    */
  private val MYSELF_URL_PREFIX: String = configuration.getString("mmp.daily.localhost.url.prefix") getOrElse {
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
  def getCalsCtx(calIds: Traversable[String]): Future[MCalsCtx] = {
    // TODO Opt Кеш ОЧЕНЬ нужен для multiGet на стороне MCalendar. Вообще лучше эту модель запилить внутрь MNod и заюзать mNodeCache
    val mcalsFut = mCalendars.multiGetMap(calIds)
    // TODO Получать по multiGet календари пачкой из MCalendars, заворачивать их в HolydayManager'ы.
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
