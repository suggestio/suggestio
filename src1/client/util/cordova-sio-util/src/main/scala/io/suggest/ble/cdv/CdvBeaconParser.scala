package io.suggest.ble.cdv

import cordova.plugins.ble.central.BtDevice
import io.suggest.log.Log
import io.suggest.msg.ErrorMsg_t
import io.suggest.primo.{IApply1, TypeT}
import io.suggest.radio.MRadioSignal

import scala.scalajs.js.JSON
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 11:20
  * Description: BLE Beacon Parsing interface.
  */
trait CdvBeaconParser extends TypeT with Log {

  /** Beacon parsing result type. */
  type ParseRes_t = Option[Either[Any, MRadioSignal]]


  /** Incoming cordova-ble device info. */
  def dev: BtDevice

  /** To parse dev into optional beacon instance.
    * @return None - not a beacon dev.
    *         Some() - found beacon, parsed as T.
    * @throws Exception if something gone wrong. Invalid beacon or some internal program error.
    */
  def parse(): ParseRes_t

  /** If parse() throwed, this message will be used as error message. */
  def parserFailMsg: ErrorMsg_t

  /** Safe wrapper around parse(), to log exceptions, return None on errors. */
  def tryParse(): ParseRes_t = {
    Try( parse() )
      .fold[ParseRes_t](
        {ex =>
          logger.error( parserFailMsg, ex, JSON.stringify(dev) )
          None
        },
        identity
      )
  }

}


/** Interface for companion objects, produces beacon parsers [[CdvBeaconParser]]-implementations. */
trait BeaconParserFactory extends IApply1 {
  override type ApplyArg_t = BtDevice
  override type T <: CdvBeaconParser
}
