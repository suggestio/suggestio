package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.lk.nodes.MLknNodeResp
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.2020 21:37
  * Description: Модель кэша ответов сервера для сканера маячков.
  */
object MBeaconScan {

  @inline implicit def univEq: UnivEq[MBeaconScan] = UnivEq.derive

  def scanReq = GenLens[MBeaconScan](_.scanReq)
  def updateTimeout = GenLens[MBeaconScan](_.updateTimeout)

  def empty = apply()

}

/** Контейнер состояния скана маячков.
  *
  * @param scanReq Реквест на сервер с запросом по маячкам.
  * @param updateTimeout Таймер обновления рендера данных на экране, чтобы подавлять лавину параллельных рендеров
  *                      из-за кучи одновременных сигналов.
  *                      Может никогда не иметь Pot.ready, а балансировать между empty и pending - этого достаточно.
  */
case class MBeaconScan(
                        scanReq                 : Pot[MLknNodeResp]                   = Pot.empty,
                        updateTimeout           : Pot[Int]                            = Pot.empty,
                      )
