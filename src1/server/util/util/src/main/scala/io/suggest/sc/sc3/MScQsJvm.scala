package io.suggest.sc.sc3

import io.suggest.dev.MScreen
import io.suggest.sc.MScApiVsn
import play.api.mvc.QueryStringBindable
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MLocEnv
import io.suggest.sc.ads.{MAdsSearchReq, MLookupMode, MScFocusArgs, MScGridArgs, MScNodesArgs}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.xplay.qsb.QueryStringBindableImpl
import io.suggest.common.empty.OptionUtil.BoolOptOps


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
        } yield {
          for {
            screenOpt           <- screenOptE
            apiVsn              <- apiVsnE
            locEnv              <- locEnvE
          } yield {
            MScCommonQs(
              screen          = screenOpt,
              apiVsn          = apiVsn,
              locEnv          = locEnv,
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
          // TODO SearchTab bind/unbind
        )
      }

    }
  }


  implicit def scNodesArgsQsb(implicit
                              boolB: QueryStringBindable[Boolean],
                             ): QueryStringBindable[MScNodesArgs] = {
    new QueryStringBindableImpl[MScNodesArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScNodesArgs]] = {
        val F = MScNodesArgs.Fields
        val k = key1F( key )
        for {
          // Надо помнить, что должен быть хотя бы один не-Option параметр. Иначе тут всегда будет всё хорошо при None во всех биндерах.
          searchNodesE <- boolB.bind( k(F.SEARCH_NODES_FN), params )
        } yield {
          for {
            searchNodes <- searchNodesE
          } yield {
            MScNodesArgs(
              _searchNodes = searchNodes,
            )
          }
        }
      }

      override def unbind(key: String, value: MScNodesArgs): String = {
        val F = MScNodesArgs.Fields
        val k = key1F( key )
        _mergeUnbinded1(
          boolB.unbind( k(F.SEARCH_NODES_FN), value._searchNodes ),
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
            prodIdOpt       <- prodIdOptE
            rcvrIdOpt       <- rcvrIdOptE
            genOpt          <- genOptE
            limitOpt        <- limitOptE
            offsetOpt       <- offsetOptE
            tagNodeIdOpt    <- tagNodeIdOptE
            textQueryOpt    <- textQueryOptE
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


  implicit def mScGridArgsQsb(implicit
                              boolB       : QueryStringBindable[Boolean],
                              boolOptB    : QueryStringBindable[Option[Boolean]],
                             ): QueryStringBindable[MScGridArgs] = {
    new QueryStringBindableImpl[MScGridArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScGridArgs]] = {
        val k = key1F(key)
        val F = MScGridArgs.Fields
        for {
          adTitlesE           <- boolB.bind( k(F.WITH_TITLE), params )
          focAfterJumpOptE    <- boolOptB.bind( k(F.FOC_AFTER_JUMP), params )
          allow404OptE        <- boolOptB.bind( k(F.ALLOW_404), params )
        } yield {
          for {
            adTitles          <- adTitlesE
            focAfterJumpOpt   <- focAfterJumpOptE
            allow404Opt       <- allow404OptE
          } yield {
            MScGridArgs(
              withTitle        = adTitles,
              focAfterJump    = focAfterJumpOpt,
              allow404        = allow404Opt.getOrElseTrue,
            )
          }
        }
      }

      override def unbind(key: String, value: MScGridArgs): String = {
        val k = key1F(key)
        val F = MScGridArgs.Fields
        _mergeUnbinded1(
          boolB.unbind( k(F.WITH_TITLE), value.withTitle ),
          boolOptB.unbind( k(F.FOC_AFTER_JUMP), value.focAfterJump ),
          boolOptB.unbind( k(F.ALLOW_404), if (value.allow404) None else Some(value.allow404) ),
        )
      }
    }
  }


  /** Поддержка QSB для [[MScQs]]. */
  implicit def mScQsQsb(implicit
                        scUapiCommonQsB    : QueryStringBindable[MScCommonQs],
                        adsSearchReqB      : QueryStringBindable[MAdsSearchReq],
                        scIndexArgsOptB    : QueryStringBindable[Option[MScIndexArgs]],
                        scFocusArgsOptB    : QueryStringBindable[Option[MScFocusArgs]],
                        scGridArgsOptB     : QueryStringBindable[Option[MScGridArgs]],
                        scNodesArgsOptB    : QueryStringBindable[Option[MScNodesArgs]],
                       ): QueryStringBindable[MScQs] = {
    new QueryStringBindableImpl[MScQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScQs]] = {
        val F = MScQs.Fields
        val k = key1F(key)
        for {
          commonQsE           <- scUapiCommonQsB.bind ( k(F.COMMON_FN),         params )
          adsSearchReqE       <- adsSearchReqB.bind   ( k(F.ADS_SEARCH_FN),     params )
          scIndexArgsOptE     <- scIndexArgsOptB.bind ( k(F.INDEX_FN),          params )
          scFocusArgsOptE     <- scFocusArgsOptB.bind ( k(F.FOCUSED_ARGS_FN),   params )
          scGridArgsOptE      <- scGridArgsOptB.bind  ( k(F.GRID_FN),           params )
          scNodesArgsOptE     <- scNodesArgsOptB.bind ( k(F.NODES_FN),          params )
        } yield {
          for {
            commonQs          <- commonQsE
            adsSearchReq      <- adsSearchReqE
            scIndexArgsOpt    <- scIndexArgsOptE
            scFocusArgsOpt    <- scFocusArgsOptE
            scGridArgsOpt     <- scGridArgsOptE
            scNodesArgsOpt    <- scNodesArgsOptE
          } yield {
            MScQs(
              common  = commonQs,
              search  = adsSearchReq,
              index   = scIndexArgsOpt,
              foc     = scFocusArgsOpt,
              grid    = scGridArgsOpt,
              nodes   = scNodesArgsOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: MScQs): String = {
        val F = MScQs.Fields
        val k = key1F( key )
        _mergeUnbinded1(
          scUapiCommonQsB.unbind  ( k(F.COMMON_FN),         value.common ),
          adsSearchReqB.unbind    ( k(F.ADS_SEARCH_FN),     value.search ),
          scIndexArgsOptB.unbind  ( k(F.INDEX_FN),          value.index ),
          scFocusArgsOptB.unbind  ( k(F.FOCUSED_ARGS_FN),   value.foc ),
          scGridArgsOptB.unbind   ( k(F.GRID_FN),           value.grid ),
          scNodesArgsOptB.unbind  ( k(F.NODES_FN),          value.nodes ),
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
            focIndexAllowed       <- focJumpAllowedE
            lookupModeOpt         <- lookupModeOptE
            lookupAdId            <- lookupAdIdE
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
