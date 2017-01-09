package io.suggest.ahc.util

import java.util.concurrent.Executor

import io.suggest.util.SioFutureUtil.RunnableListenableFutureWrapper
import org.asynchttpclient.{ListenableFuture, Response => AHCResponse}
import play.api.libs.ws.ahc.AhcWSResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.05.15 14:44
 * Description: Костыли для взаимодействия с Ning Async-http-client.
 */
object NingUtil {

  /**
   * Завернуть ning.Future в scala.Future.
   * @param nfut Исходный фьючерс.
   * @param ec Контекст исполнения.
   * @tparam T Тип значения фьючерса.
   * @return Экземпляр scala Future.
   */
  implicit def ningFut2scalaFut[T](nfut: ListenableFuture[T])(implicit ec: ExecutionContext): Future[T] = {
    val w = new RunnableListenableFutureWrapper[T] {
      override def _ec: ExecutionContext = ec
      override def getValue: T = nfut.get()
      override def addListener(runnable: Runnable, executor: Executor): Unit = {
        nfut.addListener(runnable, executor)
      }
    }
    w.future
  }


  /**
   * Конверсия фьючерса ответа для ning-запроса с заворачиваем в экземпляр NingWSResponse.
   * @param nfut Исходный фьючерс.
   * @param ec thread pool.
   * @return Фьючерс с ответом.
   */
  implicit def ningFut2wsScalaFut(nfut: ListenableFuture[AHCResponse])
                                 (implicit ec: ExecutionContext): Future[AhcWSResponse] = {
    ningFut2scalaFut(nfut)
      .map { AhcWSResponse.apply }
  }

}
