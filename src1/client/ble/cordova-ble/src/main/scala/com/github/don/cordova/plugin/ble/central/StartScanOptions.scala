package com.github.don.cordova.plugin.ble.central

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.18 16:06
  * Description: JSON options for startScanWithOptions().
  */
trait StartScanOptions extends js.Object {

  val reportDuplicates: js.UndefOr[Boolean] = js.undefined

  // Android:
  val scanMode: js.UndefOr[StartScanOptions.ScanMode] = js.undefined
  val callbackType: js.UndefOr[StartScanOptions.CallbackType] = js.undefined
  val matchMode: js.UndefOr[StartScanOptions.MatchMode] = js.undefined
  val numOfMatches: js.UndefOr[StartScanOptions.NumOfMatches] = js.undefined
  val phy: js.UndefOr[StartScanOptions.Phy] = js.undefined
  val legacy: js.UndefOr[Boolean] = js.undefined
  val reportDelay: js.UndefOr[Long] = js.undefined

}


object StartScanOptions {

  type ScanMode <: String
  object ScanMode {
    final def LOW_POWER = "lowPower".asInstanceOf[ScanMode]
    final def BALANCED = "balanced".asInstanceOf[ScanMode]
    final def LOW_LATENCY = "lowLatency".asInstanceOf[ScanMode]
    final def OPPORTUNISTIC = "opportunistic".asInstanceOf[ScanMode]
  }


  type CallbackType <: String
  object CallbackType {
    final def ALL = "all".asInstanceOf[CallbackType]
    final def FIRST = "first".asInstanceOf[CallbackType]
    final def LOST = "lost".asInstanceOf[CallbackType]
  }


  type MatchMode <: String
  object MatchMode {
    final def AGGRESSIVE = "aggressive".asInstanceOf[MatchMode]
    final def STICKY = "sticky".asInstanceOf[MatchMode]
  }


  type NumOfMatches <: String
  object NumOfMatches {
    final def ONE = "one".asInstanceOf[NumOfMatches]
    final def FEW = "few".asInstanceOf[NumOfMatches]
    final def MAX = "max".asInstanceOf[NumOfMatches]
  }


  type Phy <: String
  object Phy {
    final def `1M` = "1m".asInstanceOf[Phy]
    final def CODED = "coded".asInstanceOf[Phy]
    final def ALL = "all".asInstanceOf[Phy]
  }

}
