package io.suggest.sc.sc3

import io.suggest.dev.MScreen
import io.suggest.sc.MScApiVsn
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MLocEnv
import io.suggest.sc.ads.{MAdsSearchReq, MLookupMode, MScFocusArgs}
import io.suggest.sc.index.MScIndexArgs


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 14:25
  * Description: Серверная поддержка моделей [[MScQs]], [[MScCommonQs]].
  */
object MScQsJvm {

  /** QSB-поддержка для [[MScCommonQs]]. */
  implicit def mScCommonQsQsb(implicit
                              screenOptB   : QueryStringBindable[Option[MScreen]],
                              apiVsnB      : QueryStringBindable[MScApiVsn],
                              locEnvB      : QueryStringBindable[MLocEnv],
                              boolOptB     : QueryStringBindable[Option[Boolean]]
                             ): QueryStringBindable[MScCommonQs] = {
    new QueryStringBindableImpl[MScCommonQs] {
      import MScCommonQs.Fields._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScCommonQs]] = {
        val k = key1F(key)
        for {
          screenOptE            <- screenOptB.bind  ( k(SCREEN_FN),             params )
          apiVsnE               <- apiVsnB.bind     ( k(API_VSN_FN),            params )
          locEnvE               <- locEnvB.bind     ( k(LOC_ENV_FN),            params )
          searchGridAdsOptE     <- boolOptB.bind    ( k(SEARCH_GRID_ADS_FN),    params )
          searchTagsOptE        <- boolOptB.bind    ( k(SEARCH_TAGS_FN),        params )
        } yield {
          for {
            screenOpt           <- screenOptE.right
            apiVsn              <- apiVsnE.right
            locEnv              <- locEnvE.right
            searchGridAdsOpt    <- searchGridAdsOptE.right
            searchTagsOpt       <- searchTagsOptE.right
          } yield {
            MScCommonQs(
              screen          = screenOpt,
              apiVsn          = apiVsn,
              locEnv          = locEnv,
              searchGridAds   = searchGridAdsOpt,
              searchNodes      = searchTagsOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MScCommonQs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          screenOptB.unbind   ( k(SCREEN_FN),             value.screen ),
          apiVsnB.unbind      ( k(API_VSN_FN),            value.apiVsn ),
          locEnvB.unbind      ( k(LOC_ENV_FN),            value.locEnv ),
          boolOptB.unbind     ( k(SEARCH_GRID_ADS_FN),    value.searchGridAds ),
          boolOptB.unbind     ( k(SEARCH_TAGS_FN),        value.searchNodes ),
          // TODO SearchTab bind/unbind
        )
      }

    }
  }


  /** Поддержка интеграции с URL query string через play router. */
  implicit def mScAdsSearchQsQsb(implicit
                                 esIdOptB       : QueryStringBindable[Option[MEsUuId]],
                                 longOptB       : QueryStringBindable[Option[Long]],
                                 intOptB        : QueryStringBindable[Option[Int]],
                                 locEnvB        : QueryStringBindable[MLocEnv],
                                 strOptB        : QueryStringBindable[Option[String]],
                                ): QueryStringBindable[MAdsSearchReq] = {
    new QueryStringBindableImpl[MAdsSearchReq] {
      import io.suggest.ad.search.AdSearchConstants._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MAdsSearchReq]] = {
        val k = key1F(key)
        for {
          prodIdOptE        <- esIdOptB.bind  (k(PRODUCER_ID_FN),     params)
          rcvrIdOptE        <- esIdOptB.bind  (k(RECEIVER_ID_FN),     params)
          genOptE           <- longOptB.bind  (k(GENERATION_FN),      params)
          limitOptE         <- intOptB.bind   (k(LIMIT_FN),           params)
          offsetOptE        <- intOptB.bind   (k(OFFSET_FN),          params)
          tagNodeIdOptE     <- esIdOptB.bind  (k(TAG_NODE_ID_FN),     params)
          textQueryOptE     <- strOptB.bind   (k(TEXT_QUERY_FN),      params)
        } yield {
          for {
            prodIdOpt       <- prodIdOptE.right
            rcvrIdOpt       <- rcvrIdOptE.right
            genOpt          <- genOptE.right
            limitOpt        <- limitOptE.right
            offsetOpt       <- offsetOptE.right
            tagNodeIdOpt    <- tagNodeIdOptE.right
            textQueryOpt    <- textQueryOptE.right
          } yield {
            MAdsSearchReq(
              prodId        = prodIdOpt,
              rcvrId        = rcvrIdOpt,
              genOpt        = genOpt,
              limit         = limitOpt,
              offset        = offsetOpt,
              tagNodeId     = tagNodeIdOpt,
              textQuery     = textQueryOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: MAdsSearchReq): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          esIdOptB.unbind   (k(PRODUCER_ID_FN),     value.prodId),
          esIdOptB.unbind   (k(RECEIVER_ID_FN),     value.rcvrId),
          longOptB.unbind   (k(GENERATION_FN),      value.genOpt),
          intOptB.unbind    (k(LIMIT_FN),           value.limit),
          intOptB.unbind    (k(OFFSET_FN),          value.offset),
          esIdOptB.unbind   (k(TAG_NODE_ID_FN),     value.tagNodeId),
          strOptB.unbind    (k(TEXT_QUERY_FN),      value.textQuery)
        )
      }
    }
  }


  /** Поддержка QSB для [[MScQs]]. */
  implicit def mScQsQsb(implicit
                        scUapiCommonQsB    : QueryStringBindable[MScCommonQs],
                        adsSearchReqB      : QueryStringBindable[MAdsSearchReq],
                        scIndexArgsOptB    : QueryStringBindable[Option[MScIndexArgs]],
                        scFocusArgsOptB    : QueryStringBindable[Option[MScFocusArgs]]
                       ): QueryStringBindable[MScQs] = {
    new QueryStringBindableImpl[MScQs] {
      import MScQs.Fields._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScQs]] = {
        val k = key1F(key)
        for {
          commonQsE           <- scUapiCommonQsB.bind ( k(COMMON_FN),         params )
          adsSearchReqE       <- adsSearchReqB.bind   ( k(ADS_SEARCH_FN),     params )
          scIndexArgsOptE     <- scIndexArgsOptB.bind ( k(INDEX_FN),          params )
          scFocusArgsOptE     <- scFocusArgsOptB.bind ( k(FOCUSED_ARGS_FN),   params )
        } yield {
          for {
            commonQs          <- commonQsE.right
            adsSearchReq      <- adsSearchReqE.right
            scIndexArgsOpt    <- scIndexArgsOptE.right
            scFocusArgsOpt    <- scFocusArgsOptE.right
          } yield {
            MScQs(
              common  = commonQs,
              search  = adsSearchReq,
              index   = scIndexArgsOpt,
              foc     = scFocusArgsOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MScQs): String = {
        val k = key1F( key )
        _mergeUnbinded1(
          scUapiCommonQsB.unbind  ( k(COMMON_FN),         value.common ),
          adsSearchReqB.unbind    ( k(ADS_SEARCH_FN),     value.search ),
          scIndexArgsOptB.unbind  ( k(INDEX_FN),          value.index ),
          scFocusArgsOptB.unbind  ( k(FOCUSED_ARGS_FN),   value.foc )
        )
      }

    }

  }


  /** Поддержка QSB для MScFocusArgs. */
  implicit def mScFocusArgsQsb(implicit
                               boolB            : QueryStringBindable[Boolean],
                               lookupModeOptB   : QueryStringBindable[Option[MLookupMode]],
                               strB             : QueryStringBindable[String]
                              ): QueryStringBindable[MScFocusArgs] = {
    new QueryStringBindableImpl[MScFocusArgs] {
      import io.suggest.ad.search.AdSearchConstants._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScFocusArgs]] = {
        val k = key1F(key)
        for {
          focJumpAllowedE         <- boolB.bind         ( k(FOC_INDEX_ALLOWED_FN),    params )
          lookupModeOptE          <- lookupModeOptB.bind( k(AD_LOOKUP_MODE_FN),       params )
          lookupAdIdE             <- strB.bind          ( k(LOOKUP_AD_ID_FN),         params )
        } yield {
          for {
            focIndexAllowed       <- focJumpAllowedE.right
            lookupModeOpt         <- lookupModeOptE.right
            lookupAdId            <- lookupAdIdE.right
          } yield {
            MScFocusArgs(
              focIndexAllowed     = focIndexAllowed,
              lookupMode          = lookupModeOpt,
              lookupAdId          = lookupAdId
            )
          }
        }
      }

      override def unbind(key: String, value: MScFocusArgs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          boolB.unbind          ( k(FOC_INDEX_ALLOWED_FN),    value.focIndexAllowed ),
          lookupModeOptB.unbind ( k(AD_LOOKUP_MODE_FN),       value.lookupMode ),
          strB.unbind           ( k(LOOKUP_AD_ID_FN),         value.lookupAdId )
        )
      }

    }
  }

}
