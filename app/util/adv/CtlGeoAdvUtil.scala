package util.adv

import java.sql.Connection
import java.util.Currency

import models.{MBillMmpDaily, MAdvReq, MAdvI, MAdvStatic}
import play.api.db.Database
import util.async.AsyncUtil
import util.async.AsyncUtil.jdbcAsync

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.15 10:40
 * Description: Контроллерская утиль для adv-данных.
 * Для ускорения работы с SQL-базой было включено активное распараллеливание.
 */

class CtlGeoAdvUtil {

  def LIMIT_DFLT = -1

  /**
   * Абстрактный поиск в абстрактной adv-модели.
   * @param model Adv-модель.
   * @param limit Лимит результатов. Если <= 0, то дефолтовое значение.
   * @param f Функция вызова поиска.
   * @tparam T Тип возвращаемых значений adv-модели.
   * @return Фьючерс со списком результатов.
   */
  def advFind[T](model: MAdvStatic, limit: Int = LIMIT_DFLT)
                (f: (Int, Connection) => T)
                (implicit db: Database): Future[T] = {
    val limit1 = if (limit > 0) limit else model.LIMIT_DFLT
    Future {
      db.withConnection { implicit c =>
        f(limit1, c)
      }
    }(AsyncUtil.jdbcExecutionContext)
  }

  /**
   * Асинхронная сборка данных по заблокированным деньгам.
   * @param adId id рекламной карточки.
   * @return Фьючерс с картой результатов.
   */
  def collectBlockedSums(adId: String)(implicit db: Database): Future[ List[(Float, Currency)] ] = {
    Future {
      db.withConnection { implicit c =>
        MAdvReq.calculateBlockedSumForAd(adId)
      }
    }(AsyncUtil.jdbcExecutionContext)
  }

  /**
   * Абстрактный поиск в Adv-модели по adId.
   * @param model Модель.
   * @param adId id рекламной карточки.
   * @param limit лимит результатов. Если <= 0, то используется дефолтовый для модели.
   * @tparam T1 Тип возвращаемого значения.
   * @return Фьючерс со списком экземпляров указанной модели.
   */
  def advFindByAdId[T1 <: MAdvI](model: MAdvStatic {type T = T1}, adId: String, limit: Int = LIMIT_DFLT)
                                (implicit db: Database): Future[List[T1]] = {
    advFind(model, limit) { (limit1, c) =>
      model.findByAdId(adId, limit = limit1)(c)
    }
  }

  /**
   * Абстрактный асинхронный поиск по adv-модели по adId.
   * @param model Модель.
   * @param adId id рекламной карточки.
   * @param limit Лимит результатов.
   * @tparam T1 Тип возвращаемых экземпляров модели.
   * @return Фьючерс со списком результатов.
   */
  def advFindNonExpiredByAdId[T1 <: MAdvI](model: MAdvStatic {type T = T1}, adId: String, limit: Int = LIMIT_DFLT)
                                          (implicit db: Database): Future[List[T1]] = {
    advFind(model, limit) { (limit1, c) =>
      model.findNotExpiredByAdId(adId, limit = limit1)(c)
    }
  }


  def findAdnIdsMmpReady()(implicit db: Database): Future[List[String]] = {
    Future {
      db.withConnection { implicit c =>
        MBillMmpDaily.findAllAdnIds
      }
    }(AsyncUtil.jdbcExecutionContext)
  }

}

