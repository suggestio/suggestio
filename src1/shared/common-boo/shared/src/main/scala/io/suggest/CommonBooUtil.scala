package io.suggest

import boopickle.Default._
import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.ble.MUidBeacon
import io.suggest.color.{MColorData, MColors, MHistogram, MRgb}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.crypto.hash.MHash
import io.suggest.dev.{MPxRatio, MScreen, MSzMult}
import io.suggest.es.model.MEsUuId
import io.suggest.file.MSrvFileInfo
import io.suggest.font.{MFont, MFontSize}
import io.suggest.jd.tags.{JdTag, MJdProps1, MJdShadow, MJdTagName}
import io.suggest.jd.{MJdData, MJdDoc, MJdEdge, MJdEdgeId, MJdTagId}
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.media.MMediaInfo
import io.suggest.n2.edge.{MEdgeFlag, MEdgeFlagData, MPredicate}
import io.suggest.n2.media.storage.{MStorage, MStorageInfo, MStorageInfoData}
import io.suggest.n2.media.{MFileMeta, MFileMetaHash, MFileMetaHashFlag, MPictureMeta}
import io.suggest.n2.node.{MNodeIdType, MNodeType}
import io.suggest.primo.ISetUnset
import io.suggest.sc.MScApiVsn
import io.suggest.sc.ads.{MAdsSearchReq, MIndexAdOpenQs, MSc3AdData, MSc3AdsResp, MScAdInfo, MScAdMatchInfo, MScFocusArgs, MScGridArgs, MScNodeMatchInfo, MScNodesArgs}
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs, MWelcomeInfo}
import io.suggest.sc.sc3.{MSc3Resp, MSc3RespAction, MScCommonQs, MScConfUpdate, MScQs, MScRespActionType}
import io.suggest.scalaz.ScalazBooUtil._
import io.suggest.geo.GeoBooUtil._
import io.suggest.jd.tags.event.{MJdtAction, MJdtEventActions, MJdtEventInfo, MJdtEventType, MJdtEvents}
import io.suggest.jd.tags.html.{MJdHtml, MJdHtmlType}
import io.suggest.jd.tags.qd.{MQdAttrsEmbed, MQdAttrsLine, MQdAttrsText, MQdOp}
import io.suggest.sc.ssr.IScSsrAction
import io.suggest.text.MTextAlign

/** Picklers for common models.
  * Semi-auto generated to prevent size bloating and macro-generated code deduplication.
  * Without these deduplicated picklers, compiler will generate 100-150 mib of macro-classes.
  */
object CommonBooUtil {

  // io.suggest.*
  implicit val size2di: Pickler[MSize2di] = generatePickler

  implicit val textAlignsP: Pickler[MTextAlign] = generatePickler
  implicit val fontP: Pickler[MFont] = generatePickler
  implicit val fontSizeP: Pickler[MFontSize] = generatePickler

  implicit val screenP: Pickler[MScreen] = generatePickler
  implicit val pxRatioP: Pickler[MPxRatio] = generatePickler
  implicit val esUuidP: Pickler[MEsUuId] = generatePickler

  implicit val hashP: Pickler[MHash] = generatePickler

  implicit val mMediaInfoP: Pickler[MMediaInfo] = generatePickler

  implicit val srvFileInfoP: Pickler[MSrvFileInfo] = generatePickler

  implicit val blockExpandModeP: Pickler[MBlockExpandMode] = generatePickler

  implicit val szMultP: Pickler[MSzMult] = generatePickler

  implicit def iSetUnsetP[A: Pickler]: Pickler[ISetUnset[A]] = generatePickler


  // io.suggest.maps.*
  implicit val rcvrsMapUrlArgsP: Pickler[MRcvrsMapUrlArgs] = generatePickler
  implicit val geoNodePropsShapesP: Pickler[MGeoNodePropsShapes] = generatePickler
  implicit val geoNodesRespP: Pickler[MGeoNodesResp] = generatePickler

  // io.suggest.color.*
  implicit val mRgbP: Pickler[MRgb] = generatePickler
  implicit val colorDataP: Pickler[MColorData] = generatePickler
  implicit val colorsP: Pickler[MColors] = generatePickler


  // io.suggest.jd.*
  implicit val jdTagNameP: Pickler[MJdTagName] = generatePickler
  implicit val jdEdgeIdP: Pickler[MJdEdgeId] = generatePickler
  implicit val jdEdgeP: Pickler[MJdEdge] = generatePickler
  implicit val jdTagP: Pickler[JdTag] = generatePickler
  implicit val jdTagId: Pickler[MJdTagId] = generatePickler
  implicit val jdDocP: Pickler[MJdDoc] = generatePickler
  implicit val jdDataP: Pickler[MJdData] = generatePickler
  implicit val qdAttrsTextP: Pickler[MQdAttrsText] = generatePickler
  implicit val qdAttrsLineP: Pickler[MQdAttrsLine] = generatePickler
  implicit val qdAttrsEmbedP: Pickler[MQdAttrsEmbed] = generatePickler
  implicit val qdOpP: Pickler[MQdOp] = generatePickler
  implicit val jdProps1P: Pickler[MJdProps1] = generatePickler
  implicit val jdtEventsP: Pickler[MJdtEvents] = generatePickler
  implicit val jdtEventActionP: Pickler[MJdtEventActions] = generatePickler
  implicit val jdtEventInfoP: Pickler[MJdtEventInfo] = generatePickler
  implicit val jdtEventTypeP: Pickler[MJdtEventType] = generatePickler
  implicit val jdtActionP: Pickler[MJdtAction] = generatePickler
  implicit val jdHtmlP: Pickler[MJdHtml] = generatePickler
  implicit val jdHtmlTypeP: Pickler[MJdHtmlType] = generatePickler
  implicit val jdShadowP: Pickler[MJdShadow] = generatePickler


  // io.suggest.n2.*
  implicit val edgeFlagP: Pickler[MEdgeFlag] = generatePickler
  implicit val edgeFlagDataP: Pickler[MEdgeFlagData] = generatePickler
  implicit val nodeTypeP: Pickler[MNodeType] = generatePickler
  implicit val predicateP: Pickler[MPredicate] = generatePickler
  implicit val pictureMetaP: Pickler[MPictureMeta] = generatePickler
  implicit val histogramP: Pickler[MHistogram] = generatePickler
  implicit val fileMetaP: Pickler[MFileMeta] = generatePickler
  implicit val fileMetaHashP: Pickler[MFileMetaHash] = generatePickler
  implicit val fileMetaHashFlagP: Pickler[MFileMetaHashFlag] = generatePickler
  implicit val storageP: Pickler[MStorage] = generatePickler
  implicit val storageInfoP: Pickler[MStorageInfo] = generatePickler
  implicit val storageInfoDataP: Pickler[MStorageInfoData] = generatePickler
  implicit val welcomeInfoP: Pickler[MWelcomeInfo] = generatePickler
  implicit val nodeIdTypeP: Pickler[MNodeIdType] = generatePickler
  implicit val uidBeaconP: Pickler[MUidBeacon] = generatePickler


  // io.suggest.sc.*
  implicit val sc3IndexRespP: Pickler[MSc3IndexResp] = generatePickler
  implicit val scApiVsnP: Pickler[MScApiVsn] = generatePickler
  implicit val sc3AdDataP: Pickler[MSc3AdData] = generatePickler
  implicit val sc3AdsRespP: Pickler[MSc3AdsResp] = generatePickler
  implicit val sc3RespP: Pickler[MSc3Resp] = generatePickler
  implicit val sc3RespActionP: Pickler[MSc3RespAction] = generatePickler
  implicit val scRespActionTypeP: Pickler[MScRespActionType] = generatePickler
  implicit val scConfUpdateP: Pickler[MScConfUpdate] = generatePickler
  implicit val scAdInfoP: Pickler[MScAdInfo] = generatePickler
  implicit val scAdMatchInfoP: Pickler[MScAdMatchInfo] = generatePickler
  implicit val scNodeMatchInfoP: Pickler[MScNodeMatchInfo] = generatePickler
  implicit val scSsrActionP: Pickler[IScSsrAction] = generatePickler

  implicit val scQsP: Pickler[MScQs] = generatePickler
  implicit val scCommonQsP: Pickler[MScCommonQs] = generatePickler
  implicit val adsSearchReqP: Pickler[MAdsSearchReq] = generatePickler
  implicit val scIndexArgsP: Pickler[MScIndexArgs] = generatePickler
  implicit val scFocusArgsP: Pickler[MScFocusArgs] = generatePickler
  implicit val indexAdOpenQsP: Pickler[MIndexAdOpenQs] = generatePickler
  implicit val scGridArgsP: Pickler[MScGridArgs] = generatePickler
  implicit val scNodesArgsP: Pickler[MScNodesArgs] = generatePickler

}
