package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.lk.nodes.{MLknNode, MLknNodeResp}
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.2020 21:37
  * Description: Модель кэша ответов сервера для сканера маячков.
  */
object MBeaconScan {

  @inline implicit def univEq: UnivEq[MBeaconScan] = UnivEq.derive

  def cacheMap = GenLens[MBeaconScan](_.cacheMap)
  def scanReq = GenLens[MBeaconScan](_.scanReq)

  def empty = apply()

}

/** Контейнер состояния скана маячков.
  *
  * @param cacheMap Карта текущего кэша.
  * @param scanReq Реквест на сервер с запросом по маячкам.
  */
case class MBeaconScan(
                        cacheMap      : HashMap[String, MBeaconCachedEntry]     = HashMap.empty,
                        scanReq       : Pot[MLknNodeResp]                       = Pot.empty,
                      )


object MBeaconCachedEntry {
  @inline implicit def univEq: UnivEq[MBeaconCachedEntry] = UnivEq.derive

  def nodeScanResp = GenLens[MBeaconCachedEntry](_.nodeScanResp)
  def fetchedAtMs = GenLens[MBeaconCachedEntry](_.fetchedAtMs)

  implicit class CacheMapOpsExt( private val cacheMap: collection.Map[String, MBeaconCachedEntry] ) extends AnyVal {
    def respForUid(uid: String): Option[MLknNode] = {
      cacheMap
        .get( uid )
        .flatMap(_.nodeScanResp)
    }
  }

}
/** Элемент кэша узла с соотв. метаданными
  *
  * @param nodeScanResp Ответ сервера по данному маячку.
  *                     None означает, что маячка не обнаружено на сервере.
  * @param fetchedAtMs Когда произошёл запрос к серверу.
  */
case class MBeaconCachedEntry(
                               nodeScanResp     : Option[MLknNode] = None,
                               fetchedAtMs      : Long        = System.currentTimeMillis(),
                             )