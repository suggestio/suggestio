package io.suggest.n2.edge

import io.suggest.common.empty.EmptyUtil._
import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.geo.MNodeGeoLevel
import io.suggest.model.PrefixedFn
import io.suggest.n2.media.MEdgeMedia
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.15 11:09
  * Description: Модель эджа, карта которых хранится внутри N2-узла MNode.
  *
  * Изначально, этот эдж был направленным ребром N2-графа.
  * Потом, появилась параметризация эджа каким-то дополнительным payload'ом.
  * Затем, nodeId стал необязательным, и эдж стал некоей совсем абстрактной перечисляемой единицей объекта данных,
  * которая лишь в частном случае является параметризованным мульти-ребром графа N2.
  *
  * При запиливании doc-tags для сборки абстрактного документа карточки,
  * было решено хранить и индексировать куски текста в эджах, наравне с отсылками к Media-узлам и прочим вещам.
  * Таким образом, эджи стали абстрактной удобной моделью представления данных на базе nested-объектов.
  */
object MEdge
  extends IEsMappingProps
{

  /** Контейнер имён полей. */
  object Fields {

    val PREDICATE_FN  = "p"
    val NODE_ID_FN    = "i"
    val ORDER_FN      = "o"
    val INFO_FN       = "n"
    val DOC_FN        = "d"
    val MEDIA_FN      = "m"


    object Media extends PrefixedFn {
      override protected def _PARENT_FN = MEDIA_FN

      import MEdgeMedia.Fields.{FileMeta => FM}

      def MEDIA_FM_MIME_FN = _fullFn( FM.FM_MIME_FN )
      def MEDIA_FM_MIME_AS_TEXT_FN = _fullFn( FM.FM_MIME_AS_TEXT_FN )

      /** Full FN nested-поля с хешами. */
      def MEDIA_FM_HASHES_FN = _fullFn( FM.FM_HASHES_FN )

      def MEDIA_FM_HASHES_TYPE_FN = _fullFn( FM.FM_HASHES_TYPE_FN )
      def MEDIA_FM_HASHES_VALUE_FN = _fullFn( FM.FM_HASHES_VALUE_FN )

      def MEDIA_FM_SIZE_B_FN = _fullFn( FM.FM_SIZE_B_FN )

      def MEDIA_FM_IS_ORIGINAL_FN = _fullFn( FM.FM_IS_ORIGINAL_FN )


      import MEdgeMedia.Fields.{StorageFns => S}

      def MEDIA_S_TYPE_FN = _fullFn( S.S_TYPE_FN )
      def MEDIA_S_DATA_META_FN = _fullFn( S.S_DATA_META_FN )

    }


    /** Модель названий полей, проброшенных сюда из полей [[MEdgeInfo]]-модели. */
    object Info extends PrefixedFn {

      override protected def _PARENT_FN = INFO_FN

      import MEdgeInfo.{Fields => F}

      def FLAG_FN       = _fullFn( F.FLAG_FN )

      // Теги
      def TAGS_FN       = _fullFn( F.TAGS_FN )
      def TAGS_RAW_FN   = _fullFn( F.Tags.TAGS_RAW_FN )

      // Гео-шейпы
      def INFO_GS_FN                            = _fullFn( F.GEO_SHAPES_FN )

      import F.{GeoShapes => Gs}
      def INFO_GS_GLEVEL_FN                     = _fullFn( Gs.GS_GLEVEL_FN )
      def INFO_GS_GJSON_COMPAT_FN               = _fullFn( Gs.GS_GJSON_COMPAT_FN )
      def INFO_GS_SHAPE_FN(ngl: MNodeGeoLevel)  = _fullFn( Gs.GS_SHAPE_FN(ngl) )

      // Гео-точки
      def INFO_GEO_POINTS_FN                    = _fullFn( F.GEO_POINT_FN )

      // Внешние сервисы.
      def INFO_EXT_SERVICE_FN                   = _fullFn( F.EXT_SERVICE_FN )
      def INFO_OS_FAMILY_FN                     = _fullFn( F.OS_FAMILY_FN )

    }


    /** Модель полных названий [[MEdgeDoc]]-полей. */
    object DocFns extends PrefixedFn {

      import MEdgeDoc.{Fields => F}
      override protected def _PARENT_FN = DOC_FN

      def TEXT_FN = _fullFn( F.TEXT_FN )
      def UID_FN  = _fullFn( F.UID_FN )

    }

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val edgeJson: OFormat[MEdge] = (
    (__ \ PREDICATE_FN).format(MPredicate.MPREDICATE_DEEP_FORMAT) and
    (__ \ NODE_ID_FN).formatNullable[Set[String]]
      .inmap [Set[String]] (
        EmptyUtil.opt2ImplEmptyF(Set.empty),
        nodeIds => if (nodeIds.nonEmpty) Some(nodeIds) else None
      ) and
    (__ \ ORDER_FN).formatNullable[Int] and
    (__ \ INFO_FN).formatNullable[MEdgeInfo]
      .inmap [MEdgeInfo] (
        opt2ImplMEmptyF( MEdgeInfo ),
        implEmpty2OptF
      ) and
    (__ \ DOC_FN).formatNullable[MEdgeDoc]
      .inmap [MEdgeDoc] (
        opt2ImplMEmptyF( MEdgeDoc ),
        implEmpty2OptF
      ) and
    (__ \ MEDIA_FN).formatNullable[MEdgeMedia]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.PREDICATE_FN -> FKeyWord.indexedJs,
      F.NODE_ID_FN -> FKeyWord.indexedJs,
      // orderId -- not_analyzed, используется в т.ч. для хранения статистики использования геотегов, как это не странно...
      F.ORDER_FN -> FNumber(
        typ   = DocFieldTypes.Integer,
        index = someTrue,
      ),
      F.INFO_FN -> FObject.plain( MEdgeInfo ),
      F.DOC_FN -> FObject.plain( MEdgeDoc ),
      F.MEDIA_FN -> FObject.plain( MEdgeMedia ),
    )
  }

  val predicate = GenLens[MEdge](_.predicate)
  val nodeIds   = GenLens[MEdge](_.nodeIds)
  val order     = GenLens[MEdge](_.order)
  val info      = GenLens[MEdge](_.info)
  val doc       = GenLens[MEdge](_.doc)
  val media     = GenLens[MEdge](_.media)


  @inline implicit def univEq: UnivEq[MEdge] = UnivEq.derive

}



/** Реализация node edge-модели.
  *
  * @param predicate Предикат, т.е. некоторый тип эджа.
  * @param nodeIds id ноды на дальнем конце эджа, если есть.
  * @param order Для поддержкания порядка эджей можно использовать это опциональное поле.
  *              Можно также использовать для некоего внутреннего доп.идентификатора.
  * @param info Контейнер доп.данных текущего эджа.
  * @param media Эдж, указывающий на файл, хранит здесь мета-данные файла.
  *              Сюда была замёржена модель MMedia.
  */
case class MEdge(
                  predicate  : MPredicate,
                  // Обычно nodeId задан, поэтому без default тут для защиты от возможных ошибок.
                  nodeIds    : Set[String]    = Set.empty,
                  order      : Option[Int]    = None,
                  info       : MEdgeInfo      = MEdgeInfo.empty,
                  doc        : MEdgeDoc       = MEdgeDoc.empty,
                  media      : Option[MEdgeMedia] = None,
                ) {

  override def toString: String = {
    val sb = new StringBuilder(64)
      .append("E(")
    sb.append(predicate.value)
      .append(':')
    for (nodeId <- nodeIds) {
      sb.append(nodeId)
        .append(',')
    }
    for (ord <- order) {
      sb.append(':').append(ord)
    }
    if (info.nonEmpty) {
      sb.append('{')
        .append(info)
        .append('}')
    }
    if (doc.nonEmpty) {
      sb.append("d={")
        .append(doc.toString)
        .append("},")
    }
    sb.append(')')
      .toString()
  }


  /** Т.к. doc получился вне info, бывает нужно объеденить их, если там есть данные. */
  def edgeDatas: Iterator[Any] = {
    productIterator
      .flatMap {
        case pe: EmptyProduct if pe.nonEmpty =>
          pe :: Nil
        case Some(x) =>
          x :: Nil
        case _ =>
          Nil
      }
  }

}
