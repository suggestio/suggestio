package io.suggest.model

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.n2.media.storage.{MStorage, MStorageInfo, MStorageInfoData, MStorages}
import _root_.play.api.mvc.QueryStringBindable
import io.suggest.crypto.hash.{HashesHex, MHash, MHashes}
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.n2.edge.{MPredicate, MPredicates}
import io.suggest.n2.edge.edit.MNodeEdgeIdQs
import io.suggest.n2.media.{MFileMeta, MFileMetaHash, MFileMetaHashFlag, MFileMetaHashFlags}
import io.suggest.sc.app.{MScAppGetQs, MScAppManifestQs}
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import io.suggest.swfs.fid.Fid
import io.suggest.util.logs.MacroLogsDyn
import io.suggest.xplay.qsb.{QsbSeq, QueryStringBindableImpl}

import scala.util.Try
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2019 8:25
  * Description: Контейнер jvm-only данных для разных пошаренных common-моделей.
  */
object CommonModelsJvm extends MacroLogsDyn {

  /** Биндинги для url query string. */
  implicit def mScApiVsnQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] =
    EnumeratumJvmUtil.valueEnumQsb( MScApiVsns )

  /** QSB для инстансов [[MStorage]]. */
  implicit def mStorageQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MStorage] =
    EnumeratumJvmUtil.valueEnumQsb( MStorages )


  /** Поддержка сырого биндинга из query-string. */
  implicit def fidQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[Fid] = {
    new QueryStringBindableImpl[Fid] {
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
                                  strSetB      : QueryStringBindable[Seq[String]],
                                 ): QueryStringBindable[MStorageInfoData] = {
    new QueryStringBindableImpl[MStorageInfoData] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MStorageInfoData]] = {
        val F = MStorageInfoData.Fields
        val k = key1F(key)
        for {
          dataE       <- strB.bind( k(F.META_FN), params )
          hostsE      <- strSetB.bind( k(F.HOST_FN), params )
        } yield {
          for {
            data      <- dataE
            hosts     <- hostsE
          } yield {
            MStorageInfoData(
              meta  = data,
              hosts = hosts,
            )
          }
        }
      }

      override def unbind(key: String, value: MStorageInfoData): String = {
        val F = MStorageInfoData.Fields
        val k = key1F(key)
        _mergeUnbinded1(
          strB.unbind( k(F.META_FN), value.meta ),
          strSetB.unbind( k(F.HOST_FN), value.hosts ),
        )
      }

    }
  }


  implicit def storageInfoQsb(implicit
                              storageB   : QueryStringBindable[MStorage],
                              infoDataB  : QueryStringBindable[MStorageInfoData],
                             ): QueryStringBindable[MStorageInfo] = {
    new QueryStringBindableImpl[MStorageInfo] {
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
    new QueryStringBindableImpl[MNodeEdgeIdQs] {

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


  implicit def osPlatformQsb: QueryStringBindable[MOsFamily] =
    EnumeratumJvmUtil.valueEnumQsb( MOsFamilies )


  /** Поддержка MScAppDlQs. */
  implicit def scAppDlQs(implicit
                         predicateOptB: QueryStringBindable[Option[MPredicate]],
                        ): QueryStringBindable[MScAppGetQs] = {
    new QueryStringBindableImpl[MScAppGetQs] {
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


  implicit def mhashQsb: QueryStringBindable[MHash] =
    EnumeratumJvmUtil.valueEnumQsb( MHashes )


  /** Поддержка URL-qs вида "x.s1=aahh45234234&x.s256=aa543525325..."  */
  implicit def hashesHexQsb: QueryStringBindable[HashesHex] = {
   new QueryStringBindableImpl[HashesHex] {

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



  implicit def fileMetaHashFlagQsb: QueryStringBindable[MFileMetaHashFlag] =
    EnumeratumJvmUtil.valueEnumQsb( MFileMetaHashFlags )


  implicit def fileMetaHashQsb: QueryStringBindable[MFileMetaHash] = {
    new QueryStringBindableImpl[MFileMetaHash] {
      lazy val hashB = implicitly[QueryStringBindable[MHash]]
      lazy val strB = implicitly[QueryStringBindable[String]]
      lazy val fmHashFlagsB = implicitly[QueryStringBindable[QsbSeq[MFileMetaHashFlag]]]

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
    new QueryStringBindableImpl[MFileMeta] {
      lazy val strOptB = implicitly[QueryStringBindable[Option[String]]]
      lazy val longOptB = implicitly[QueryStringBindable[Option[Long]]]
      lazy val boolB = implicitly[QueryStringBindable[Boolean]]
      lazy val fmHashesB = implicitly[QueryStringBindable[QsbSeq[MFileMetaHash]]]

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
    val strOptB = implicitly[QueryStringBindable[Option[String]]]
    val hashesHexB = implicitly[QueryStringBindable[HashesHex]]

    new QueryStringBindableImpl[MScAppManifestQs] {
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

      override def unbind(key: String, value: MScAppManifestQs): String = {
        ???
      }
    }
  }

}
