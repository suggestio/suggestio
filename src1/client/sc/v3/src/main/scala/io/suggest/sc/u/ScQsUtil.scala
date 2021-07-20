package io.suggest.sc.u

import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MPxRatios, MScreen}
import io.suggest.sc.ads.{MAdsSearchReq, MIndexAdOpenQs, MScFocusArgs, MScGridArgs, MScNodesArgs}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.sc3.{MScCommonQs, MScQs}
import io.suggest.es.model.MEsUuId.Implicits._
import io.suggest.geo.MLocEnv
import scalaz.NonEmptyList

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.2020 15:48
  * Description: Утиль для управлением MScQs, оперирующие пачками моделей.
  */
object ScQsUtil {

  /** Кол-во карточек за один ответ. */
  // TODO Рассчитывать кол-во карточек за 1 реквест на основе экрана и прочих вещей.
  def adsPerReqLimit = 10


  /** Сборка LocEnv на основе описанных данных. */
  private def getLocEnv(mroot: MScRoot, withGeoLoc: Boolean, withRadioBeacons: Boolean = true): MLocEnv = {
    MLocEnv(
      geoLocOpt  = OptionUtil.maybeOpt(withGeoLoc)( mroot.geoLocOpt ),
      // При переходе в под-узел, зачем отображать маячки с предыдущей страницы? Незачем.
      beacons = if (withRadioBeacons) mroot.locEnvRadioBeacons else Nil,
    )
  }


  /** Сборка аргументов для геопоиска на основе состояния. */
  def geoSearchQs(mroot: MScRoot): MScQs = {
    MScQs(
      common = MScCommonQs(
        locEnv      = getLocEnv(
          mroot = mroot,
          withGeoLoc = true,
          // For search tags inside ble/wifi beacons, beacons-flag defined explicitly:
          withRadioBeacons = true,
        ),
        apiVsn      = mroot.internals.conf.apiVsn,
        screen      = Some( mroot.dev.screen.info.screen ),
      ),
      search = MAdsSearchReq(
        textQuery = mroot.index.search.text.searchQuery.toOption,
        rcvrId    = mroot.index.state.rcvrId.toEsUuIdOpt,
      ),
      nodes = Some( MScNodesArgs(
        _searchNodes = false,
      )),
    )
  }


  /** Данные экрана для рендера карточек плитки. */
  private def screenForGridAds(mroot: MScRoot): MScreen = {
    val scr0 = mroot.dev.screen.info.safeScreen
    // 2018-01-24 Костыль в связи с расхождением между szMult экрана и szMult плитки, тут быстрофикс:
    val pxRatio2 = MPxRatios.forRatio(
      Math.max(
        mroot.grid.core.jdConf.szMult.toDouble,
        scr0.pxRatio.pixelRatio
      )
    )
    if (pxRatio2.value > scr0.pxRatio.value)
      (MScreen.pxRatio set pxRatio2)(scr0)
    else
      scr0
  }


  /** Сборка аргументов для поиска карточек плитки из состояния выдачи. */
  def gridAdsQs(mroot: MScRoot, offset: Int): MScQs = {
    val inxState = mroot.index.state

    // TODO Унести сборку этого qs в контроллер или в утиль? Тут используется currRcvrId:
    // nodeId ресивера может быть задан в switchCtx, который известен только в контроллере, и отличаться от значения currRcvrId.
    val currRcvrId = inxState.rcvrId.toEsUuIdOpt

    MScQs(
      common = MScCommonQs(
        apiVsn = mroot.internals.conf.apiVsn,
        screen = Some( screenForGridAds(mroot) ),
        locEnv = getLocEnv(
          mroot,
          withGeoLoc = currRcvrId.isEmpty,
          withRadioBeacons = inxState.isBleGridAds,
        ),
      ),
      search = MAdsSearchReq(
        rcvrId      = currRcvrId,
        genOpt      = Some( inxState.generation ),
        tagNodeId   = mroot.index.search.geo.data.selTagIds
          .headOption      // TODO Научить сервер поддерживать несколько тегов одновременно.
          .toEsUuIdOpt,
        limit = Some( adsPerReqLimit ),
        offset = Some( offset ),
      ),
      grid = Some( MScGridArgs(
        focAfterJump = OptionUtil.SomeBool.someFalse,
      ))
    )
  }


  /** qs для фокусировки на карточке. */
  def focAdsQs(mroot: MScRoot, adIds: NonEmptyList[String]): MScQs = {
    MScQs(
      common = MScCommonQs(
        apiVsn = mroot.internals.conf.apiVsn,
        screen = Some( screenForGridAds(mroot) ),
        locEnv = getLocEnv(
          mroot,
          withGeoLoc = false,
          withRadioBeacons = mroot.index.state.isBleGridAds,
        ),
      ),
      search = MAdsSearchReq(
        rcvrId = mroot.index.state.rcvrId.toEsUuIdOpt,
      ),
      foc = Some(
        MScFocusArgs(
          indexAdOpen      = Some(
            MIndexAdOpenQs(
              withBleBeaconAds = false,
            )
          ),
          adIds            = adIds,
        )
      ),
    )
  }


  /** qs для запроса карточек только по текущим наблюдаемым ble-маячкам.
    *
    * @param mroot Состояние корневой модели.
    * @param allow404 Разрешить серверу возвращать 404-карточки?
    * @return Инстанс MScQs для Sc API.
    */
  def gridAdsOnlyBleBeaconed(mroot: MScRoot, allow404: Boolean): MScQs = {
    MScQs(
      common = MScCommonQs(
        apiVsn = mroot.internals.conf.apiVsn,
        screen = Some( screenForGridAds(mroot) ),
        locEnv = getLocEnv(mroot, withGeoLoc = false, withRadioBeacons = true),
      ),
      search = MAdsSearchReq(
        genOpt = Some( mroot.index.state.generation ),
        offset = Some(0),
        limit  = Some( adsPerReqLimit ),
      ),
      grid = Some( MScGridArgs(
        focAfterJump = OptionUtil.SomeBool.someFalse,
        // Надо возвращать названия карточек с сервера, чтобы их можно было отрендерить текстом в системной нотификации.
        withTitle = true,
        allow404 = allow404,
      )),
    )
  }


}
