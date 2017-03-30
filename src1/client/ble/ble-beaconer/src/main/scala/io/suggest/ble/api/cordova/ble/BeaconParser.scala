package io.suggest.ble.api.cordova.ble

import evothings.ble.DeviceInfo
import io.suggest.ble.BeaconSignal
import io.suggest.primo.{IApply1, TypeT}
import io.suggest.sjs.common.log.ILog
import io.suggest.sjs.common.msg.ErrorMsg_t

import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 11:20
  * Description: Абстрактный парсер cordova-ble-данных маячка.
  */
trait BeaconParser extends TypeT with ILog {

  override type T <: BeaconSignal

  /** Алиас для типа возвращаемого результата парсинга. */
  type ParseRes_t = Option[Either[Any, T]]


  /** Инстанс в cordova-ble device info. */
  def dev: DeviceInfo

  /** Пропарсить dev в опциональный инстанс маячка.
    * @return None если это не маячок.
    *         Some() если это маячок T.
    *         exception, если это какой-то кривой маячок или ошибка где-то в логике метода.
    */
  def parse(): ParseRes_t

  /** Какой код сообщения об ошибке логгировать при исключении, возникшем при парсинге. */
  def parserFailMsg: ErrorMsg_t

  /** Безопасный вызов parse(), никогда не возвращает exception. */
  def tryParse(): ParseRes_t = {
    try {
      parse()
    } catch {
      case ex: Throwable =>
        LOG.error( parserFailMsg, ex, JSON.stringify(dev) )
        None
    }
  }

}


/** Интерфейс для компаньонов, собирающих конкретные реализации [[BeaconParser]]. */
trait BeaconParserFactory extends IApply1 {
  override type ApplyArg_t = DeviceInfo
  override type T <: BeaconParser
}
