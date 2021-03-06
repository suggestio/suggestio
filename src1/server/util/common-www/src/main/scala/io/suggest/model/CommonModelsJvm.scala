package io.suggest.model

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.n2.media.storage.{MStorage, MStorageInfo, MStorageInfoData, MStorages}
import _root_.play.api.mvc.{PathBindable, QueryStringBindable}
import io.suggest.common.empty.OptionUtil
import OptionUtil.BoolOptOps
import io.suggest.bill.cart.{MCartSubmitQs, MPayableVia}
import io.suggest.ble.MUidBeacon
import io.suggest.crypto.hash.{HashesHex, MHash, MHashes}
import io.suggest.dev.{MOsFamilies, MOsFamily, MScreen}
import io.suggest.es.model.MEsUuId
import io.suggest.geo.{GeoPoint, MGeoLoc, MGeoLocSource, MGeoPoint, MLocEnv}
import io.suggest.id.login.{MLoginTab, MLoginTabs}
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknModifyQs, MLknOpKey, MLknOpKeys, MLknOpValue}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import io.suggest.n2.edge.edit.MNodeEdgeIdQs
import io.suggest.n2.media.{MFileMeta, MFileMetaHash, MFileMetaHashFlag, MFileMetaHashFlags}
import io.suggest.n2.node.{MNodeIdType, MNodeType}
import io.suggest.pay.{MPaySystem, MPaySystems}
import io.suggest.sc.ads.{MAdsSearchReq, MIndexAdOpenQs, MScFocusArgs, MScGridArgs, MScNodesArgs}
import io.suggest.sc.app.{MScAppGetQs, MScAppManifestQs}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.sc3.{MScCommonQs, MScQs}
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.spa.SioPages
import io.suggest.swfs.fid.Fid
import io.suggest.up.{MUploadChunkQs, MUploadChunkSize, MUploadChunkSizes}
import io.suggest.url.bind.QsbSeqUtil
import io.suggest.util.logs.MacroLogsDyn
import io.suggest.xplay.qsb.{AbstractQueryStringBindable, CrossQsBindable, QsbSeq}
import io.suggest.xplay.qsb.CrossQsBindable._
import io.suggest.url.bind.QueryStringBindableUtil._

import scala.util.Try
import japgolly.univeq._
import scalaz.{ICons, IList, NonEmptyList}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2019 8:25
  * Description: Контейнер jvm-only данных для разных пошаренных common-моделей.
  */
object CommonModelsJvm extends MacroLogsDyn {


  /** Биндинги для url query string. */
  implicit def mScApiVsnQsb: CrossQsBindable[MScApiVsn] =
    EnumeratumJvmUtil.valueEnumQsb( MScApiVsns )

  /** QSB для инстансов [[MStorage]]. */
  implicit def mStorageQsb: CrossQsBindable[MStorage] =
    EnumeratumJvmUtil.valueEnumQsb( MStorages )

  implicit def uploadChunkSizeQsb: CrossQsBindable[MUploadChunkSize] =
    EnumeratumJvmUtil.valueEnumQsb( MUploadChunkSizes )

  implicit def paySystemPb: PathBindable[MPaySystem] =
    EnumeratumJvmUtil.valueEnumPb( MPaySystems )

  /** Поддержка сырого биндинга из query-string. */
  implicit def fidQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[Fid] = {
    new AbstractQueryStringBindable[Fid] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Fid]] = {
        val fidStrRaw = strB.bind(key, params)
        for (fidStrE <- fidStrRaw) yield {
          for {
            fidStr <- fidStrE
            parsed <- Try( Fid(fidStr) )
              .toEither
              .left.map { ex =>
                LOGGER.error(s"qsb: failed to bind $fidStrRaw", ex)
                ex.getMessage
              }
          } yield {
            parsed
          }
        }
      }

      override def unbind(key: String, value: Fid): String = {
        strB.unbind(key, value.toString)
      }
    }
  }


  implicit def storageInfoDataQsb(implicit
                                  strB         : QueryStringBindable[String],
                                  strSeqB      : QueryStringBindable[QsbSeq[String]],
                                 ): QueryStringBindable[MStorageInfoData] = {
    new AbstractQueryStringBindable[MStorageInfoData] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MStorageInfoData]] = {
        val F = MStorageInfoData.Fields
        val k = key1F(key)
        for {
          dataE       <- strB.bind( k(F.META_FN), params )
          hostsE      <- strSeqB.bind( k(F.SHARDS_FN), params )
        } yield {
          for {
            data      <- dataE
            hosts     <- hostsE
          } yield {
            MStorageInfoData(
              meta  = data,
              shards = hosts.items.toSet,
            )
          }
        }
      }

      override def unbind(key: String, value: MStorageInfoData): String = {
        val F = MStorageInfoData.Fields
        val k = key1F(key)
        _mergeUnbinded1(
          strB.unbind( k(F.META_FN), value.meta ),
          strSeqB.unbind( k(F.SHARDS_FN), QsbSeq( value.shards.toSeq ) ),
        )
      }

    }
  }


  implicit def storageInfoQsb(implicit
                              storageB   : QueryStringBindable[MStorage],
                              infoDataB  : QueryStringBindable[MStorageInfoData],
                             ): QueryStringBindable[MStorageInfo] = {
    new AbstractQueryStringBindable[MStorageInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MStorageInfo]] = {
        val F = MStorageInfo.Fields
        val k = key1F(key)
        for {
          storageE <- storageB.bind( k(F.STORAGE_FN), params )
          infoE    <- infoDataB.bind( k(F.DATA_FN), params )
        } yield {
          for {
            storage <- storageE
            info    <- infoE
          } yield {
            MStorageInfo(
              storage = storage,
              data    = info,
            )
          }
        }
      }

      override def unbind(key: String, value: MStorageInfo): String = {
        val F = MStorageInfo.Fields
        val k = key1F(key)
        _mergeUnbinded1(
          storageB.unbind( k(F.STORAGE_FN), value.storage ),
          infoDataB.unbind( k(F.DATA_FN), value.data ),
        )
      }
    }
  }


  /** Поддержка MNodeEdgeIdQs в play router. */
  implicit def mNodeEdgeIdQsQsb(implicit
                                strB      : QueryStringBindable[String],
                                longB     : QueryStringBindable[Long],
                                intOptB   : QueryStringBindable[Option[Int]],
                               ): QueryStringBindable[MNodeEdgeIdQs] = {
    new AbstractQueryStringBindable[MNodeEdgeIdQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MNodeEdgeIdQs]] = {
        val k = key1F(key)
        val F = MNodeEdgeIdQs.Fields
        for {
          nodeIdE     <- strB.bind      (k(F.NODE_ID_FN),   params)
          nodeVsnE    <- longB.bind     (k(F.NODE_VSN_FN),  params)
          edgeIdE     <- intOptB.bind   (k(F.EDGE_ID_FN),   params)
        } yield {
          for {
            nodeId    <- nodeIdE
            nodeVsn   <- nodeVsnE
            edgeId    <- edgeIdE
          } yield {
            MNodeEdgeIdQs(
              nodeId  = nodeId,
              nodeVsn = nodeVsn,
              edgeId  = edgeId,
            )
          }
        }
      }

      override def unbind(key: String, value: MNodeEdgeIdQs): String = {
        val k = key1F(key)
        val F = MNodeEdgeIdQs.Fields
        _mergeUnbinded1(
          strB.unbind( k(F.NODE_ID_FN),   value.nodeId ),
          longB.unbind( k(F.NODE_VSN_FN),  value.nodeVsn ),
          intOptB.unbind( k(F.EDGE_ID_FN),   value.edgeId ),
        )
      }

    }
  }


  implicit def osPlatformQsb: CrossQsBindable[MOsFamily] =
    EnumeratumJvmUtil.valueEnumQsb( MOsFamilies )

  implicit def cartPayableViaQsb: CrossQsBindable[MPayableVia] = {
    MPayableVia.payableViaQsB(
      boolB = implicitly[QueryStringBindable[Boolean]],
    )
  }

  implicit def cartSubmitArgsQsb: CrossQsBindable[MCartSubmitQs] = {
    MCartSubmitQs.cartSubmitArgsQsB(
      stringOptB = implicitly[QueryStringBindable[Option[String]]],
      payViaB = cartPayableViaQsb,
    )
  }

  /** Поддержка MScAppDlQs. */
  implicit def scAppDlQs(implicit
                         predicateOptB: QueryStringBindable[Option[MPredicate]],
                        ): QueryStringBindable[MScAppGetQs] = {
    new AbstractQueryStringBindable[MScAppGetQs] {
      private def osPlatformB = implicitly[QueryStringBindable[MOsFamily]]
      private def boolB = implicitly[QueryStringBindable[Boolean]]
      private def strOptB = implicitly[QueryStringBindable[Option[String]]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScAppGetQs]] = {
        val F = MScAppGetQs.Fields
        val k = key1F( key )
        for {
          osPlatformE     <- osPlatformB.bind( k(F.OS_FAMILY), params )
          rdrE            <- boolB.bind( k(F.RDR), params )
          nodeIdOptE      <- strOptB.bind( k(F.ON_NODE_ID), params )
          predicateOptE   <- predicateOptB.bind( k(F.PREDICATE), params )
          if predicateOptE.exists(_.fold(true) { _ eqOrHasParent MPredicates.Application })
        } yield {
          for {
            osPlatform    <- osPlatformE
            rdr           <- rdrE
            nodeIdOpt     <- nodeIdOptE
            predicateOpt  <- predicateOptE
          } yield {
            MScAppGetQs(
              osFamily    = osPlatform,
              rdr         = rdr,
              onNodeId    = nodeIdOpt,
              predicate   = predicateOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: MScAppGetQs): String = {
        val F = MScAppGetQs.Fields
        val k = key1F( key )
        _mergeUnbinded1(
          osPlatformB.unbind( k(F.OS_FAMILY), value.osFamily ),
          boolB.unbind( k(F.RDR), value.rdr ),
          strOptB.unbind( k(F.ON_NODE_ID), value.onNodeId ),
          predicateOptB.unbind( k(F.PREDICATE), value.predicate ),
        )
      }
    }
  }


  implicit def mhashQsb: CrossQsBindable[MHash] =
    EnumeratumJvmUtil.valueEnumQsb( MHashes )


  /** Поддержка URL-qs вида "x.s1=aahh45234234&x.s256=aa543525325..."  */
  implicit def hashesHexQsb: QueryStringBindable[HashesHex] = {
   new AbstractQueryStringBindable[HashesHex] {

     private def strB = implicitly[QueryStringBindable[String]]

     override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HashesHex]] = {
       val hashesHexOptEithSeq = for (mhash <- MHashes.values) yield {
         for {
           hexValueEith <- strB.bind( key1(key, mhash.value), params )
         } yield {
           for (hexValue <- hexValueEith) yield {
             mhash -> hexValue
           }
         }
       }

       if (hashesHexOptEithSeq.isEmpty || hashesHexOptEithSeq.forall(_.isEmpty)) {
         None

       } else {
         val hashesHexEithSeq = hashesHexOptEithSeq
           .flatten
         val errorsIter = hashesHexEithSeq
           .iterator
           .flatMap(_.left.toOption)
         val eith = if (errorsIter.nonEmpty) {
           Left( errorsIter.mkString(",") )
         } else {
           val hhMap: HashesHex = hashesHexEithSeq
             .iterator
             .flatMap(_.toOption)
             .toMap
           Right(hhMap)
         }
         Some(eith)
       }
     }


     override def unbind(key: String, value: HashesHex): String = {
       _mergeUnbinded {
         for ((mhash, hexValue) <- value.iterator) yield {
           strB.unbind(key1(key, mhash.value), hexValue)
         }
       }
     }

   }
  }



  implicit def fileMetaHashFlagQsb: CrossQsBindable[MFileMetaHashFlag] =
    EnumeratumJvmUtil.valueEnumQsb( MFileMetaHashFlags )


  implicit def fileMetaHashQsb: QueryStringBindable[MFileMetaHash] = {
    @inline def hashB = implicitly[QueryStringBindable[MHash]]
    @inline def strB = implicitly[QueryStringBindable[String]]
    @inline def fmHashFlagsB = implicitly[QueryStringBindable[QsbSeq[MFileMetaHashFlag]]]

    new AbstractQueryStringBindable[MFileMetaHash] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MFileMetaHash]] = {
        val F = MFileMetaHash.Fields
        val k = key1F( key )
        for {
          hashE         <- hashB.bind( k(F.HASH_TYPE_FN), params )
          hexValueE     <- strB.bind( k(F.HEX_VALUE_FN), params )
          if hexValueE.exists { hashValue =>
            hashE.exists { mhash =>
              mhash.hexStrLen ==* hashValue.length
            }
          }
          flagsE        <- fmHashFlagsB.bind( k(F.FLAGS_FN), params )
          // Флаги - игнорим?
        } yield {
          for {
            hash        <- hashE
            hexValue    <- hexValueE
            flags       <- flagsE
          } yield {
            MFileMetaHash(
              hType     = hash,
              hexValue  = hexValue,
              flags     = flags.items.toSet,
            )
          }
        }
      }

      override def unbind(key: String, value: MFileMetaHash): String = {
        val F = MFileMetaHash.Fields
        val k = key1F( key )
        _mergeUnbinded1(
          hashB.unbind( k(F.HASH_TYPE_FN), value.hType ),
          strB.unbind( k(F.HEX_VALUE_FN), value.hexValue ),
          fmHashFlagsB.unbind( k(F.FLAGS_FN), QsbSeq(value.flags.toSeq) ),
        )
      }
    }
  }


  implicit def fileMetaQsb: QueryStringBindable[MFileMeta] = {
    @inline def strOptB = implicitly[QueryStringBindable[Option[String]]]
    @inline def longOptB = implicitly[QueryStringBindable[Option[Long]]]
    @inline def boolB = implicitly[QueryStringBindable[Boolean]]
    @inline def fmHashesB = implicitly[QueryStringBindable[QsbSeq[MFileMetaHash]]]

    new AbstractQueryStringBindable[MFileMeta] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MFileMeta]] = {
        val F = MFileMeta.Fields
        val k = key1F( key )
        for {
          contentTypeOptE   <- strOptB.bind( k(F.MIME_FN), params )
          if contentTypeOptE.exists( _.fold(true)(_.nonEmpty) )
          sizeBytesOptE     <- longOptB.bind( k(F.SIZE_B_FN), params )
          if sizeBytesOptE.exists( _.fold(true) { _ >= 0L } )
          isOriginalE       <- boolB.bind( k(F.IS_ORIGINAL_FN), params )
          hashesE           <- fmHashesB.bind( k(F.HASHES_HEX_FN), params )
        } yield {
          for {
            contentTypeOpt  <- contentTypeOptE
            sizeBytesOpt    <- sizeBytesOptE
            isOriginal      <- isOriginalE
            hashes          <- hashesE
          } yield {
            MFileMeta(
              mime        = contentTypeOpt,
              sizeB       = sizeBytesOpt,
              isOriginal  = isOriginal,
              hashesHex   = hashes.items,
            )
          }
        }
      }

      override def unbind(key: String, value: MFileMeta): String = {
        val F = MFileMeta.Fields
        val k = key1F( key )
        _mergeUnbinded1(
          strOptB.unbind( k(F.MIME_FN), value.mime ),
          longOptB.unbind( k(F.SIZE_B_FN), value.sizeB ),
          boolB.unbind( k(F.IS_ORIGINAL_FN), value.isOriginal ),
          fmHashesB.unbind( k(F.HASHES_HEX_FN), QsbSeq(value.hashesHex) ),
        )
      }

    }
  }


  implicit def scAppManifestQs: QueryStringBindable[MScAppManifestQs] = {
    @inline def strOptB = implicitly[QueryStringBindable[Option[String]]]
    @inline def hashesHexB = implicitly[QueryStringBindable[HashesHex]]

    new AbstractQueryStringBindable[MScAppManifestQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScAppManifestQs]] = {
        val k = key1F(key)
        val F = MScAppManifestQs.Fields
        for {
          onNodeIdOptE    <- strOptB.bind( k(F.ON_NODE_ID), params )
          hashesHexE      <- hashesHexB.bind( k(F.HASHES_HEX), params )
        } yield {
          for {
            onNodeIdOpt   <- onNodeIdOptE
            hashesHex     <- hashesHexE
          } yield {
            MScAppManifestQs(
              onNodeId = onNodeIdOpt,
              hashesHex = hashesHex,
            )
          }
        }
      }

      override def unbind(key: String, value: MScAppManifestQs): String =
        throw new UnsupportedOperationException( "scAppMainfestQs QSB.unbind() not implemented - useless" )
    }
  }


  implicit def uploadChunkQs: QueryStringBindable[MUploadChunkQs] = {
    val intOptB = implicitly[QueryStringBindable[Option[Int]]]
    @inline def chunkSizeOptB = implicitly[QueryStringBindable[Option[MUploadChunkSize]]]
    val longOptB = implicitly[QueryStringBindable[Option[Long]]]
    lazy val strOptB = implicitly[QueryStringBindable[Option[String]]]
    @inline def hashesHexB = implicitly[QueryStringBindable[HashesHex]]

    new AbstractQueryStringBindable[MUploadChunkQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MUploadChunkQs]] = {
        val F = MUploadChunkQs.Fields
        for {
          chunkNumberE          <- intOptB.bind( F.CHUNK_NUMBER, params )
          totalChunksE          <- intOptB.bind( F.TOTAL_CHUNKS, params )
          chunkSizeGeneralE     <- chunkSizeOptB.bind( F.CHUNK_SIZE_GEN, params )
          chunkSizeCurrentE     <- longOptB.bind( F.CHUNK_SIZE_CUR, params )
          totalSizeE            <- longOptB.bind( F.TOTAL_SIZE, params )
          identifierE           <- strOptB.bind( F.IDENTIFIER, params )
          fileNameE             <- strOptB.bind( F.FILENAME, params )
          relativePathE         <- strOptB.bind( F.RELATIVE_PATH, params )
          k = key1F( key )
          hashesHexE            <- hashesHexB.bind( k(F.HASHES_HEX), params )
        } yield {
          for {
            chunkNumber         <- chunkNumberE
            totalChunks         <- totalChunksE
            chunkSizeGeneral    <- chunkSizeGeneralE
            chunkSizeCurrent    <- chunkSizeCurrentE
            totalSize           <- totalSizeE
            identifier          <- identifierE
            fileName            <- fileNameE
            relativePath        <- relativePathE
            hashesHex           <- hashesHexE
          } yield {
            MUploadChunkQs(
              chunkNumberO      = chunkNumber,
              totalChunks       = totalChunks,
              chunkSizeGeneralO = chunkSizeGeneral,
              chunkSizeCurrent  = chunkSizeCurrent,
              totalSize         = totalSize,
              identifier        = identifier,
              fileName          = fileName,
              relativePath      = relativePath,
              hashesHex         = hashesHex,
            )
          }
        }
      }

      override def unbind(key: String, value: MUploadChunkQs): String = {
        val F = MUploadChunkQs.Fields

        _mergeUnbinded1(
          intOptB.unbind( F.CHUNK_NUMBER,               value.chunkNumberO ),
          intOptB.unbind( F.TOTAL_CHUNKS,               value.totalChunks ),
          chunkSizeOptB.unbind( F.CHUNK_SIZE_GEN,       value.chunkSizeGeneralO ),
          longOptB.unbind( F.CHUNK_SIZE_CUR,            value.chunkSizeCurrent ),
          longOptB.unbind( F.TOTAL_SIZE,                value.totalSize ),
          strOptB.unbind( F.IDENTIFIER,                 value.identifier ),
          strOptB.unbind( F.FILENAME,                   value.fileName ),
          strOptB.unbind( F.RELATIVE_PATH,              value.relativePath ),
          hashesHexB.unbind( key1(key, F.HASHES_HEX),   value.hashesHex ),
        )
      }
    }
  }



  /** routes-Биндер для параметров showcase'а. */
  implicit def mScIndexArgsQsb: QueryStringBindable[MScIndexArgs] = {
    new AbstractQueryStringBindable[MScIndexArgs] {
      import io.suggest.sc.ScConstants.ReqArgs._

      def strOptB = implicitly[QueryStringBindable[Option[String]]]
      def boolB = implicitly[QueryStringBindable[Boolean]]
      val boolOptB = implicitly[QueryStringBindable[Option[Boolean]]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScIndexArgs]] = {
        val f = key1F(key)
        for {
          // !!! Всегда нужно хотя бы одно ОБЯЗАТЕЛЬНЫЙ (не опциональный) биндинг.
          // Иначе, сервер будет ошибочно считать, что всегда требуется присылать индекс.
          adnIdOptE           <- strOptB.bind(f(NODE_ID_FN),          params)
          geoIntoRcvrE        <- boolB.bind(f(GEO_INTO_RCVR_FN),      params)
          // TODO 2020-07-16: Надо через несколько дней/недель заменить boolOpt на bool, убрав соотв.костыли.
          retUserLocOptE      <- boolOptB.bind(f(RET_GEO_LOC_FN),     params)
          returnEphemeralE    <- boolOptB.bind(f(RETURN_EPHEMERAL),   params)
        } yield {
          for {
            _adnIdOpt         <- adnIdOptE
            _geoIntoRcvr      <- geoIntoRcvrE
            _retUserLocOpt    <- retUserLocOptE
            _returnEphemeralO <- returnEphemeralE
          } yield {
            MScIndexArgs(
              nodeId          = _adnIdOpt,
              geoIntoRcvr     = _geoIntoRcvr,
              retUserLoc      = _retUserLocOpt.getOrElseFalse,
              returnEphemeral = _returnEphemeralO.getOrElseFalse,
            )
          }
        }
      }

      override def unbind(key: String, value: MScIndexArgs): String = {
        val f = key1F(key)
        _mergeUnbinded1(
          strOptB.unbind(f(NODE_ID_FN),           value.nodeId),
          boolB.unbind  (f(GEO_INTO_RCVR_FN),     value.geoIntoRcvr),
          boolOptB.unbind(f(RET_GEO_LOC_FN),      OptionUtil.maybeTrue(value.retUserLoc) ),
          boolOptB.unbind(f(RETURN_EPHEMERAL),    OptionUtil.maybeTrue(value.returnEphemeral) ),
        )
      }

    }
  }


  /** URL Query-string binder for SioPages.Sc3 showcase state URIs. */
  implicit def sc3MainScreenQsb: CrossQsBindable[SioPages.Sc3] = {
    import io.suggest.n2.node.MNodeTypesJvm._

    SioPages.Sc3.sc3QsB(
      stringOptB = implicitly[QueryStringBindable[Option[String]]],
      boolOptB = implicitly[QueryStringBindable[Option[Boolean]]],
      longOptB = implicitly[QueryStringBindable[Option[Long]]],
      geoPointOptB = {
        implicit val geoPointQsb = GeoPoint.pipeDelimitedQsb
        implicitly[QueryStringBindable[Option[MGeoPoint]]]
      },
      nodeIdTypeSeqB = QsbSeqUtil.qsbSeqQsB[MNodeIdType],
      loginOptB = implicitly[QueryStringBindable[Option[SioPages.Login]]],
    )
  }


  /** qs-биндинг таба Login-страницы. */
  implicit def loginTabQsb: CrossQsBindable[MLoginTab] =
    EnumeratumJvmUtil.valueEnumQsb( MLoginTabs )


  /** qs-биндинг для Login-страницы. */
  implicit def loginPageQsb(implicit
                            loginTabB       : CrossQsBindable[MLoginTab],
                            stringOptB      : QueryStringBindable[Option[String]],
                           ): CrossQsBindable[SioPages.Login] = {
    SioPages.Login.loginQsB( loginTabB, stringOptB )
  }


  /** QueryStringBindable implementation for MNodeIdType instances. */
  implicit def nodeIdTypeQsb(implicit
                             stringB: QueryStringBindable[String],
                             nodeTypeB: CrossQsBindable[MNodeType],
                            ): CrossQsBindable[MNodeIdType] = {
    MNodeIdType.nodeIdTypeQsB( stringB, nodeTypeB )
  }


  /** QSB для [[MLknBeaconsScanReq]]. */
  implicit def lknBeaconsAskReq(implicit
                                nodeIdTypeSeqB: QueryStringBindable[QsbSeq[MNodeIdType]],
                                stringOptB: QueryStringBindable[Option[String]],
                               ): QueryStringBindable[MLknBeaconsScanReq] = {
    new AbstractQueryStringBindable[MLknBeaconsScanReq] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLknBeaconsScanReq]] = {
        val F = MLknBeaconsScanReq.Fields
        val k = key1F( key )

        for {
          beaconIdsSeqE <- nodeIdTypeSeqB.bind( k(F.BEACON_UIDS), params )
          adIdOptE <- stringOptB.bind( k(F.AD_ID), params )
        } yield {
          for {
            beaconIdsSeq <- beaconIdsSeqE
            adIdOpt <- adIdOptE
            resp <- MLknBeaconsScanReq
              .validate(
                MLknBeaconsScanReq(
                  beaconUids  = beaconIdsSeq.items.toList,
                  adId        = adIdOpt,
                )
              )
              .toEither
              .left.map { errorsNel =>
                errorsNel
                  .iterator
                  .mkString( "\n" )
              }
          } yield resp
        }
      }

      override def unbind(key: String, value: MLknBeaconsScanReq): String = {
        val F = MLknBeaconsScanReq.Fields
        val k = key1F(key)
        _mergeUnbinded1(
          nodeIdTypeSeqB.unbind   ( k(F.BEACON_UIDS),     QsbSeq(value.beaconUids) ),
          stringOptB.unbind       ( k(F.AD_ID),           value.adId ),
        )
      }

    }
  }


  /** Поддержка биндинга инстансов модели в play router. */
  implicit def mBeaconDataQsb(implicit
                              nodeIdTypeB  : QueryStringBindable[MNodeIdType],
                              intOptB      : QueryStringBindable[Option[Int]],
                             ): QueryStringBindable[MUidBeacon] = {
    new AbstractQueryStringBindable[MUidBeacon] {
      private def strB = implicitly[QueryStringBindable[String]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MUidBeacon]] = {
        val k = key1F(key)
        val F = MUidBeacon.Fields
        for {
          // TODO проверять по uuid | uuid_major_minor
          nodeIdTypeE        <- {
            val fullFn = k(F.UID_FN)
            nodeIdTypeB
              .bind(fullFn, params)
              // TODO 2021-06-25 Drop fallback .orElse() when mobile apps will be updated.
              .orElse {
                strB
                  .bind( fullFn, params )
                  .map( _.map( MNodeIdType.bleBeaconFallback ) )
              }
          }
          distanceCmE     <- intOptB.bind  (k(F.DISTANCE_CM_FN),  params)
        } yield {
          for {
            nodeIdType    <- nodeIdTypeE
            distanceCm    <- distanceCmE
          } yield {
            MUidBeacon(
              node        = nodeIdType,
              distanceCm  = distanceCm,
            )
          }
        }
      }

      override def unbind(key: String, value: MUidBeacon): String = {
        val k = key1F(key)
        val F = MUidBeacon.Fields
        _mergeUnbinded1(
          nodeIdTypeB.unbind    (k(F.UID_FN),          value.node),
          intOptB.unbind        (k(F.DISTANCE_CM_FN),  value.distanceCm),
        )
      }
    }
  }


  implicit def lknOpKeyQsb: CrossQsBindable[MLknOpKey] =
    EnumeratumJvmUtil.valueEnumQsb( MLknOpKeys )

  implicit def lknOpValueQsb: QueryStringBindable[MLknOpValue] = {
    new AbstractQueryStringBindable[MLknOpValue] {
      private def boolOptB = implicitly[QueryStringBindable[Option[Boolean]]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLknOpValue]] = {
        val F = MLknOpValue.Fields
        val k = key1F(key)
        for {
          boolOptE <- boolOptB.bind( k(F.BOOL), params )
        } yield {
          for {
            boolOpt <- boolOptE
          } yield {
            MLknOpValue(
              bool = boolOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: MLknOpValue): String = {
        val F = MLknOpValue.Fields
        val k = key1F(key)
        boolOptB.unbind( k(F.BOOL), value.bool )
      }

    }
  }

  /** Поддержка бинда для MLknModifyQs. */
  implicit def lknModifyQsb: QueryStringBindable[MLknModifyQs] = {
    new AbstractQueryStringBindable[MLknModifyQs] {
      private def stringSeqB = implicitly[QueryStringBindable[QsbSeq[String]]]
      private def stringOptB = implicitly[QueryStringBindable[Option[String]]]
      private def opKeyB = implicitly[QueryStringBindable[MLknOpKey]]
      private def opValueB = implicitly[QueryStringBindable[MLknOpValue]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLknModifyQs]] = {
        val k = key1F(key)
        val F = MLknModifyQs.Fields
        for {
          onNodeRkE     <- stringSeqB.bind  ( k(F.ON_NODE_RK),    params )
          adIdOptE      <- stringOptB.bind  ( k(F.AD_ID),         params )
          opKeyE        <- opKeyB.bind      ( k(F.OP_KEY),        params )
          opValueE      <- opValueB.bind    ( k(F.OP_VALUE),      params )
        } yield {
          for {
            onNodeRk    <- onNodeRkE
            adIdOpt     <- adIdOptE
            opKey       <- opKeyE
            opValue     <- opValueE
          } yield {
            MLknModifyQs(
              onNodeRk  = onNodeRk.items.toList,
              adIdOpt   = adIdOpt,
              opKey     = opKey,
              opValue   = opValue,
            )
          }
        }
      }

      override def unbind(key: String, value: MLknModifyQs): String = {
        val k = key1F(key)
        val F = MLknModifyQs.Fields
        _mergeUnbinded1(
          stringSeqB.unbind ( k(F.ON_NODE_RK),    QsbSeq(value.onNodeRk) ),
          stringOptB.unbind ( k(F.AD_ID),         value.adIdOpt ),
          opKeyB.unbind     ( k(F.OP_KEY),        value.opKey   ),
          opValueB.unbind   ( k(F.OP_VALUE),      value.opValue ),
        )
      }

    }
  }


  implicit def zNelQsb[A](implicit
                          qsbSeqB: QueryStringBindable[QsbSeq[A]]
                         ): QueryStringBindable[NonEmptyList[A]] = {
    new AbstractQueryStringBindable[NonEmptyList[A]] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, NonEmptyList[A]]] = {
        qsbSeqB
          .bind( key, params )
          .filter( _.fold(_ => true, _.items.nonEmpty) )
          .map(
            _.flatMap { qsbSeq =>
              IList.fromSeq( qsbSeq.items ) match {
                case ICons(head, tail) =>
                  Right( NonEmptyList.nel(head, tail) )
                case _ =>
                  Left( "error.unexpected" )
              }
            }
          )
      }

      override def unbind(key: String, value: NonEmptyList[A]): String = {
        qsbSeqB.unbind( key, QsbSeq(value.asSeq) )
      }
    }
  }


  /** NonEmptyList[A], но если length == 1, то просто единичное значение A без QsbSeq-мишуры. */
  def zNelOrSingleValueQsb[A](implicit
                              singleB : QueryStringBindable[A]
                             ): QueryStringBindable[NonEmptyList[A]] = {
    new AbstractQueryStringBindable[NonEmptyList[A]] {
      private def zNelB = zNelQsb[A]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, NonEmptyList[A]]] = {
        zNelB
          .bind(key, params)
          .orElse {
            singleB
              .bind(key, params)
              .map(_.map( NonEmptyList(_) ))
          }
      }

      override def unbind(key: String, value: NonEmptyList[A]): String = {
        if (value.tail.isEmpty) {
          singleB.unbind( key, value.head )
        } else {
          zNelB.unbind( key, value )
        }
      }

    }
  }


  implicit def mGeoLocSourceQsb(implicit strB: QueryStringBindable[String]): CrossQsBindable[MGeoLocSource] =
    MGeoLocSource.geoLocSourceQsB( strB )

  /** QSB support for MGeoLoc. */
  implicit def mGeoLocQsb(implicit
                          geoPointB  : QueryStringBindable[MGeoPoint],
                          geoLocSourceB: QueryStringBindable[Option[MGeoLocSource]],
                          doubleOptB : QueryStringBindable[Option[Double]],
                         ): CrossQsBindable[MGeoLoc] = {
    MGeoLoc.mGeoLocQsB( geoPointB, geoLocSourceB, doubleOptB )
  }


  implicit def mLocEnvQsb(implicit
                          geoLocB   : QueryStringBindable[MGeoLoc],
                          beaconsB  : QueryStringBindable[QsbSeq[MUidBeacon]],
                         ): QueryStringBindable[MLocEnv] = {
    new AbstractQueryStringBindable[MLocEnv] {
      private def geoLocOptB = implicitly[QueryStringBindable[Option[MGeoLoc]]]
      private def geoLocsSeqB = implicitly[QueryStringBindable[QsbSeq[MGeoLoc]]]

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLocEnv]] = {
        val k = key1F(key)
        val F = MLocEnv.Fields
        for {
          geoLocsE <- {
            val geoLocKey = k( F.GEO_LOC_FN )
            geoLocsSeqB
              .bind( geoLocKey, params )
              .map( _.map(_.items.distinctBy(_.point)) ) // TODO distinctBy - move into qs validation (not yet implemented)
              .filter( _.fold(_ => true, _.nonEmpty) ) // Left() - bypass?
              .orElse {
                // 2021-09-22 Compat. non-seq optional values for cordova mobile-apps <= v5.2.1
                geoLocOptB
                  .bind( geoLocKey, params )
                  .map( _.map(_.toSeq) )
              }
          }
          beaconsE      <- beaconsB.bind   (k( F.BEACONS_FN ), params)
        } yield {
          for {
            geoLocs     <- geoLocsE
            beacons     <- beaconsE
          } yield {
            MLocEnv(
              geoLoc   = geoLocs,
              beacons  = beacons.items
            )
          }
        }
      }

      override def unbind(key: String, value: MLocEnv): String = {
        val k = key1F(key)
        val F = MLocEnv.Fields
        _mergeUnbinded1(
          geoLocsSeqB.unbind (k( F.GEO_LOC_FN ), QsbSeq(value.geoLoc) ),
          beaconsB.unbind    (k( F.BEACONS_FN ), QsbSeq(value.beacons) ),
        )
      }
    }
  }


  /** QSB-поддержка для [[MScCommonQs]]. */
  implicit def mScCommonQsQsb(implicit
                              screenOptB   : QueryStringBindable[Option[MScreen]],
                              apiVsnB      : QueryStringBindable[MScApiVsn],
                              locEnvB      : QueryStringBindable[MLocEnv],
                              boolOptB     : QueryStringBindable[Option[Boolean]]
                             ): QueryStringBindable[MScCommonQs] = {
    new AbstractQueryStringBindable[MScCommonQs] {
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
    new AbstractQueryStringBindable[MScNodesArgs] {
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
    new AbstractQueryStringBindable[MAdsSearchReq] {
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
    new AbstractQueryStringBindable[MScGridArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScGridArgs]] = {
        val k = key1F(key)
        val F = MScGridArgs.Fields
        for {
          adTitlesE           <- boolB.bind( k(F.WITH_TITLE), params )
          focAfterJumpOptE    <- boolOptB.bind( k(F.FOC_AFTER_JUMP), params )
          allow404OptE        <- boolOptB.bind( k(F.ALLOW_404), params )
          onlyRadioBeaconsE   <- boolOptB.bind( k(F.ONLY_RADIO_BEACONS), params )
        } yield {
          for {
            adTitles          <- adTitlesE
            focAfterJumpOpt   <- focAfterJumpOptE
            allow404Opt       <- allow404OptE
            onlyRadioBeaconsO <- onlyRadioBeaconsE
          } yield {
            MScGridArgs(
              withTitle        = adTitles,
              focAfterJump    = focAfterJumpOpt,
              allow404        = allow404Opt.getOrElseTrue,
              onlyRadioBeacons = onlyRadioBeaconsO.getOrElseFalse,
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
          boolOptB.unbind( k(F.ALLOW_404), Option.when( !value.allow404 )(value.allow404) ),
          boolOptB.unbind( k(F.ONLY_RADIO_BEACONS), Option.when( !value.onlyRadioBeacons )(value.onlyRadioBeacons) ),
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
    new AbstractQueryStringBindable[MScQs] {
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


  /** Поддержка QSB для MIndexAdOpenQs. */
  implicit def indexAdOpenQsb(implicit
                              boolB            : QueryStringBindable[Boolean],
                             ): QueryStringBindable[MIndexAdOpenQs] = {
    new AbstractQueryStringBindable[MIndexAdOpenQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MIndexAdOpenQs]] = {
        val F = MIndexAdOpenQs.Fields
        val k = key1F( key )
        for {
          withBleBeaconAdsE <- boolB.bind( k(F.WITH_BLE_BEACON_ADS), params )
        } yield {
          for {
            withBleBeaconAds <- withBleBeaconAdsE
          } yield {
            MIndexAdOpenQs(
              withBleBeaconAds = withBleBeaconAds,
            )
          }
        }
      }

      override def unbind(key: String, value: MIndexAdOpenQs): String = {
        val F = MIndexAdOpenQs.Fields
        val k = key1F( key )
        boolB.unbind( k(F.WITH_BLE_BEACON_ADS), value.withBleBeaconAds )
      }

    }
  }


  /** Поддержка QSB для MScFocusArgs. */
  implicit def mScFocusArgsQsb(implicit
                               indexAdOpenQs    : QueryStringBindable[Option[MIndexAdOpenQs]],
                              ): QueryStringBindable[MScFocusArgs] = {
    new AbstractQueryStringBindable[MScFocusArgs] {
      private def boolB = implicitly[QueryStringBindable[Boolean]]
      private def zNelOrSingleStrB = zNelOrSingleValueQsb[String]

      import io.suggest.ad.search.AdSearchConstants._

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScFocusArgs]] = {
        val k = key1F(key)
        for {
          indexAdOpenOptE         <- {
            val indexAdOpenK = k( FOC_INDEX_AD_OPEN_FN )
            indexAdOpenQs
              .bind( indexAdOpenK, params )
              .orElse {
                boolB
                  .bind( indexAdOpenK, params )
                  .map( _.map(MIndexAdOpenQs.fromFocIndexAdOpenEnabled) )
              }
          }
          lookupAdIdE             <- zNelOrSingleStrB.bind  ( k(AD_IDS_FN),               params )
        } yield {
          for {
            indexAdOpenOpt        <- indexAdOpenOptE
            lookupAdId            <- lookupAdIdE
          } yield {
            MScFocusArgs(
              indexAdOpen         = indexAdOpenOpt,
              adIds               = lookupAdId,
            )
          }
        }
      }

      override def unbind(key: String, value: MScFocusArgs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          indexAdOpenQs.unbind  ( k(FOC_INDEX_AD_OPEN_FN),    value.indexAdOpen ),
          zNelOrSingleStrB.unbind( k(AD_IDS_FN),              value.adIds ),
        )
      }

    }
  }

}
